"""Exercita os três CLIs com HTTP e quadros WebSocket em localhost."""
import base64
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

VALIDACAO = Path(__file__).resolve().parents[1]
FANTASMA = 'sombras/piloto_fantasma.py'
LOJA = 'loja/comprar_e_equipar.py'
ADMIN = 'eventos/disparar_evento.py'


class Servidor(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def responder(self, status, objeto):
        self.send_response(status)
        self.end_headers()
        self.wfile.write(json.dumps(objeto).encode())

    def atender(self):
        s = self.server
        corpo = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))) or b'{}')
        if self.path == '/api/sessoes' and corpo == {'nome': 'Teste', 'senha': 'senha-local'}:
            return self.responder(200, {'token': 'local'})
        if self.path == '/api/hordas/atual':
            return self.responder(200, {'numero': 42})
        if self.headers.get('Authorization') != 'Bearer local':
            return self.responder(401, {})
        if self.path.startswith('/ws/sombras'):
            return self.canal()
        if self.path == '/api/loja':
            return self.responder(200, [{'id': 'guara', 'preco': 60}])
        if self.path == '/api/jogadores/eu/hangar':
            return self.responder(200, s.hangar)
        if self.command == 'POST' and self.path == '/api/admin/eventos':
            return self.responder(s.status_evento, corpo)
        if corpo == {'skin': 'guara'}:
            if self.command == 'POST' and self.path == '/api/loja/compras':
                s.compras += 1
                s.hangar['saldo'] -= 60
                s.hangar['skins'].append('guara')
                return self.responder(201, s.hangar)
            if self.command == 'PUT' and self.path == '/api/jogadores/eu/skin':
                s.hangar['equipada'] = 'guara'
                return self.responder(200, {'equipada': 'guara'})
        self.responder(404, {})

    do_POST = do_GET = do_PUT = atender

    def canal(self):
        self.server.salas.append(self.path)
        self.protocol_version = 'HTTP/1.1'
        self.send_response_only(101)
        self.send_header('Upgrade', 'websocket')
        self.send_header('Connection', 'Upgrade')
        chave = self.headers['Sec-WebSocket-Key'] + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'
        self.send_header('Sec-WebSocket-Accept', base64.b64encode(hashlib.sha1(chave.encode()).digest()).decode())
        self.end_headers()
        inicio = time.monotonic()
        mensagens = [
            dict(tipo='sombra', piloto=2, nome='Ana', skin='guara', x=10, y=20),
            dict(tipo='evento-aviso', evento='p', nome='TEMPESTADE', em=15),
            dict(tipo='evento-inicio', evento='p', nome='TEMPESTADE', duracao=1),
            dict(tipo='evento-inicio', evento='v', nome='PASSAGEIRO_VIP', duracao=1),
            dict(tipo='vip-pego', evento='v', nome='Ana'),
        ]
        for msg in mensagens:
            dados = json.dumps(msg).encode()
            self.wfile.write(bytes([129, len(dados)]) + dados)
        while True:
            opcode, tamanho = self.rfile.read(2)
            n = tamanho & 127
            if n == 126:
                n, = struct.unpack('>H', self.rfile.read(2))
            mascara = self.rfile.read(4)
            dados = bytes(b ^ mascara[i % 4] for i, b in enumerate(self.rfile.read(n)))
            if opcode == 136:
                self.close_connection = True
                self.wfile.write(b'\x88\x00')
                return
            self.server.recebidas.append((time.monotonic() - inicio, json.loads(dados)))


class TesteLocal(unittest.TestCase):
    def setUp(self):
        self.s = ThreadingHTTPServer(('127.0.0.1', 0), Servidor)
        threading.Thread(target=self.s.serve_forever, daemon=True).start()
        self.addCleanup(self.s.server_close)
        self.addCleanup(self.s.shutdown)
        self.s.recebidas = []
        self.s.salas = []
        self.s.compras = 0
        self.s.hangar = dict(saldo=60, skins=['padrao'], equipada='padrao')

    def rodar(self, script, *args, codigo=0):
        env = os.environ.copy()
        for conta in ('PILOTO', 'ADMIN', 'FANTASMA'):
            env['MISSAOMARTE_SENHA_' + conta] = 'senha-local'
        r = subprocess.run([sys.executable, '-I', str(VALIDACAO / script), '--servidor',
                            f'http://127.0.0.1:{self.s.server_port}', '--nome', 'Teste', *args],
                           capture_output=True, text=True, env=env, timeout=10)
        self.assertEqual(r.returncode, codigo, r.stdout + r.stderr)
        return r.stdout + r.stderr

    def test_salas_e_eventos(self):
        for opcoes, sala in [((), ''), (('--sala', 'livre'), '?sala=livre'),
                             (('--sala', 'horda-17'), '?sala=horda-17')]:
            self.rodar(FANTASMA, '--segundos', '0.2', *opcoes)
            self.assertEqual(self.s.salas[-1], '/ws/sombras' + sala)
        self.assertTrue(all(m['tipo'] == 'posicao' for _, m in self.s.recebidas))
        saida = self.rodar(FANTASMA, '--sala', 'atual', '--sobreviver', '--pegar-vip',
                           '--skin', 'guara', '--segundos', '1.4')
        self.assertEqual(self.s.salas[-1], '/ws/sombras?sala=horda-42')
        for tipo in ('evento-aviso', 'evento-inicio', 'vip-pego'):
            self.assertRegex(saida, r'\d\d:\d\d:\d\d .*' + tipo)
        self.assertIn('skin: guara', saida)
        (vip, coleta), (fim, resultado) = [(t, m) for t, m in self.s.recebidas if m['tipo'] != 'posicao']
        self.assertLess(vip, 1)
        self.assertEqual(coleta, dict(tipo='vip', evento='v'))
        self.assertGreaterEqual(fim, 1)
        self.assertEqual(resultado, dict(tipo='evento-resultado', evento='p', sobreviveu=True))

    def test_loja(self):
        self.s.hangar['saldo'] = 59
        self.assertIn('Saldo insuficiente', self.rodar(LOJA, '--skin', 'guara', codigo=1))
        self.assertEqual(self.s.compras, 0)
        self.s.hangar['saldo'] = 60
        for _ in range(2):
            self.assertIn('Saldo: 0 | Skins: padrao, guara | Equipada: guara', self.rodar(LOJA, '--skin', 'guara'))
        self.assertEqual(self.s.compras, 1)

    def test_admin(self):
        for status, codigo in [(201, 0), (403, 1)]:
            self.s.status_evento = status
            self.assertIn(f'HTTP {status}\n{{"tipo": "TEMPESTADE"}}',
                          self.rodar(ADMIN, '--tipo', 'TEMPESTADE', codigo=codigo))


if __name__ == '__main__':
    unittest.main()
