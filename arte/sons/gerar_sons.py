#!/usr/bin/env python3
"""Gera os efeitos sonoros do jogo por síntese (PCM), sem nenhum arquivo de fora.

Mesma ideia da seção "Sons procedurais com PCM" do referencial teórico: cada som é uma sequência de amostras
calculadas (senoides, ondas quadradas e triangulares, ruído) gravada como WAV 16 bits, mono, 44,1 kHz.
O ruído usa uma semente fixa, então rodar o script de novo produz exatamente os mesmos arquivos.

    python3 arte/sons/gerar_sons.py            # grava em assets/sons/
"""
import math
import random
import struct
import wave
from pathlib import Path

TAXA = 44100
PICO = 0.6                      # volume máximo (fração do maior valor de 16 bits)
SAIDA = Path(__file__).resolve().parents[2] / 'assets' / 'sons'


def tempo(segundos):
    return [i / TAXA for i in range(int(segundos * TAXA))]


def envelope(t, duracao, ataque=0.005, queda=4.0):
    """Sobe em `ataque` segundos e decai exponencialmente; zera no fim para não estalar."""
    subida = min(1.0, t / ataque) if ataque > 0 else 1.0
    fim = max(0.0, 1 - max(0.0, t - (duracao - 0.01)) / 0.01)
    return subida * math.exp(-queda * t / duracao) * fim


def seno(f, t):
    return math.sin(2 * math.pi * f * t)


def quadrada(f, t):
    return 1.0 if seno(f, t) >= 0 else -1.0


def triangular(f, t):
    fase = (f * t) % 1.0
    return 4 * abs(fase - 0.5) - 1


def ruido_filtrado(n, rng, suavidade):
    """Ruído branco passado por um filtro passa-baixa de um polo (quanto maior a suavidade, mais grave)."""
    saida, y = [], 0.0
    for _ in range(n):
        y += (rng.uniform(-1, 1) - y) / suavidade
        saida.append(y)
    pico = max(abs(v) for v in saida) or 1
    return [v / pico for v in saida]


def notas(sequencia, forma, duracao_nota, queda=3.0):
    amostras = []
    for f in sequencia:
        for t in tempo(duracao_nota):
            amostras.append(forma(f, t) * envelope(t, duracao_nota, 0.004, queda))
    return amostras


# ---------------------------------------------------------------- sons do jogo

def mover(rng):
    """Turno de movimento: sopro curto do propulsor."""
    d = 0.11
    ruido = ruido_filtrado(int(d * TAXA), rng, 6)
    return [0.7 * ruido[i] * envelope(t, d, 0.01, 3) + 0.3 * seno(140, t) * envelope(t, d, 0.005, 5)
            for i, t in enumerate(tempo(d))]


def embarque(rng):
    """Passageiro a bordo: duas notas subindo (Mi5, Si5)."""
    return notas([659.25, 987.77], lambda f, t: 0.7 * seno(f, t) + 0.3 * seno(2 * f, t), 0.12, 2.5)


def colisao(rng):
    """Colisão com perda de vida: baque grave e ruído."""
    d = 0.32
    ruido = ruido_filtrado(int(d * TAXA), rng, 3)
    return [0.55 * ruido[i] * envelope(t, d, 0.002, 6) + 0.6 * seno(70 * (1 - 0.4 * t / d), t) * envelope(t, d, 0.002, 4)
            for i, t in enumerate(tempo(d))]


def explosao(rng):
    """Nave destruída: estrondo longo e cada vez mais grave."""
    d = 0.9
    ruido = ruido_filtrado(int(d * TAXA), rng, 10)
    return [0.85 * ruido[i] * envelope(t, d, 0.003, 3.5) + 0.35 * seno(48, t) * envelope(t, d, 0.003, 3)
            for i, t in enumerate(tempo(d))]


def alerta(rng):
    """Todos a bordo: "volte à plataforma" (duas notas, aguda e grave)."""
    return notas([880.0, 660.0, 880.0], lambda f, t: 0.6 * triangular(f, t), 0.09, 1.5)


def invalido(rng):
    """Ação sem efeito (sem passageiro aqui, nave cheia, tecla inválida): zumbido curto e grave."""
    d = 0.12
    return [0.45 * quadrada(110, t) * envelope(t, d, 0.003, 2) for t in tempo(d)]


def vitoria(rng):
    """Missão cumprida: arpejo subindo (Dó5, Mi5, Sol5, Dó6)."""
    return notas([523.25, 659.25, 783.99, 1046.5], lambda f, t: 0.6 * triangular(f, t) + 0.4 * seno(f, t), 0.13, 1.8)


def derrota(rng):
    """Fim de jogo: três notas descendo (Lá4, Mi4, Lá3), como o "game over" do referencial."""
    return notas([440.0, 329.63, 220.0], lambda f, t: 0.8 * triangular(f, t), 0.2, 1.5)


SONS = {'mover': mover, 'embarque': embarque, 'colisao': colisao, 'explosao': explosao,
        'alerta': alerta, 'invalido': invalido, 'vitoria': vitoria, 'derrota': derrota}


def gravar(nome, amostras):
    pico = max(abs(a) for a in amostras) or 1
    escala = PICO * 32767 / pico
    caminho = SAIDA / f'{nome}.wav'
    with wave.open(str(caminho), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(TAXA)
        w.writeframes(b''.join(struct.pack('<h', round(a * escala)) for a in amostras))
    return caminho


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    for i, (nome, gerar) in enumerate(SONS.items()):
        amostras = gerar(random.Random(4200 + i))
        caminho = gravar(nome, amostras)
        print(f'{caminho.name:14} {len(amostras) / TAXA:5.2f} s  {caminho.stat().st_size:7} bytes')


if __name__ == '__main__':
    main()
