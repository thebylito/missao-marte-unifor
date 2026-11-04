#!/usr/bin/env python3
"""Joga partidas completas do Missão Marte de console e confere as regras observáveis.

Complementa executar_roteiros.py: lá a saída tem que ser idêntica ao gabarito; aqui o mapa é sorteado,
então o piloto lê o mapa a cada turno, decide o comando e confere invariantes:

  - pontos = inicial - movimentos + soma dos passageiros embarcados (10/15/20) em toda tela;
  - vidas só caem em colisão; com 0 vidas a partida termina com GAME OVER;
  - vitória só com todos embarcados e a nave em (0,0); ranking.json recebe a entrada;
  - pontos <= 0 encerram a partida.

Modos (estratégias de pilotagem):
  vencer    - busca o passageiro mais próximo desviando de # e X e volta para (0,0)
  colidir   - procura o asteroide mais próximo e bate nele até acabar as vidas
  esgotar   - anda de um lado para o outro até os pontos zerarem

    python3 validacao/piloto_automatico.py --partidas 20 --modo vencer -- java -cp out Main
"""
import argparse
import json
import os
import re
import select
import subprocess
import sys
import tempfile
import time
from collections import deque
from pathlib import Path

PONTOS_INICIAIS = {'facil': 30, 'medio': 20, 'dificil': 45}
VALOR = {'Professor': 10, 'Engenheiro': 15, 'Astronauta': 20}
RE_STATUS = re.compile(r'Nave em \((-?\d+),(-?\d+)\) \| Pontos: (-?\d+) \| Vidas: (\d+) \| A bordo: (\d+)/(\d+)')
RE_PASS = re.compile(r'^ - (.+) \((\w+)\) em \((-?\d+),(-?\d+)\)$', re.M)
RE_CABECALHO = re.compile(r'^\s{4}((?:\s+-?\d+)+)\s*$', re.M)
RE_LINHA = re.compile(r'^\s*(-?\d+)\|(.*)$', re.M)
RE_EMBARQUE = re.compile(r'Passageiro (.+) embarcado com sucesso! \+(\d+) pontos!')
FIM = ('Missão cumprida!', 'GAME OVER!', 'Missão perdida.', 'Missão abortada')


class Jogo:
    def __init__(self, comando, cwd):
        self.p = subprocess.Popen(comando, cwd=cwd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                  stderr=subprocess.STDOUT)
        os.set_blocking(self.p.stdout.fileno(), False)
        self.log = []

    def ler(self, ocioso=0.25, limite=10.0):
        """Lê até o processo ficar ocioso (o prompt não termina em quebra de linha)."""
        partes, inicio, ultimo = [], time.time(), time.time()
        while time.time() - inicio < limite:
            pronto, _, _ = select.select([self.p.stdout], [], [], 0.05)
            if pronto:
                bloco = self.p.stdout.read()
                if bloco:
                    partes.append(bloco)
                    ultimo = time.time()
                    continue
                if self.p.poll() is not None:
                    break
            if partes and time.time() - ultimo > ocioso:
                break
        texto = b''.join(partes).decode('utf-8', errors='replace')
        self.log.append(texto)
        return texto

    def enviar(self, linha):
        self.log.append(f'<<{linha}>>\n')
        self.p.stdin.write((linha + '\n').encode())
        self.p.stdin.flush()

    def fechar(self):
        try:
            self.p.stdin.close()
        except BrokenPipeError:
            pass
        self.p.wait(timeout=10)


def ler_tela(texto):
    """Extrai a última tela de turno: grade, passageiros e status."""
    i = texto.rfind('Mapa da Missão')
    if i < 0:
        return None
    tela = texto[i:]
    st = RE_STATUS.search(tela)
    cab = RE_CABECALHO.search(tela)
    if not st or not cab:
        return None
    xs = [int(v) for v in cab.group(1).split()]
    grade = {}
    for m in RE_LINHA.finditer(tela):
        y, resto = int(m.group(1)), m.group(2)
        for k, x in enumerate(xs):
            grade[(x, y)] = resto[3 * k + 2] if len(resto) > 3 * k + 2 else '?'
    passageiros = [(n, t, int(x), int(y)) for n, t, x, y in RE_PASS.findall(tela)]
    x, y, pts, vidas, bordo, cap = map(int, st.groups())
    return {'nave': (x, y), 'pontos': pts, 'vidas': vidas, 'bordo': bordo, 'cap': cap,
            'grade': grade, 'passageiros': passageiros, 'lim': (min(xs), max(xs))}


PASSO = {'w': (0, -1), 's': (0, 1), 'a': (-1, 0), 'd': (1, 0)}


def rota(tela, alvo, evitar):
    """BFS na grade até o alvo, evitando as células com os símbolos em `evitar`."""
    lo, hi = tela['lim']
    ini = tela['nave']
    fila, veio = deque([ini]), {ini: None}
    while fila:
        c = fila.popleft()
        if c == alvo:
            break
        for cmd, (dx, dy) in PASSO.items():
            n = (c[0] + dx, c[1] + dy)
            if not (lo <= n[0] <= hi and lo <= n[1] <= hi) or n in veio:
                continue
            if n != alvo and tela['grade'].get(n) in evitar:
                continue
            veio[n] = (c, cmd)
            fila.append(n)
    if alvo not in veio:
        return None
    cmds, c = [], alvo
    while veio[c]:
        c, cmd = veio[c]
        cmds.append(cmd)
    return cmds[::-1]


