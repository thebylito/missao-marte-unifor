#!/usr/bin/env python3
"""Consulta o hangar, compra a skin se necessário e a equipa."""
import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from http_fase8 import entrar, requisitar


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--servidor', default='http://localhost:8080')
    p.add_argument('--nome', required=True)
    p.add_argument('--skin', required=True)
    a = p.parse_args()
    token = entrar(a.servidor, a.nome, 'MISSAOMARTE_SENHA_PILOTO')

    def consultar(metodo, caminho, corpo=None):
        status, resposta = requisitar(a.servidor, metodo, caminho, corpo, token)
        if not 200 <= status < 300:
            sys.exit(f'{metodo} {caminho}: HTTP {status} {resposta}')
        return json.loads(resposta)

    def mostrar_hangar():
        hangar = consultar('GET', '/api/jogadores/eu/hangar')
        print(f'Saldo: {hangar["saldo"]} | Skins: {", ".join(hangar["skins"])} | Equipada: {hangar["equipada"]}')
        return hangar

    hangar = mostrar_hangar()
    pedido = {'skin': a.skin}
    if a.skin not in hangar['skins']:
        loja = consultar('GET', '/api/loja')
        item = next((i for i in loja if i['id'] == a.skin), None)
        if item is None:
            sys.exit(f'Skin fora do catálogo: {a.skin}')
        if hangar['saldo'] < item['preco']:
            sys.exit(f'Saldo insuficiente para {a.skin} (preço {item["preco"]}).')
        consultar('POST', '/api/loja/compras', pedido)
    if hangar['equipada'] != a.skin:
        consultar('PUT', '/api/jogadores/eu/skin', pedido)
    if mostrar_hangar()['equipada'] != a.skin:
        sys.exit(f'O perfil não confirmou a skin {a.skin}.')


if __name__ == '__main__':
    main()
