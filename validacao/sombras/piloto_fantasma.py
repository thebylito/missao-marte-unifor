#!/usr/bin/env python3
"""Piloto fantasma do modo shadow (Fase 7): um segundo piloto sem jogo, só pela rede.

Entra no servidor com uma conta (cria a conta se ela não existir), abre o canal /ws/sombras com o token e voa em
círculos perto da base, mandando a posição 10 vezes por segundo, como o jogo faz. Ao mesmo tempo, registra as
sombras que recebe dos outros pilotos (o jogo no celular ou no desktop).

Serve para duas coisas:
  - validar o canal de ponta a ponta com um jogo de verdade do outro lado (o jogo vê o fantasma; o fantasma vê o jogo);
  - ver uma sombra com um aparelho só.

Só a biblioteca padrão do Python: o aperto de mão e os quadros do WebSocket (RFC 6455) estão escritos aqui, no
mínimo necessário (texto, ping/pong e fechamento). A senha nunca é impressa; o token também não.

Uso:
  python3 validacao/sombras/piloto_fantasma.py --servidor http://localhost:8080 --nome Fantasma --segundos 60
  python3 validacao/sombras/piloto_fantasma.py --explodir-a-cada 10   (explode a cada 10 s; os outros veem a explosão)
  (a senha vem de MISSAOMARTE_SENHA_FANTASMA; sem ela, usa uma senha fixa só de teste)
"""
import argparse
import base64
import json
import math
import os
import re
import socket
import ssl
import struct
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter

# perto da base (x de 260 a 516, superfície em 128): o fantasma aparece na tela logo que a partida começa
CENTRO = (620.0, 360.0)
RAIO = 140.0
VOLTA_SEGUNDOS = 8.0


def pedir(servidor, caminho, corpo):
    pedido = urllib.request.Request(servidor + caminho, data=json.dumps(corpo).encode(),
                                    headers={'Content-Type': 'application/json'}, method='POST')
    try:
        with urllib.request.urlopen(pedido, timeout=5) as r:
            return r.status, json.loads(r.read() or b'{}')
    except urllib.error.HTTPError as e:
        return e.code, {}


def entrar(servidor, nome, senha):
    """Entra; se a conta não existir, cria e entra. Devolve o token."""
    status, resposta = pedir(servidor, '/api/sessoes', {'nome': nome, 'senha': senha})
    if status != 200:
        criado, _ = pedir(servidor, '/api/jogadores', {'nome': nome, 'senha': senha})
        if criado != 201:
            sys.exit(f'não deu para entrar nem criar a conta "{nome}" (login {status}, cadastro {criado})')
        status, resposta = pedir(servidor, '/api/sessoes', {'nome': nome, 'senha': senha})
    if status != 200 or 'token' not in resposta:
        sys.exit(f'login de "{nome}" falhou ({status})')
    return resposta['token']


