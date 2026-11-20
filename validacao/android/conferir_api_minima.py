#!/usr/bin/env python3
"""Confere se o código do jogo no APK só usa APIs do Java/Android que existem no minSdk (Fase 6).

O lint do Android não analisa o código dos módulos Java puros (core e nucleo): para ele são dependências externas.
Este script olha o resultado final, o bytecode dex dentro do APK, depois que o D8 já reescreveu o que sabe
reescrever (os "backports", como List.of e String.isBlank). Cada referência que o código do jogo faz a um método,
campo ou classe da plataforma é procurada no api-versions.xml do SDK; se ela só existe numa API maior que o minSdk,
o jogo quebraria (NoSuchMethodError ou NoClassDefFoundError) num aparelho antigo.

Chamadas herdadas também contam: o javac grava "AndroidLauncher.getOnBackInvokedDispatcher", e não
"Activity.getOnBackInvokedDispatcher". Por isso a hierarquia de cada classe (superclasse e interfaces, lidas do
próprio dex) é percorrida até chegar a uma classe da plataforma.

Só conta o código do jogo (classes em br/edu/unifor/). As bibliotecas (libGDX, AndroidX) chamam APIs novas de
propósito, depois de conferir Build.VERSION.SDK_INT, e uma análise do bytecode não enxerga essa conferência.

    python3 validacao/android/conferir_api_minima.py android/build/outputs/apk/debug/android-debug.apk \
        --sdk "$ANDROID_HOME" --min 26
"""
import argparse
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

DONO = re.compile(r"^\s*Class descriptor\s+:\s+'L([\w/$]+);'")
SUPER = re.compile(r"^\s*Superclass\s+:\s+'L([\w/$]+);'")
INTERFACE = re.compile(r"^\s*#\d+\s+:\s+'L([\w/$]+);'")
NOME = re.compile(r"^\s*name\s+:\s+'([\w$<>]+)'")
TIPO = re.compile(r"^\s*type\s+:\s+'(\S+)'")
METODO = re.compile(r'L([\w/$]+);\.([\w$<>]+):(\([^)]*\)\S+)')
CAMPO = re.compile(r'(?:[is]get|[is]put)[\w-]*\s[^L;]*?, L([\w/$]+);\.([\w$]+):')
CLASSE = re.compile(r'(?:new-instance|const-class|check-cast|instance-of|new-array|filled-new-array)[^,]*, \[*L([\w/$]+);')


def ler_api(xml):
    """{classe: (since, {metodo_ou_campo: since}, [superclasses e interfaces])}"""
    api = {}
    for c in ET.parse(xml).getroot().iter('class'):
        desde = int(c.get('since', '1'))
        membros = {}
        for m in c:
            if m.tag in ('method', 'field'):
                membros[m.get('name')] = int(m.get('since', desde))
        pais = [p.get('name') for p in c if p.tag in ('extends', 'implements')]
        api[c.get('name')] = (desde, membros, pais)
    return api


class Bytecode:
    """O que o dexdump mostra: as classes do APK, os pais de cada uma, o que cada uma define e as referências."""

    def __init__(self):
        self.pais = {}          # classe -> [superclasse, interfaces...]
        self.definidos = {}     # classe -> {nome+tipo de método, nome de campo}
        self.referencias = {}   # classe que referencia -> {(tipo, dono, membro)}

    def ler(self, texto):
        dono = None
        em_interfaces = False
        nome = None
        for linha in texto.splitlines():
            d = DONO.match(linha)
            if d:
                dono = d.group(1)
                self.pais[dono] = []
                self.definidos[dono] = set()
                self.referencias[dono] = set()
                em_interfaces = False
                continue
            if dono is None:
                continue
            s = SUPER.match(linha)
            if s:
                self.pais[dono].append(s.group(1))
                continue
            if linha.strip().startswith('Interfaces'):
                em_interfaces = True
                continue
            if em_interfaces:
                i = INTERFACE.match(linha)
                if i:
                    self.pais[dono].append(i.group(1))
                    continue
                em_interfaces = False
            n = NOME.match(linha)
            if n:
                nome = n.group(1)
                continue
            t = TIPO.match(linha)
            if t and nome:
                tipo = t.group(1)
                self.definidos[dono].add(nome + tipo if tipo.startswith('(') else nome)
                nome = None
                continue
            for m in METODO.finditer(linha):
                self.referencias[dono].add(('método', m.group(1), m.group(2) + m.group(3)))
            for m in CAMPO.finditer(linha):
                self.referencias[dono].add(('campo', m.group(1), m.group(2)))
            for m in CLASSE.finditer(linha):
                self.referencias[dono].add(('classe', m.group(1), None))


