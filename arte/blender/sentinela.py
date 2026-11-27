"""Torre de varredura original: pedestal imóvel e cabeça com lente articulada.

Célula 128×128; apoio em (64, 118). Os 16 quadros formam um ciclo de
varredura, e nunca representam deslocamento físico do pedestal.
"""
import math
from common import root, cube, sphere, cone, rod, material, emissive

PIVOT = (64, 118)


def build():
    base = root('Sentinela — pedestal fixo')
    graphite = material('Sentinela — grafite violeta', (.075, .065, .12, 1), .5, .4)
    armor = material('Sentinela — placas de titânio', (.44, .49, .54, 1), .65, .32)
    ivory = material('Sentinela — identificação marfim', (.77, .78, .64, 1), .25)
    orange = material('Sentinela — faixa de atenção', (.72, .23, .025, 1), .35)
    lens = emissive('Sentinela — lente âmbar', (1, .34, .025, 1), 1.25)
    slit = emissive('Sentinela — marca da varredura', (1, .61, .09, 1), 1.05)

    # Feet and chassis stay at identical coordinates for the whole loop.
    cube('Placa de ancoragem', (0, 0, -49), (74, 34, 10), graphite, base, 2)
    for side in (-1, 1):
        cube('Sapata aparafusada', (side * 28, -2, -51), (18, 31, 6), armor, base, 1)
        rod('Escora diagonal', (side * 29, 2, -46), (side * 12, 2, -17), 4, armor, base)
        sphere('Parafuso de ancoragem', (side * 28, -18, -49), (2, 1, 2), ivory, base, 1)
    cone('Coluna octogonal', (0, 0, -19), 17, 12, 57, graphite, base, 8)
    cube('Blindagem frontal', (0, -15, -21), (25, 5, 33), armor, base, 2)
    cube('Faixa de atenção', (0, -18, -21), (24, 1.5, 6), orange, base, .3)
    for x in (-6, 0, 6):
        cube('Grade de ventilação', (x, -18.2, -32), (2, 1, 9), graphite, base, .2)
    cone('Anel de rotação', (0, 0, 10), 22, 22, 8, armor, base, 16)
    cone('Separador do anel', (0, 0, 15), 17, 17, 3, graphite, base, 16)

    head = root('Cabeça — varredura sem mover a base')
    head.parent = base
    head.location = (0, 0, 31)
    cube('Cabeça blindada', (0, 0, 0), (57, 30, 27), armor, head, 5)
    cube('Viseira escura', (0, -16, 0), (44, 5, 18), graphite, head, 3)
    cube('Sobrancelha de proteção', (0, -19, 12), (54, 12, 5), orange, head, 1)
    sphere('Aro da lente', (0, -20, 0), (11, 4, 11), ivory, head)
    sphere('Lente óptica', (0, -23, 0), (8, 3.5, 8), lens, head)
    sphere('Reflexo da lente', (-2.5, -26.3, 3), (2.3, .8, 2.3), slit, head)
    for side in (-1, 1):
        cube('Sensor secundário', (side * 19, -19, 0), (3, 2, 10), slit, head, .6)
        cone('Eixo lateral', (side * 29, 0, 0), 6, 6, 5, graphite, head, 12).rotation_euler[1] = math.pi / 2

    def animate(frame):
        phase = math.tau * frame / 16
        head.rotation_euler[2] = .92 * math.sin(phase)
        head.rotation_euler[0] = .045 * math.cos(phase)
        lens.node_tree.nodes.get('Emission').inputs['Strength'].default_value = 1.25 + .18 * math.cos(phase)
        slit.node_tree.nodes.get('Emission').inputs['Strength'].default_value = 1.05 + .3 * math.sin(phase + .4)

    return animate