class WebSocket:
    """Cliente WebSocket mínimo (só texto)."""

    def __init__(self, url, token):
        u = urllib.parse.urlparse(url)
        porta = u.port or (443 if u.scheme == 'wss' else 80)
        bruto = socket.create_connection((u.hostname, porta), timeout=5)
        self.sock = ssl.create_default_context().wrap_socket(bruto, server_hostname=u.hostname) \
            if u.scheme == 'wss' else bruto
        chave = base64.b64encode(os.urandom(16)).decode()
        destino = u.path + ('?' + u.query if u.query else '')
        self.sock.sendall((f'GET {destino} HTTP/1.1\r\nHost: {u.hostname}:{porta}\r\nUpgrade: websocket\r\n'
                           f'Connection: Upgrade\r\nSec-WebSocket-Key: {chave}\r\nSec-WebSocket-Version: 13\r\n'
                           f'Authorization: Bearer {token}\r\n\r\n').encode())
        resposta = b''
        while b'\r\n\r\n' not in resposta:
            parte = self.sock.recv(1024)
            if not parte:
                break
            resposta += parte
        linha = resposta.split(b'\r\n', 1)[0].decode(errors='replace')
        if ' 101' not in linha:
            raise ConnectionError(f'aperto de mão recusado: {linha}')
        self.sobra = resposta.split(b'\r\n\r\n', 1)[1]
        self.sock.settimeout(None)
        self.trava = threading.Lock()

    def _quadro(self, opcode, dados):
        mascara = os.urandom(4)
        n = len(dados)
        cabeca = bytes([0x80 | opcode])
        if n < 126:
            cabeca += bytes([0x80 | n])
        elif n < 65536:
            cabeca += bytes([0x80 | 126]) + struct.pack('>H', n)
        else:
            cabeca += bytes([0x80 | 127]) + struct.pack('>Q', n)
        with self.trava:
            self.sock.sendall(cabeca + mascara + bytes(b ^ mascara[i % 4] for i, b in enumerate(dados)))

    def enviar(self, texto):
        self._quadro(0x1, texto.encode())

    def fechar(self):
        try:
            self._quadro(0x8, struct.pack('>H', 1000))
        except OSError:
            pass

    def _ler(self, n):
        while len(self.sobra) < n:
            parte = self.sock.recv(4096)
            if not parte:
                raise ConnectionError('conexão fechada')
            self.sobra += parte
        dados, self.sobra = self.sobra[:n], self.sobra[n:]
        return dados

    def receber(self):
        """A próxima mensagem de texto (responde aos pings no caminho); None quando fecha."""
        while True:
            b0, b1 = self._ler(2)
            opcode, n = b0 & 0x0F, b1 & 0x7F
            if n == 126:
                n = struct.unpack('>H', self._ler(2))[0]
            elif n == 127:
                n = struct.unpack('>Q', self._ler(8))[0]
            dados = self._ler(n)
            if opcode == 0x1:
                return dados.decode()
            if opcode == 0x9:
                self._quadro(0xA, dados)
            elif opcode == 0x8:
                return None


