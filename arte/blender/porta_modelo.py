"""Portas de resgate com duas folhas deslizantes e interior iluminado."""
import importlib
import bpy
from common import cube, material


def acrescentar_porta(add, x, frente, z, largura, altura, acabamento, estrutura, luz):
    add('cube', 'Moldura da porta', estrutura, (x,frente,z),
        (largura+2.4,1.4,altura+2.4), bevel=.7)
    add('cube', 'Interior da porta', estrutura, (x,frente-.8,z),
        (largura, .4, altura), bevel=.35)
    for sinal, nome in ((-1,'esquerda'),(1,'direita')):
        add('cube', 'Porta '+nome, acabamento,
            (x+sinal*largura/4,frente-1.3,z), (largura/2,.65,altura), bevel=.35)
        add('cube', 'Puxador '+nome, estrutura,
            (x+sinal*1.5,frente-1.8,z), (1,.5,4), bevel=.2)
    add('cube','Luz sobre a porta',luz,(x,frente-1.5,z+altura/2+.6),
        (largura*.55,.5,.9),bevel=.1)


def construir(skin):
    modulo = importlib.import_module('nave' if skin=='padrao' else 'naves.'+skin+'_modelo')
    voo = modulo.build()
    voo(0)
    folhas = [bpy.data.objects['Porta esquerda'],bpy.data.objects['Porta direita']]
    puxadores = [bpy.data.objects['Puxador esquerda'],bpy.data.objects['Puxador direita']]
    origens = [(o,o.location.copy()) for o in folhas+puxadores]
    # Os modificadores não aumentam o deslocamento; metade da abertura recebe cada folha.
    guara = skin.startswith('guara')
    x_porta = 21 if guara else 0
    z_porta = -9.5 if guara else -13.5
    material_rampa = material('Degraus de resgate',(.28,.30,.31,1),.65,.35)
    degraus = [cube('Degrau retrátil', (x_porta,-23,z_porta), (10,6,2),
                    material_rampa, folhas[0].parent,.3) for _ in range(4)]
    largura = 10 if guara else 14
    deslocamento = largura/2
    def animar(i):
        fracao = i/11
        abertura = fracao*fracao*(3-2*fracao)
        for indice,(objeto,origem) in enumerate(origens):
            objeto.location = origem
            objeto.location.x += (-1 if indice%2==0 else 1)*deslocamento*abertura
        extensao = max(0, (abertura-.35)/.65)
        for indice, degrau in enumerate(degraus):
            degrau.hide_render = extensao == 0
            degrau.location.x = x_porta + (indice+1)*6*extensao
            degrau.location.z = z_porta + (-38-z_porta)*(indice+1)/4*extensao
    return animar
