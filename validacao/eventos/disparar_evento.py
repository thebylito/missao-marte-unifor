#!/usr/bin/env python3
"""Dispara um evento com uma conta administrativa já cadastrada."""
import argparse
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from http_fase8 import entrar, requisitar


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--servidor', default='http://localhost:8080')
    p.add_argument('--nome', required=True)
    p.add_argument('--tipo', required=True, choices=('CHUVA_DE_METEOROS', 'TEMPESTADE',
                                                     'PANE_DE_GRAVIDADE', 'PASSAGEIRO_VIP'))
    a = p.parse_args()
    token = entrar(a.servidor, a.nome, 'MISSAOMARTE_SENHA_ADMIN')
    status, corpo = requisitar(a.servidor, 'POST', '/api/admin/eventos', {'tipo': a.tipo}, token)
    print(f'HTTP {status}\n{corpo}')
    return 0 if 200 <= status < 300 else 1


if __name__ == '__main__':
    sys.exit(main())