def sala_valida(valor):
    if valor not in ('atual', 'livre') and not re.fullmatch(r'horda-[0-9]+', valor):
        raise argparse.ArgumentTypeError('use livre, horda-N ou atual')
    return valor


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--servidor', default='http://localhost:8080')
    p.add_argument('--nome', default='Fantasma')
    p.add_argument('--segundos', type=float, default=60)
    p.add_argument('--explodir-a-cada', type=float, default=0, metavar='S',
                   help='explode a cada S segundos e fica 2 s sem mandar posição, como a nave destruída do jogo')
    p.add_argument('--sala', type=sala_valida, help='livre, horda-N ou atual')
    p.add_argument('--sobreviver', action='store_true', help='declara sobrevivência no fim dos perigos')
    p.add_argument('--pegar-vip', action='store_true', help='disputa o VIP no início do evento')
    p.add_argument('--skin', metavar='ID', help='anota a skin esperada; a equipada vem do perfil')
    a = p.parse_args()
    servidor = a.servidor.rstrip('/')
    senha = os.environ.get('MISSAOMARTE_SENHA_FANTASMA', 'fantasma-so-de-teste')
    token = entrar(servidor, a.nome, senha)
    ws_url = ('wss://' + servidor[8:] if servidor.startswith('https://') else 'ws://' + servidor[7:]) + '/ws/sombras'
    sala = a.sala
    if sala == 'atual':
        with urllib.request.urlopen(servidor + '/api/hordas/atual', timeout=5) as resposta:
            numero = json.load(resposta)['numero']
        if type(numero) is not int or numero < 0:
            sys.exit('número de horda inválido')
        sala = f'horda-{numero}'
    if sala:
        ws_url += '?' + urllib.parse.urlencode({'sala': sala})
    if a.skin:
        print(f'Skin esperada: {a.skin} (equipada pelo perfil)', flush=True)
    ws = WebSocket(ws_url, token)
    del token
    print(f'{a.nome}: conectado a {ws_url}; voando por {a.segundos:.0f} s', flush=True)

    vistos = Counter()
    ultima = {}
    sairam = []
    explosoes = []

    temporizadores = {}
    fim = threading.Event()
    trava_eventos = threading.Lock()

    def resultado(evento):
        with trava_eventos:
            temporizadores.pop(evento, None)
            if not fim.is_set():
                ws.enviar(json.dumps({'tipo': 'evento-resultado', 'evento': evento, 'sobreviveu': True}))

    def ouvir():
        while True:
            try:
                texto = ws.receber()
            except (OSError, ConnectionError):
                return
            if texto is None:
                return
            m = json.loads(texto)
            if m.get('tipo') == 'sombra':
                if m['nome'] not in vistos or m.get('skin') != ultima[m['nome']].get('skin'):
                    print(f'  vê a sombra de {m["nome"]} (piloto {m["piloto"]}) em x={m["x"]:.0f} y={m["y"]:.0f} '
                          f'(skin: {m.get("skin", "não informada")})',
                          flush=True)
                vistos[m['nome']] += 1
                ultima[m['nome']] = m
            elif m.get('tipo') in ('evento-aviso', 'evento-inicio', 'vip-pego'):
                print(time.strftime('%H:%M:%S'), texto, flush=True)
                if m['tipo'] == 'evento-inicio':
                    evento = m['evento']
                    with trava_eventos:
                        if fim.is_set():
                            return
                        if m['nome'] == 'PASSAGEIRO_VIP':
                            if a.pegar_vip:
                                ws.enviar(json.dumps({'tipo': 'vip', 'evento': evento}))
                        elif a.sobreviver and evento not in temporizadores:
                            timer = threading.Timer(m['duracao'], resultado, args=(evento,))
                            temporizadores[evento] = timer
                            timer.start()
            elif m.get('tipo') == 'saiu':
                sairam.append(m['piloto'])
                print(f'  piloto {m["piloto"]} saiu', flush=True)
            elif m.get('tipo') == 'explosao':
                explosoes.append(m['piloto'])
                print(f'  piloto {m["piloto"]} explodiu em x={m["x"]:.0f} y={m["y"]:.0f}', flush=True)

    threading.Thread(target=ouvir, daemon=True).start()
    inicio = time.monotonic()
    enviadas = 0
    explodiu = 0
    proxima_explosao = a.explodir_a_cada if a.explodir_a_cada > 0 else math.inf
    while time.monotonic() - inicio < a.segundos:
        t = time.monotonic() - inicio
        fase = 2 * math.pi * t / VOLTA_SEGUNDOS
        x = CENTRO[0] + RAIO * math.cos(fase)
        y = CENTRO[1] + RAIO * math.sin(fase)
        if t >= proxima_explosao:
            ws.enviar(json.dumps({'tipo': 'explodiu', 'x': round(x, 1), 'y': round(y, 1)}))
            explodiu += 1
            print(f'{a.nome}: explodiu em x={x:.0f} y={y:.0f}', flush=True)
            time.sleep(2)   # destruída: o jogo não manda posição até renascer
            proxima_explosao = time.monotonic() - inicio + a.explodir_a_cada
            continue
        # o bico aponta para (-sen a, cos a); no círculo anti-horário, o movimento é (-sen fase, cos fase): a = fase
        angulo = (math.degrees(fase) + 180) % 360 - 180
        ws.enviar(json.dumps({'tipo': 'posicao', 'x': round(x, 1), 'y': round(y, 1), 'angulo': round(angulo, 1),
                              'motor': math.sin(fase * 2) > 0}))
        enviadas += 1
        time.sleep(0.1)
    with trava_eventos:
        fim.set()
        for timer in temporizadores.values():
            timer.cancel()
    ws.fechar()
    time.sleep(0.3)
    print(f'{a.nome}: {enviadas} posições enviadas; recebidas: '
          + (', '.join(f'{n} ({c}x, última em x={ultima[n]["x"]:.0f} y={ultima[n]["y"]:.0f})'
                       for n, c in vistos.items()) or 'nenhuma sombra')
          + (f'; saíram: {sairam}' if sairam else '')
          + (f'; explosões enviadas: {explodiu}' if explodiu else '')
          + (f'; explosões recebidas: {explosoes}' if explosoes else ''), flush=True)


if __name__ == '__main__':
    main()