def decidir(tela, modo, turno):
    nave = tela['nave']
    if modo == 'esgotar':
        lo, hi = tela['lim']
        return 'd' if turno % 2 == 0 and nave[0] < hi else 'a'
    if modo == 'colidir':
        asteroides = [c for c, s in tela['grade'].items() if s == '#']
        for a in sorted(asteroides, key=lambda c: abs(c[0] - nave[0]) + abs(c[1] - nave[1])):
            r = rota(tela, a, '#X')
            if r:
                return r[0]
        # a nave já está em cima do asteroide (o '@' esconde o '#'): sai e volta
        return 'd' if nave[0] < tela['lim'][1] else 'a'
    # vencer
    for n, t, x, y in tela['passageiros']:
        if (x, y) == nave:
            return 'c'
    alvos = [(x, y) for _, _, x, y in tela['passageiros']] or [(0, 0)]
    for alvo in sorted(alvos, key=lambda c: abs(c[0] - nave[0]) + abs(c[1] - nave[1])):
        r = rota(tela, alvo, '#X') or rota(tela, alvo, '#')
        if r:
            return r[0]
    return 'w' if nave[1] > tela['lim'][0] else 's'


def partida(comando, modo, dificuldade, tamanho, n):
    erros = []
    with tempfile.TemporaryDirectory() as tmp:
        j = Jogo(comando, tmp)
        j.ler()
        for linha in ('1', f'Piloto{n}', dificuldade, str(tamanho), ''):
            j.enviar(linha)
        texto = j.ler()
        esperado = PONTOS_INICIAIS[dificuldade]
        movimentos, vidas, fim, turno = 0, 3, None, 0
        while turno < 400:
            tela = ler_tela(texto)
            if tela is None:
                erros.append('não achei a tela do turno')
                break
            if tela['pontos'] != esperado:
                erros.append(f'turno {turno}: pontos {tela["pontos"]} != esperado {esperado}')
                esperado = tela['pontos']
            if tela['vidas'] > vidas:
                erros.append(f'turno {turno}: vidas aumentaram')
            vidas = tela['vidas']
            cmd = decidir(tela, modo, turno)
            j.enviar(cmd)
            texto = j.ler()
            turno += 1
            if cmd in PASSO:
                movimentos += 1
                esperado -= 1
            m = RE_EMBARQUE.search(texto)
            if m:
                tipo = next((t for nm, t, _, _ in tela['passageiros'] if nm == m.group(1)), None)
                if int(m.group(2)) != VALOR.get(tipo):
                    erros.append(f'{m.group(1)} ({tipo}) valeu {m.group(2)}')
                esperado += int(m.group(2))
            if 'Alerta! Colisão detectada!' in texto and vidas - 1 != int(
                    re.search(r'Vidas restantes: (\d+)', texto).group(1)):
                erros.append('vida não caiu exatamente 1')
            fim = next((f for f in FIM if f in texto), None)
            if fim:
                break
        # confere o fim
        if fim == 'Missão cumprida!':
            if 'Nave acoplada à plataforma em (0,0)' not in texto:
                erros.append('vitória sem acoplar em (0,0)')
            stats = re.search(r'Pontuação Final: (-?\d+) pontos.*?Movimentos Efetuados: (\d+)', texto, re.S)
            if not stats or int(stats.group(1)) != esperado or int(stats.group(2)) != movimentos:
                erros.append(f'estatísticas {stats and stats.groups()} != ({esperado}, {movimentos})')
        elif fim == 'GAME OVER!' and 'Vidas restantes' not in ''.join(j.log) and vidas != 1:
            erros.append('game over sem perder as vidas antes')
        elif fim == 'Missão perdida.' and esperado > 0:
            erros.append(f'perdeu por pontos com {esperado} pontos')
        elif fim is None:
            erros.append('a partida não terminou em 400 turnos')
        j.enviar('4')
        j.ler(limite=3)
        j.fechar()
        ranking = Path(tmp, 'ranking.json')
        entradas = json.loads(ranking.read_text('utf-8')) if ranking.exists() else []
        if fim == 'Missão cumprida!' and esperado > 0 and not any(e['score'] == esperado for e in entradas):
            erros.append('vitória não gravada no ranking.json')
        if fim != 'Missão cumprida!' and entradas:
            erros.append('ranking gravado sem vitória')
    return {'fim': fim, 'pontos': esperado, 'movimentos': movimentos, 'turnos': turno,
            'erros': erros, 'log': ''.join(j.log)}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--partidas', type=int, default=10)
    p.add_argument('--modo', choices=['vencer', 'colidir', 'esgotar'], default='vencer')
    p.add_argument('--dificuldade', choices=list(PONTOS_INICIAIS), default='facil')
    p.add_argument('--tamanho', type=int, default=2)
    p.add_argument('--log', type=Path, help='grava o diálogo da primeira partida neste arquivo')
    p.add_argument('comando', nargs=argparse.REMAINDER)
    a = p.parse_args()
    comando = a.comando[1:] if a.comando[:1] == ['--'] else a.comando

    finais, com_erro = {}, 0
    for n in range(a.partidas):
        r = partida(comando, a.modo, a.dificuldade, a.tamanho, n)
        finais[r['fim']] = finais.get(r['fim'], 0) + 1
        if n == 0 and a.log:
            a.log.parent.mkdir(parents=True, exist_ok=True)
            a.log.write_text(r['log'], encoding='utf-8')
        if r['erros']:
            com_erro += 1
            print(f'partida {n}: {r["fim"]} — ERROS: {r["erros"]}')
    print(f'modo={a.modo} dificuldade={a.dificuldade} tamanho={a.tamanho}: '
          f'{a.partidas} partidas, finais={finais}, partidas com regra violada={com_erro}')
    return 1 if com_erro else 0


if __name__ == '__main__':
    sys.exit(main())
