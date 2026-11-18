"""Sentinela marciana hostil (Fase 4): orbe blindado, olho vermelho pulsante e oito espinhos girando no plano da tela.

Oito quadros em laço: os espinhos giram 45 graus no total (a simetria de 8 fecha o laço sem salto) e o olho pulsa.
Modelo inédito, feito só com primitivas, como os demais assets.
"""
import math
from common import sphere, cone, material, emissive, root, METAL, DARK

FRAMES = 8
RAIO_ESPINHOS = 27


def build():
    r = root('Hostile sentinel')
    casco = material('Gunmetal shell', METAL, .85, .32)
    faixa = material('Dark armor band', DARK, .6, .45)
    espinho = material('Oxidized spikes', (.36, .09, .05, 1), .7, .4)
    olho = emissive('Hostile eye', (1, .07, .02, 1), 3)
    brilho = olho.node_tree.nodes.get('Emission')

    sphere('Armored orb', (0, 0, 0), (21, 21, 21), casco, r, 3)
    sphere('Equator band', (0, 0, 0), (23, 23, 6), faixa, r, 3)
    sphere('Red eye', (0, -17, 5), (8, 6, 8), olho, r, 2)
    sphere('Eye socket', (0, -14, 5), (11, 6, 11), faixa, r, 2)

    anel = root('Spike ring')
    anel.parent = r
    for k in range(8):
        a = 2 * math.pi * k / 8
        c = cone(f'Spike {k}', (math.cos(a) * RAIO_ESPINHOS, 2, math.sin(a) * RAIO_ESPINHOS),
                 4.5, 0, 14, espinho, anel, vertices=10)
        # o cone nasce apontando para +Z; girar em Y por (90° - a) o aponta para fora, na direção a do plano XZ
        c.rotation_euler = (0, math.pi / 2 - a, 0)

    def animate(i):
        anel.rotation_euler[1] = -(math.pi / 4) * i / FRAMES
        brilho.inputs['Strength'].default_value = 2.2 + 2.6 * (.5 + .5 * math.cos(2 * math.pi * i / FRAMES))

    return animate
