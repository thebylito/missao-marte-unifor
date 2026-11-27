#!/usr/bin/env python3
"""Valida M01–M03 pela interface Android e o replay M02 após reabrir o processo.

Requer os APKs de validação já instalados. Não instala, não limpa dados nem altera
o save; somente o próprio jogo grava o progresso ao jogar as missões.
"""
import argparse
import queue
import re
import subprocess
import threading
import time
from pathlib import Path

PACOTE = 'br.edu.unifor.missaomarte.historia'
RUNNER = PACOTE + '.test/ensaio.CapituloUm'


class Gravador:
    """Segmentos por missão, renovados antes do limite de 175 s do Android."""

    def __init__(self, adb, saida, modo):
        self.adb, self.saida, self.modo = adb, saida, modo
        self.segmento = 'abertura' if modo == 'voo' else 'replay-M02'
        self.finalizar = threading.Event()
        self.arquivos = []
        self.erro = None
        self.thread = threading.Thread(target=self._gravar, daemon=True)

    def iniciar(self):
        self.thread.start()

    def selecionar(self, missao):
        if self.modo == 'voo':
            self.segmento = missao

    def _parar(self, processo, remoto):
        if processo.poll() is None:
            # Só encerra o screenrecord que contém nosso caminho único.
            processos = subprocess.check_output(
                self.adb + ['shell', 'ps', '-A', '-o', 'PID,ARGS'], text=True, timeout=10)
            for linha in processos.splitlines():
                campos = linha.strip().split()
                if campos and 'screenrecord' in linha and remoto in linha:
                    subprocess.run(self.adb + ['shell', 'kill', '-2', campos[0]],
                                   check=True, timeout=10)
            try:
                processo.wait(timeout=10)
            except subprocess.TimeoutExpired:
                processo.terminate()
                processo.wait(timeout=5)

    def _gravar(self):
        processo = None
        remoto = None
        try:
            indice = 0
            while not self.finalizar.is_set():
                segmento = self.segmento
                indice += 1
                nome = f'{self.modo}-{segmento}-{indice:02}.mp4'
                remoto = f'/data/local/tmp/capitulo1-{time.time_ns()}-{nome}'
                processo = subprocess.Popen(self.adb + ['shell', 'screenrecord',
                    '--time-limit', '175', '--bit-rate', '4000000', remoto],
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                self.arquivos.append((remoto, self.saida / nome))
                inicio = time.monotonic()
                while not self.finalizar.wait(.1):
                    if self.segmento != segmento or time.monotonic() - inicio >= 170:
                        break
                    if processo.poll() is not None:
                        raise RuntimeError(f'screenrecord terminou antes do limite: {nome}')
                self._parar(processo, remoto)
                processo = None
        except Exception as erro:
            self.erro = erro
        finally:
            if processo is not None:
                try:
                    self._parar(processo, remoto)
                except Exception as erro:
                    self.erro = self.erro or erro

    def fechar(self):
        self.finalizar.set()
        self.thread.join(timeout=35)
        if self.thread.is_alive():
            raise RuntimeError('screenrecord não encerrou no prazo')
        for remoto, local in self.arquivos:
            subprocess.run(self.adb + ['pull', remoto, str(local)], check=True, timeout=30)
            if not local.exists() or local.stat().st_size == 0:
                raise RuntimeError(f'vídeo vazio: {local}')
        if self.erro:
            raise RuntimeError(f'falha na captura de vídeo: {self.erro}')


def executar(adb, saida, modo, esperados):
    subprocess.run(adb + ['shell', 'am', 'force-stop', PACOTE], check=True, timeout=15)
    comando = adb + ['shell', 'am', 'instrument', '-w', '-e', 'modo', modo]
    for missao, pontos in esperados.items():
        comando += ['-e', 'esperado_' + missao, str(pontos)]
    comando += [RUNNER]
    linhas, pontuacoes = [], {}
    indice = 0
    recebidas = queue.Queue()
    gravador = Gravador(adb, saida, modo)
    gravador.iniciar()
    processo = subprocess.Popen(comando, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, bufsize=1)

    def ler():
        try:
            for linha in processo.stdout:
                recebidas.put(linha)
        finally:
            recebidas.put(None)

    leitor = threading.Thread(target=ler, daemon=True)
    leitor.start()
    erro = None
    prazo = time.monotonic() + (450 if modo == 'voo' else 60) + 30
    try:
        while True:
            if time.monotonic() > prazo:
                raise TimeoutError(f'{modo}: instrumentação excedeu o prazo do host')
            try:
                linha = recebidas.get(timeout=.2)
            except queue.Empty:
                continue
            if linha is None:
                break
            print(modo, linha.rstrip(), flush=True)
            linhas.append(linha)
            etapa = re.match(r'INSTRUMENTATION_STATUS: etapa=(M0[123]): (.*)', linha)
            if etapa:
                missao, descricao = etapa.groups()
                gravador.selecionar(missao)
                indice += 1
                time.sleep(.18)  # a tela desenha o estado anunciado no próximo quadro
                estado = subprocess.check_output(
                    adb + ['shell', 'dumpsys', 'activity', 'activities'], text=True, timeout=10)
                if not any('ResumedActivity' in l and PACOTE + '/' in l
                           for l in estado.splitlines()):
                    # finish() pode fechar a Activity antes de o host consumir o último marco.
                    # Resultado funcional continua vindo da instrumentação; não capturar outro app.
                    with (saida / f'{modo}-snapshots.tsv').open('a') as manifesto:
                        manifesto.write(f'NAO_CAPTURADO\t{descricao} (atividade fora do primeiro plano)\n')
                    continue
                png = subprocess.check_output(adb + ['exec-out', 'screencap', '-p'], timeout=10)
                if not png.startswith(b'\x89PNG\r\n\x1a\n'):
                    raise RuntimeError('screencap não retornou PNG')
                (saida / f'{modo}-{indice:02}-{missao}.png').write_bytes(png)
                with (saida / f'{modo}-snapshots.tsv').open('a') as manifesto:
                    manifesto.write(f'{modo}-{indice:02}-{missao}.png\t{descricao}\n')
            pontos = re.match(r'INSTRUMENTATION_STATUS: pontuacoes=(.*)', linha)
            if pontos:
                pontuacoes = {m: int(p) for m, p in re.findall(r'(M0[123]):(\d+)', pontos.group(1))}
        processo.wait(timeout=10)
    except Exception as falha:
        erro = falha
        # O único pacote interrompido é o APK isolado da validação.
        subprocess.run(adb + ['shell', 'am', 'force-stop', PACOTE], check=False, timeout=15)
        processo.terminate()
        processo.wait(timeout=10)
    finally:
        (saida / f'{modo}.txt').write_text(''.join(linhas), encoding='utf-8')
        gravador.fechar()
    if erro:
        raise erro
    if processo.returncode != 0 or not any(
            l.strip() == 'INSTRUMENTATION_RESULT: resultado=PASS' for l in linhas):
        raise RuntimeError(f'FAIL: {modo}; veja {saida / (modo + ".txt")}')
    if set(pontuacoes) != {'M01', 'M02', 'M03'}:
        raise RuntimeError(f'{modo}: evidência das três pontuações ausente')
    if esperados and pontuacoes != esperados:
        raise RuntimeError('pontuações diferentes entre voo e processo reaberto')
    return pontuacoes


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--dispositivo', required=True)
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, '-s', args.dispositivo]
    pontos = executar(adb, args.saida, 'voo', {})
    executar(adb, args.saida, 'reabrir', pontos)
    print('PASS: M01–M03 em voo contínuo; capítulo salvo; replay M02 preservou as três pontuações.')


if __name__ == '__main__':
    main()
