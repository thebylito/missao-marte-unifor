#!/usr/bin/env python3
"""Executa os três resgates locais e guarda logs e capturas do aplicativo."""
import argparse
import subprocess
import time
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--dispositivo', required=True)
    parser.add_argument('--saida', type=Path, required=True)
    parser.add_argument('--passageiro', type=int, choices=(1,2,3))
    parser.add_argument('--gravar', action='store_true')
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, '-s', args.dispositivo]
    for indice, nome in enumerate(('astronauta', 'engenheiro', 'professor'), 1):
        if args.passageiro is not None and indice != args.passageiro:
            continue
        comando = adb + ['shell', 'am', 'instrument', '-w', '-e', 'passageiro', str(indice),
                         '-e', 'skin', ('padrao','guara','padrao_5')[indice-1],
                         'br.edu.unifor.missaomarte.test/ensaio.Polar']
        linhas = []
        captura = 0
        gravacao = None
        video_remoto = f"/data/local/tmp/missao-marte-{nome}-{time.time_ns()}.mp4"
        with subprocess.Popen(comando, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True) as processo:
            for linha in processo.stdout:
                linhas.append(linha)
                print(nome, linha.strip(), flush=True)
                if args.gravar and gravacao is None and 'Jato ao alcance' in linha:
                    gravacao = subprocess.Popen(adb + ['shell', 'screenrecord', '--time-limit', '12',
                            '--bit-rate', '5000000', video_remoto],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                if linha.startswith('INSTRUMENTATION_STATUS: etapa='):
                    if "carregado" in linha:
                        time.sleep(.3)
                    captura += 1
                    estado = subprocess.check_output(adb + ['shell', 'dumpsys', 'activity', 'activities'], text=True)
                    em_primeiro_plano = any(
                        ('topResumedActivity' in item or 'mResumedActivity' in item)
                        and 'br.edu.unifor.missaomarte/' in item for item in estado.splitlines())
                    if em_primeiro_plano:
                        dados = subprocess.check_output(adb + ['exec-out', 'screencap', '-p'])
                        (args.saida / f'{nome}-{captura:02}.png').write_bytes(dados)
        log = ''.join(linhas)
        (args.saida / f'{nome}.txt').write_text(log)
        if gravacao is not None:
            gravacao.wait(timeout=20)
            if processo.returncode == 0 and 'resultado=PASS' in log:
                subprocess.run(adb + ['pull', video_remoto, str(args.saida / f'{nome}.mp4')], check=True)
        if processo.returncode != 0 or 'resultado=PASS' not in log:
            raise RuntimeError(f'O ensaio do {nome} falhou; veja o log.')
    print('PASS: ciclos solicitados concluídos.', flush=True)

if __name__ == '__main__':
    main()
