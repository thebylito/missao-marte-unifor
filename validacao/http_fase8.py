"""HTTP JSON usado pelas ferramentas de conta da Fase 8."""
import json
import os
import sys
import urllib.error
import urllib.request


def requisitar(servidor, metodo, caminho, corpo=None, token=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    pedido = urllib.request.Request(servidor.rstrip('/') + caminho, dados, headers, method=metodo)
    try:
        resposta = urllib.request.urlopen(pedido, timeout=5)
    except urllib.error.HTTPError as erro:
        resposta = erro
    except OSError as erro:
        sys.exit(f'{metodo} {caminho}: {erro}')
    with resposta:
        return resposta.status, resposta.read().decode(errors='replace')


def entrar(servidor, nome, variavel):
    senha = os.environ.get(variavel)
    if not senha:
        sys.exit(f'Defina {variavel} para entrar com a conta {nome}.')
    status, corpo = requisitar(servidor, 'POST', '/api/sessoes', {'nome': nome, 'senha': senha})
    if status != 200:
        sys.exit(f'Login de {nome} falhou (HTTP {status}).')
    return json.loads(corpo)['token']
