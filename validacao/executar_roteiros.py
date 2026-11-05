#!/usr/bin/env python3
"""Executa o jogo de console com roteiros de entrada fixos e compara a saída com um gabarito.

Serve para qualquer fase: o comando que inicia o jogo é passado depois de "--".

    # gera o gabarito a partir da Fase 0
    python3 validacao/executar_roteiros.py --gerar validacao/gabarito/fase-00 -- java -cp /tmp/out Main
    # compara outra fase com o gabarito
    python3 validacao/executar_roteiros.py --comparar validacao/gabarito/fase-01 -- java -cp build/classes br.edu.unifor.missaomarte.Main

Se o comando precisar saber a pasta de trabalho (por exemplo, `./gradlew run`, que roda na pasta do projeto),
use o marcador {pasta}:  -- ./gradlew -p . run -q --console=plain -PpastaDoJogo={pasta}

Cada roteiro `NN-nome.in` é a entrada digitada. Se existir `NN-nome.ranking.json`, ele é copiado para o
diretório de trabalho antes da execução (ranking pré-existente). O jogo roda num diretório temporário
limpo, então o ranking.json local nunca é tocado.

A saída registrada é: stdout + código de saída + conteúdo final do ranking.json. As posições sorteadas
no mapa são mascaradas (o sorteio é aleatório na Fase 0); tudo o mais precisa ser idêntico.
"""
import argparse
import difflib
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROTEIROS = Path(__file__).resolve().parent / 'roteiros'

LINHA_MAPA = re.compile(r'^(\s*-?\d+\|).*$')
COORD_PASSAGEIRO = re.compile(r'^( - .+ \(\w+\) em )\(-?\d+,-?\d+\)$')


def mascarar(texto):
    """Esconde o que depende do sorteio do mapa; mantém dimensões, textos e contagens."""
    linhas = []
    for linha in texto.split('\n'):
        linha = LINHA_MAPA.sub(r'\1 <sorteado>', linha)
        linha = COORD_PASSAGEIRO.sub(r'\1(x,y)', linha)
        linhas.append(linha)
    return '\n'.join(linhas)


def executar(roteiro, comando):
    entrada = roteiro.read_bytes()
    with tempfile.TemporaryDirectory() as tmp:
        fixture = roteiro.with_suffix('.ranking.json')
        if fixture.exists():
            shutil.copy(fixture, Path(tmp) / 'ranking.json')
        cmd = [parte.replace('{pasta}', tmp) for parte in comando]
        r = subprocess.run(cmd, input=entrada, cwd=tmp, capture_output=True, timeout=120)
        ranking = Path(tmp) / 'ranking.json'
        final = ranking.read_text(encoding='utf-8') if ranking.exists() else '<ausente>'
    saida = r.stdout.decode('utf-8', errors='replace')
    erro = r.stderr.decode('utf-8', errors='replace')
    return (f'{mascarar(saida)}\n----- código de saída: {r.returncode}\n'
            f'----- stderr:\n{erro}\n----- ranking.json final:\n{final}\n')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    modo = p.add_mutually_exclusive_group(required=True)
    modo.add_argument('--gerar', type=Path, help='grava as saídas como gabarito neste diretório')
    modo.add_argument('--comparar', type=Path, help='compara as saídas com o gabarito deste diretório')
    p.add_argument('comando', nargs=argparse.REMAINDER, help='-- comando que inicia o jogo')
    a = p.parse_args()
    comando = a.comando[1:] if a.comando[:1] == ['--'] else a.comando
    if not comando:
        p.error('informe o comando do jogo depois de --')

    falhas = 0
    roteiros = sorted(ROTEIROS.glob('*.in'))
    for roteiro in roteiros:
        obtido = executar(roteiro, comando)
        nome = roteiro.stem + '.out'
        if a.gerar:
            a.gerar.mkdir(parents=True, exist_ok=True)
            (a.gerar / nome).write_text(obtido, encoding='utf-8')
            print(f'gravado  {nome}')
            continue
        esperado = (a.comparar / nome).read_text(encoding='utf-8')
        if obtido == esperado:
            print(f'IGUAL    {roteiro.stem}')
        else:
            falhas += 1
            print(f'DIFERENTE {roteiro.stem}')
            sys.stdout.writelines(difflib.unified_diff(
                esperado.splitlines(True), obtido.splitlines(True), 'gabarito', 'obtido', n=2))
    if a.comparar:
        print(f'\n{len(roteiros) - falhas}/{len(roteiros)} roteiros idênticos ao gabarito')
    return 1 if falhas else 0


if __name__ == '__main__':
    sys.exit(main())
