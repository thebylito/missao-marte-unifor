"""Sons discretos de caverna e variação do embarque, PCM original."""
import math
import random
from gerar_sons import gravar, tempo, envelope, embarque

def ambiente(tipo):
    rng = random.Random(814)
    duracao = 1.6
    resultado = []
    for t in tempo(duracao):
        if tipo == 'gelo':
            pulso = math.exp(-abs(t-.24)*24) + .5*math.exp(-abs(t-.72)*30)
            valor = pulso * (math.sin(t*2*math.pi*1800) + .35*rng.uniform(-1,1))
        elif tipo == 'ferro':
            valor = sum(math.sin(t*2*math.pi*f)*math.exp(-t*d)
                        for f,d in [(116,3),(291,5),(497,7)])
        else:
            valor = rng.uniform(-1,1)*.3 + math.sin(t*2*math.pi*61)*.2
        resultado.append(valor*envelope(t,duracao,.025,1))
    # gravar normaliza o pico; diminuir as amostras PCM depois preserva o ambiente discreto.
    caminho = gravar('ambiente_'+tipo,resultado)
    import wave, struct
    with wave.open(str(caminho),'rb') as w:
        params=w.getparams(); dados=w.readframes(w.getnframes())
    valores=struct.unpack('<'+'h'*(len(dados)//2),dados)
    with wave.open(str(caminho),'wb') as w:
        w.setparams(params)
        w.writeframes(struct.pack('<'+'h'*len(valores),*(round(v*.18) for v in valores)))

if __name__ == '__main__':
    for tipo in ('marte','gelo','ferro'): ambiente(tipo)
    gravar('embarque_alternativo',[math.sin(t*2*math.pi*(740 if t<.11 else 1110))*envelope(t,.27,.006,2) for t in tempo(.27)])