def nivel_na_plataforma(api, classe, membro, vistos=None):
    """API em que o membro existe nessa classe da plataforma ou nos pais dela (None se não achar)."""
    vistos = vistos if vistos is not None else set()
    if classe in vistos or classe not in api:
        return None
    vistos.add(classe)
    desde, membros, pais = api[classe]
    if membro in membros:
        return membros[membro]
    achados = [n for p in pais if (n := nivel_na_plataforma(api, p, membro, vistos)) is not None]
    return min(achados) if achados else None


def resolver(api, codigo, classe, membro, vistos=None):
    """
    Segue a hierarquia a partir do dono gravado no bytecode. Devolve ('app', None) se o membro é definido no código
    do APK, ('plataforma', nível) se vem da plataforma, ou ('?', None) se não deu para descobrir.
    """
    vistos = vistos if vistos is not None else set()
    if classe in vistos:
        return ('?', None)
    vistos.add(classe)
    if classe in api:
        n = nivel_na_plataforma(api, classe, membro)
        return ('plataforma', n) if n is not None else ('?', None)
    if classe in codigo.pais:
        if membro in codigo.definidos[classe]:
            return ('app', None)
        resultados = [resolver(api, codigo, p, membro, vistos) for p in codigo.pais[classe]]
        niveis = [n for o, n in resultados if o == 'plataforma']
        if niveis:
            return ('plataforma', min(niveis))
        if any(o == 'app' for o, _ in resultados):
            return ('app', None)
    return ('?', None)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('apk')
    p.add_argument('--sdk', required=True, help='pasta do Android SDK (ANDROID_HOME)')
    p.add_argument('--min', type=int, required=True, help='minSdk do app')
    p.add_argument('--plataforma', default='android-36')
    p.add_argument('--pacote', default='br/edu/unifor/', help='prefixo das classes do jogo a conferir')
    a = p.parse_args()
    sdk = Path(a.sdk)
    api = ler_api(sdk / 'platforms' / a.plataforma / 'data' / 'api-versions.xml')
    dexdump = sorted((sdk / 'build-tools').glob('*/dexdump'))[-1]

    codigo = Bytecode()
    with tempfile.TemporaryDirectory() as pasta, zipfile.ZipFile(a.apk) as z:
        dexes = [n for n in z.namelist() if re.fullmatch(r'classes\d*\.dex', n)]
        for n in dexes:
            z.extract(n, pasta)
            saida = subprocess.run([str(dexdump), '-d', str(Path(pasta) / n)], capture_output=True)
            codigo.ler(saida.stdout.decode('utf-8', errors='replace'))

    do_jogo = sorted(c for c in codigo.pais if c.startswith(a.pacote))
    referencias = set()
    for c in do_jogo:
        referencias |= codigo.referencias[c]

    problemas, desconhecidas, conferidas = [], [], 0
    for tipo, dono, membro in sorted(referencias, key=lambda r: (r[1], r[2] or '')):
        if tipo == 'classe':
            if dono in api:
                conferidas += 1
                if api[dono][0] > a.min:
                    problemas.append(f'API {api[dono][0]}: classe {dono.replace("/", ".")}')
            continue
        origem, nivel = resolver(api, codigo, dono, membro)
        if origem == 'app':
            continue
        if origem == '?':
            desconhecidas.append(f'{tipo} {dono}.{membro}')
            continue
        conferidas += 1
        if nivel > a.min:
            problemas.append(f'API {nivel}: {tipo} {dono.replace("/", ".")}.{membro}')

    print(f'APK: {Path(a.apk).name} ({len(dexes)} dex); minSdk {a.min}; base: {a.plataforma}/data/api-versions.xml')
    print(f'Classes do jogo ({a.pacote}*) no APK: {len(do_jogo)}')
    print(f'Referências delas à plataforma (Java e Android), conferidas no bytecode final: {conferidas}')
    print(f'Acima do minSdk {a.min}: {len(problemas)}')
    for linha in problemas:
        print('  ' + linha)
    print(f'Não resolvidas (nem no APK nem no api-versions.xml): {len(desconhecidas)}')
    for linha in desconhecidas[:30]:
        print('  ' + linha)
    return 1 if problemas else 0


if __name__ == '__main__':
    sys.exit(main())
