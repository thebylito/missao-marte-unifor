#!/usr/bin/env python3
"""Valida voo contínuo da M01 e preservação do progresso em um novo processo."""
import argparse
import subprocess
import time
from pathlib import Path

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',required=True)
p.add_argument('--dispositivo',required=True)
p.add_argument('--saida',type=Path,required=True)
a=p.parse_args()
a.saida.mkdir(parents=True,exist_ok=True)
adb=[a.adb,'-s',a.dispositivo]
for modo in ('voo','reabrir'):
    subprocess.run(adb+['shell','am','force-stop','br.edu.unifor.missaomarte.historia'],check=True)
    remoto='/data/local/tmp/m01-'+modo+'-'+str(time.time_ns())+'.mp4'
    linhas=[]
    gravacao=None
    indice=0
    with subprocess.Popen(adb+['shell','am','instrument','-w','-e','modo',modo,
            'br.edu.unifor.missaomarte.historia.test/ensaio.Missao01'],stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,text=True) as processo:
        for linha in processo.stdout:
            print(modo,linha.strip(),flush=True);linhas.append(linha)
            if linha.startswith('INSTRUMENTATION_STATUS: etapa='):
                if gravacao is None:
                    gravacao=subprocess.Popen(adb+['shell','screenrecord','--time-limit','175','--bit-rate','4000000',remoto],
                        stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                indice+=1
                time.sleep(.18)
                estado=subprocess.check_output(adb+['shell','dumpsys','activity','activities'],text=True)
                if any('ResumedActivity' in l and 'br.edu.unifor.missaomarte.historia/' in l for l in estado.splitlines()):
                    (a.saida/f'{modo}-{indice:02}.png').write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
    log=''.join(linhas)
    (a.saida/f'{modo}.txt').write_text(log)
    if gravacao:
        processos=subprocess.check_output(adb+['shell','ps','-A','-o','PID,ARGS'],text=True)
        for l in processos.splitlines():
            if 'screenrecord' in l and remoto in l:
                subprocess.run(adb+['shell','kill','-2',l.strip().split()[0]],check=True)
        gravacao.wait(timeout=180)
        subprocess.run(adb+['pull',remoto,str(a.saida/f'{modo}.mp4')],check=True)
    if processo.returncode!=0 or 'resultado=PASS' not in log:
        raise SystemExit('FAIL: '+modo+'; veja os logs.')
print('PASS: voo contínuo, conclusão salva e leitura em novo processo.')
