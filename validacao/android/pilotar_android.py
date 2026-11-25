#!/usr/bin/env python3
"""Joga o Missão Marte num Android (emulador ou celular) pelo adb, sem mãos, e salva prints de cada passo.

Os toques são dados em coordenadas do MUNDO do jogo (as mesmas do código das telas) e convertidos para pixels com a
mesma conta do ExtendViewport (mínimo 800x600, centro da câmera em 400,300). As caixas de texto do Android (nome,
senha, endereço) são preenchidas com "input text" e confirmadas tocando no OK, que o uiautomator localiza.

    python3 validacao/android/pilotar_android.py --dispositivo emulator-5554 --saida /tmp/prints --roteiro completo

Roteiros:
  completo   - entrada (retrato), endereço do servidor, cadastro, menu, nova partida (Fácil, mapa 3), a partida
               vencedora da semente 3 tocando no direcional, paisagem, segundo plano e volta, ranking global, histórico
  convidado  - entra como convidado, embarca um passageiro e aborta pelo botão (resultado só no ranking local)
  pilotagem  - (Fase 7) Modo Pilotagem no Fácil: segura MOTOR e a nave sobe e pousa de volta; sobe, gira e cai torta
               (explode); gira a tela e vai ao segundo plano no meio do voo; aborta e abre o ranking da pilotagem.
               O adb só segura um dedo por vez: girar e acelerar juntos fica para a partida jogada à mão.
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

PACOTE = 'br.edu.unifor.missaomarte'
ATIVIDADE = PACOTE + '/.android.AndroidLauncher'


class Android:
    def __init__(self, serial, saida):
        self.serial = serial
        self.saida = Path(saida)
        self.saida.mkdir(parents=True, exist_ok=True)
        self.numero = 0
        self.paisagem = False
        self.inicio = None   # hora do aparelho ao abrir o jogo: o log é lido a partir dela (sem apagar o logcat)

    def adb(self, *args, binario=False):
        r = subprocess.run(['adb', '-s', self.serial, *args], capture_output=True, timeout=60)
        return r.stdout if binario else r.stdout.decode(errors='replace')

    def tela(self):
        """
        Tamanho atual da tela em pixels, perguntado ao aparelho a cada toque. Não dá para presumir pela última
        rotação pedida: alguns celulares (visto num Samsung) voltam para retrato ao passar pela tela inicial.
        """
        atual = re.search(r'cur=(\d+)x(\d+)', self.adb('shell', 'dumpsys', 'window', 'displays'))
        if atual:
            return int(atual.group(1)), int(atual.group(2))
        w, h = map(int, re.search(r'(\d+)x(\d+)', self.adb('shell', 'wm', 'size').splitlines()[-1]).groups())
        w, h = min(w, h), max(w, h)
        return (h, w) if self.paisagem else (w, h)

    def pixel(self, x, y):
        """Mundo do jogo -> pixel da tela (mesma conta do ExtendViewport do TelaBase)."""
        w, h = self.tela()
        s = min(w / 800, h / 600)
        ww, wh = w / s, h / s
        esquerda, base = 400 - ww / 2, 300 - wh / 2
        return round((x - esquerda) * s), round((base + wh - y) * s)

    def tocar(self, x, y, espera=0.6):
        px, py = self.pixel(x, y)
        self.adb('shell', 'input', 'tap', str(px), str(py))
        time.sleep(espera)

    def segurar(self, x, y, segundos, espera=0.3):
        """Toque longo (um dedo parado): o jogo recebe o toque, e o soltar no fim."""
        px, py = self.pixel(x, y)
        self.adb('shell', 'input', 'swipe', str(px), str(py), str(px), str(py), str(int(segundos * 1000)))
        time.sleep(espera)

    def print(self, nome, espera=0.8):
        time.sleep(espera)
        self.numero += 1
        arquivo = self.saida / f'{self.numero:02d}-{nome}.png'
        arquivo.write_bytes(self.adb('exec-out', 'screencap', '-p', binario=True))
        print('print', arquivo.name, flush=True)

    def caixa_de_texto(self, texto):
        """Preenche a caixa de texto do sistema e toca no OK."""
        time.sleep(1.2)   # as caixas do jogo abrem vazias (o valor atual aparece como dica)
        if texto:
            self.adb('shell', 'input', 'text', texto.replace(' ', '%s'))
        time.sleep(0.5)
        # /data/local/tmp é a pasta do próprio adb: nada fica nos arquivos do dono do celular
        self.adb('shell', 'uiautomator', 'dump', '/data/local/tmp/ui.xml')
        xml = self.adb('shell', 'cat', '/data/local/tmp/ui.xml')
        self.adb('shell', 'rm', '-f', '/data/local/tmp/ui.xml')
        botao = re.search(r'text="(?:OK|Ok|ok)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
        if not botao:
            raise RuntimeError('botão OK da caixa de texto não encontrado')
        x1, y1, x2, y2 = map(int, botao.groups())
        self.adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(0.8)

    def girar(self, paisagem):
        self.adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        self.adb('shell', 'settings', 'put', 'system', 'user_rotation', '1' if paisagem else '0')
        self.paisagem = paisagem
        time.sleep(2)

    def abrir(self, semente=None):
        self.adb('shell', 'am', 'force-stop', PACOTE)
        self.adb('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed')
        self.inicio = self.adb('shell', 'date', "'+%m-%d %H:%M:%S.000'").strip()   # aspas: o adb shell junta os argumentos
        extras = ['--el', 'semente', str(semente)] if semente is not None else []
        self.adb('shell', 'am', 'start', '-n', ATIVIDADE, *extras)
        time.sleep(6)

    def log(self):
        return self.adb('logcat', '-d', '-T', self.inicio, '-s', 'Ciclo:I', 'AndroidRuntime:E')

    def log_do_app(self):
        """Tudo o que o processo do jogo escreveu no logcat (para procurar senha e token)."""
        pid = self.adb('shell', 'pidof', PACOTE).strip()
        return self.adb('logcat', '-d', '-T', self.inicio, '--pid', pid) if pid else ''


# ------------------------------------------------------------------ coordenadas das telas (as mesmas do código)

def opcao_entrada(i):          # TelaEntrada: Y_DAS_OPCOES = 325, PASSO = 42, linha() começa 8 acima do texto
    return 440, 325 - i * 42 - 13

def opcao_menu(i):             # TelaMenu: Y_DAS_OPCOES = 385, PASSO = 36
    return 470, 385 - i * 36 - 10

CAMPO_NOME, CAMPO_SENHA = (470, 288), (470, 238)
BOTAO_CONFIRMAR, BOTAO_VOLTAR = (310, 160), (490, 160)
NOVA_DIF_ANTERIOR, NOVA_MAPA_MENOR, NOVA_DECOLAR = (595, 334), (595, 264), (325, 96)
ABA_GLOBAL, VOLTAR_RANKING = (485, 449), (400, 68)


def botoes_da_partida(android):
    """Centro de cada botão de toque, com a mesma conta do LayoutDaPartida (lida do log não é possível: recalcula)."""
    w, h = android.tela()
    s = min(w / 800, h / 600)
    ww, wh = w / s, h / s
    vx, vy = 400 - ww / 2, 300 - wh / 2
    b = max(72, min(120, min(ww, wh) * 0.13))
    g, margem = 10, 20
    if wh > ww:   # retrato: direcional embaixo à esquerda, ações embaixo à direita
        x0, y0 = vx + margem, vy + margem
        largura = 2 * b + g
        xa = vx + ww - margem - largura
        ya = y0
    else:         # paisagem: controles na borda direita; direcional embaixo, ações em cima
        largura = 3 * b + 2 * g
        x0, y0 = vx + ww - margem - largura, vy + margem
        xa, ya = x0, y0 + 3 * b + 2 * g + 16
    passo = b + g
    centro = lambda x, y, lw=b, lh=b: (x + lw / 2, y + lh / 2)
    return {
        'w': centro(x0 + passo, y0 + 2 * passo), 's': centro(x0 + passo, y0),
        'a': centro(x0, y0 + passo), 'd': centro(x0 + 2 * passo, y0 + passo),
        'c': centro(xa, ya, largura), 'm': centro(xa, ya + b + g, largura),
        'q': centro(xa, ya + 2 * b + 3 * g, largura),
    }


NOVA_MODO = (635, 477)          # TelaNovaPartida: botão "Trocar" do modo (Fase 7)


def botoes_da_pilotagem(android):
    """Centro dos botões da pilotagem, com a mesma conta do LayoutDaPilotagem."""
    w, h = android.tela()
    s = min(w / 800, h / 600)
    ww, wh = w / s, h / s
    vx, vy = 400 - ww / 2, 300 - wh / 2
    b = max(72, min(140, min(ww, wh) * 0.16))
    m, g = 20, 14
    motor = b * 1.3
    topo_do_minimapa = vy + wh - m - 240 * (1440 / 3840)
    return {
        'esquerda': (vx + m + b / 2, vy + m + b / 2),
        'direita': (vx + m + b + g + b / 2, vy + m + b / 2),
        'motor': (vx + ww - m - motor / 2, vy + m + motor / 2),
        'abortar': (vx + ww - m - 75, topo_do_minimapa - g - 36),
    }


def pilotagem(a, _endereco, _nome):
    a.girar(False)
    a.abrir()
    a.tocar(*opcao_entrada(2))                          # 3. convidado
    a.tocar(*opcao_menu(0))                             # nova missão
    a.tocar(*NOVA_DIF_ANTERIOR)                         # Médio -> Fácil
    a.print('nova-partida-pilotagem')
    a.tocar(*NOVA_DECOLAR, espera=1.5)
    a.print('pilotagem-retrato-na-base')
    bt = botoes_da_pilotagem(a)
    a.segurar(*bt['motor'], 0.6, espera=0.1)            # sobe um pouco...
    a.print('pilotagem-subindo', espera=0.2)
    time.sleep(2.0)                                     # ...e pousa de volta na base, devagar
    a.print('pilotagem-pousou-de-volta')
    a.girar(True)
    a.print('pilotagem-paisagem')
    bt = botoes_da_pilotagem(a)
    a.segurar(*bt['motor'], 1.2, espera=0.0)            # sobe mais...
    a.segurar(*bt['esquerda'], 0.35, espera=0.0)        # ...gira no ar e cai torta: explode
    a.print('pilotagem-girando', espera=0.0)
    time.sleep(1.6)
    a.print('pilotagem-explosao', espera=0.0)
    time.sleep(2.5)                                     # nave nova na base
    a.segurar(*bt['motor'], 1.0, espera=0.0)            # no meio de um voo...
    a.adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')   # ...segundo plano
    time.sleep(2)
    a.adb('shell', 'am', 'start', '-n', ATIVIDADE)
    time.sleep(3)
    a.print('pilotagem-depois-do-segundo-plano')
    a.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')   # o Voltar não aborta o voo
    a.print('pilotagem-voltar-ignorado')
    bt = botoes_da_pilotagem(a)
    a.tocar(*bt['abortar'], espera=1.5)
    a.print('fim-pilotagem-abortada')
    a.tocar(400, 300, espera=1)
    a.girar(False)
    a.tocar(*opcao_menu(1), espera=1)                   # ranking
    a.tocar(*RANKING_MODO_PILOTAGEM, espera=1)
    a.print('ranking-pilotagem')


RANKING_MODO_PILOTAGEM = (485, 502)   # TelaRanking: botão do modo Pilotagem (Fase 7)


def jogar(android, teclas, espera=0.35):
    for t in teclas:
        android.tocar(*botoes_da_partida(android)[t], espera=espera)


def completo(a, endereco, nome):
    a.girar(False)
    a.abrir(semente=3)
    a.print('entrada-retrato')
    a.tocar(*opcao_entrada(3))                         # 4. Servidor -> caixa de texto do sistema
    a.print('caixa-endereco-do-servidor')
    a.caixa_de_texto(endereco)
    a.print('entrada-servidor-gravado')
    a.tocar(*opcao_entrada(1))                         # 2. Criar uma conta
    a.tocar(*CAMPO_NOME)
    a.caixa_de_texto(nome)
    a.tocar(*CAMPO_SENHA)
    a.caixa_de_texto('senha-de-teste-' + nome.lower())
    a.print('entrada-cadastro-preenchido')
    a.tocar(*BOTAO_CONFIRMAR, espera=2.5)
    a.print('menu-conectado')
    a.tocar(*opcao_menu(0))                            # nova partida
    a.tocar(*NOVA_MODO)                                # modo: clássico (a pilotagem é o padrão)
    a.tocar(*NOVA_DIF_ANTERIOR)                        # Médio -> Fácil
    a.tocar(*NOVA_MAPA_MENOR)
    a.tocar(*NOVA_MAPA_MENOR)                          # mapa 5 -> 3
    a.print('nova-partida-toque')
    a.tocar(*NOVA_DECOLAR, espera=1)
    a.print('partida-retrato')
    jogar(a, 'ddcss')
    a.print('partida-retrato-embarques')
    a.girar(True)
    a.print('partida-paisagem')
    jogar(a, 'cdwcaaaa')
    a.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')   # Voltar na partida: ignorado (não aborta)
    a.print('partida-paisagem-voltar-ignorado')
    a.adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')   # segundo plano
    time.sleep(2)
    a.adb('shell', 'am', 'start', '-n', ATIVIDADE)       # volta (singleTask: a mesma partida)
    time.sleep(3)
    a.print('partida-depois-de-voltar-do-segundo-plano')
    print('orientação ao voltar:', 'x'.join(map(str, a.tela())), flush=True)
    jogar(a, 'aawwwcdddss')
    a.print('fim-vitoria-enviada', espera=2.5)
    a.tocar(400, 300, espera=1)                         # tocar em qualquer lugar volta ao menu
    a.girar(False)
    a.tocar(*opcao_menu(1), espera=1)                   # ranking
    a.tocar(*ABA_GLOBAL, espera=2)
    a.print('ranking-global')
    a.tocar(*VOLTAR_RANKING)
    a.tocar(*opcao_menu(4), espera=2)                   # histórico
    a.print('historico')


def convidado(a, _endereco, _nome):
    a.girar(False)
    a.abrir(semente=3)
    a.tocar(*opcao_entrada(2))                          # 3. convidado
    a.print('menu-convidado')
    a.tocar(*opcao_menu(0))
    a.tocar(*NOVA_MODO)                                 # modo: clássico (a pilotagem é o padrão)
    a.tocar(*NOVA_DIF_ANTERIOR)
    a.tocar(*NOVA_MAPA_MENOR)
    a.tocar(*NOVA_MAPA_MENOR)                           # Fácil, mapa 3: o mesmo mapa da semente 3
    a.tocar(*NOVA_DECOLAR, espera=1)
    jogar(a, 'ddc')                                     # começo da rota vencedora: embarca o primeiro passageiro
    a.print('partida-convidado')
    jogar(a, 'q')
    a.print('fim-abortada-convidado', espera=1.5)
    a.tocar(400, 300, espera=1)                         # volta ao menu
    a.tocar(*opcao_menu(1), espera=1)                   # ranking
    a.print('ranking-local-convidado')
    a.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')   # Voltar do Android = Esc: volta ao menu
    a.print('menu-depois-do-voltar')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--dispositivo', required=True)
    p.add_argument('--saida', required=True)
    p.add_argument('--roteiro', choices=['completo', 'convidado', 'pilotagem'], default='completo')
    p.add_argument('--servidor', default='http://10.0.2.2:8080', help='endereço digitado na opção 4 da entrada')
    p.add_argument('--nome', default='Bia', help='piloto cadastrado no roteiro completo (precisa ser novo no servidor)')
    a = p.parse_args()
    android = Android(a.dispositivo, a.saida)
    {'completo': completo, 'convidado': convidado, 'pilotagem': pilotagem}[a.roteiro](android, a.servidor, a.nome)
    (Path(a.saida) / 'logcat.txt').write_text(android.log())
    (Path(a.saida) / 'logcat-do-app.txt').write_text(android.log_do_app())
    print('fim do roteiro')


if __name__ == '__main__':
    sys.exit(main())
