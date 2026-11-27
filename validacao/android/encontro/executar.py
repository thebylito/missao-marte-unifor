#!/usr/bin/env python3
"""Roda a arena instrumentada, grava vídeo e captura somente o app do protótipo."""
import argparse
import subprocess
import time
from pathlib import Path

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb',required=True)
parser.add_argument('--dispositivo',required=True)
parser.add_argument('--saida',type=Path,required=True)
a=parser.parse_args()
a.saida.mkdir(parents=True,exist_ok=True)
adb=[a.adb,'-s',a.dispositivo]
video='/data/local/tmp/missao-encontro-'+str(time.time_ns())+'.mp4'
linhas=[]
gravacao=None
indice=0
with subprocess.Popen(adb+['shell','am','instrument','-w',
        'br.edu.unifor.missaomarte.historia.test/ensaio.Encontro'],stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,text=True) as processo:
    for linha in processo.stdout:
        print(linha.strip(),flush=True)
        linhas.append(linha)
        if linha.startswith('INSTRUMENTATION_STATUS: etapa='):
            if gravacao is None:
                gravacao=subprocess.Popen(adb+['shell','screenrecord','--time-limit','45',
                    '--bit-rate','5000000',video],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            indice+=1
            time.sleep(.15)
            estado=subprocess.check_output(adb+['shell','dumpsys','activity','activities'],text=True)
            if any('ResumedActivity' in l and 'br.edu.unifor.missaomarte.historia/' in l for l in estado.splitlines()):
                (a.saida/f'{indice:02}.png').write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
log=''.join(linhas)
(a.saida/'instrumentacao.txt').write_text(log)
if gravacao:
    # Finaliza somente o screenrecord cujo arquivo temporário exclusivo pertence a este ensaio.
    processos=subprocess.check_output(adb+['shell','ps','-A','-o','PID,ARGS'],text=True)
    for l in processos.splitlines():
        if 'screenrecord' in l and video in l:
            subprocess.run(adb+['shell','kill','-2',l.strip().split()[0]],check=True)
    gravacao.wait(timeout=50)
    subprocess.run(adb+['pull',video,str(a.saida/'encontro.mp4')],check=True)
if processo.returncode!=0 or 'resultado=PASS' not in log:
    raise SystemExit('FAIL: veja instrumentacao.txt')
print('PASS: encontro instrumentado, imagens e vídeo registrados.')
