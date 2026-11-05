#!/usr/bin/env python3
"""Prova de equivalência: joga as mesmas partidas, com a mesma semente, em duas versões do jogo e compara tudo.

Com a mesma semente, as duas versões sorteiam o mesmo mapa e movem os inimigos da mesma forma. Como o piloto
automático é determinístico, o diálogo inteiro (mapas, mensagens, pontos) precisa sair igual. Só a linha
"Tempo de Jogo" é mascarada, porque depende do relógio real.

Com --so-saida, as teclas enviadas pelo piloto são retiradas do log e compara-se só o texto impresso, na
ordem. Serve quando uma versão imprime algo mais cedo do que a outra (na Fase 1 o menu passou a aparecer
antes de o jogador digitar): o texto é o mesmo, só muda onde a tecla aparece no log.

    python3 validacao/comparar_com_semente.py --sementes 1-40 \\
        --referencia "java -Dmissaomarte.semente={semente} -cp /tmp/fase0 Main" \\
        --candidato  "java -Dmissaomarte.semente={semente} -cp build/classes br.edu.unifor.missaomarte.Main"

A Fase 0 original usa `new Random()` sem semente; para servir de referência ela é compilada a partir de uma
cópia temporária com `new Random(Long.getLong("missaomarte.semente"))` (a pasta src/ da tag fase-0 não muda).
"""
import argparse
import difflib
import re
import shlex
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

PILOTO = Path(__file__).resolve().parent / 'piloto_automatico.py'
CENARIOS = [  # (modo, dificuldade, tamanho)
    ('vencer', 'facil', 2), ('vencer', 'medio', 3), ('vencer', 'dificil', 3),
    ('colidir', 'dificil', 2), ('esgotar', 'medio', 4),
]


def faixa(texto):
    ini, _, fim = texto.partition('-')
    return range(int(ini), int(fim or ini) + 1)


def jogar(modelo, semente, cenario, pasta, so_saida=False):
    modo, dificuldade, tamanho = cenario
    log = Path(pasta) / f'{semente}-{modo}-{dificuldade}.log'
    comando = shlex.split(modelo.format(semente=semente))
    subprocess.run([sys.executable, str(PILOTO), '--partidas', '1', '--modo', modo, '--dificuldade', dificuldade,
                    '--tamanho', str(tamanho), '--log', str(log), '--', *comando],
                   capture_output=True, timeout=300)
    texto = log.read_text(encoding='utf-8') if log.exists() else '<sem log>'
    texto = re.sub(r'Tempo de Jogo: \d+ segundos', 'Tempo de Jogo: <t> segundos', texto)
    return re.sub(r'<<[^>]*>>\n', '', texto) if so_saida else texto


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--sementes', default='1-20')
    p.add_argument('--referencia', required=True)
    p.add_argument('--candidato', required=True)
    p.add_argument('--paralelo', type=int, default=6)
    p.add_argument('--so-saida', action='store_true', help='compara só o texto impresso, sem as teclas')
    a = p.parse_args()

    casos = [(s, c) for s in faixa(a.sementes) for c in CENARIOS]
    with tempfile.TemporaryDirectory() as ref, tempfile.TemporaryDirectory() as cand, \
            ThreadPoolExecutor(a.paralelo) as pool:
        futuros = [(s, c, pool.submit(jogar, a.referencia, s, c, ref, a.so_saida),
                    pool.submit(jogar, a.candidato, s, c, cand, a.so_saida))
                   for s, c in casos]
        diferentes, finais = 0, {}
        for s, c, fr, fc in futuros:
            r, k = fr.result(), fc.result()
            fim = next((f for f in ('Missão cumprida!', 'GAME OVER!', 'Missão perdida.') if f in r), '?')
            finais[fim] = finais.get(fim, 0) + 1
            if r != k:
                diferentes += 1
                print(f'DIFERENTE semente={s} cenário={c}')
                sys.stdout.writelines(list(difflib.unified_diff(
                    r.splitlines(True), k.splitlines(True), 'referência', 'candidato', n=3))[:60])
    print(f'{len(casos) - diferentes}/{len(casos)} partidas idênticas '
          f'({len(faixa(a.sementes))} sementes x {len(CENARIOS)} cenários; finais na referência: {finais})')
    return 1 if diferentes else 0


if __name__ == '__main__':
    sys.exit(main())
