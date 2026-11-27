"""Instalação original de transmissão da M02, distinta do posto habitável.

Célula 256×192; apoio em (128, 178). Quadros 0–3: desligada, 4–7:
orientação/ativação, 8–11: transmissão ativa. Nenhuma peça move a base.
"""
import math
from common import root, cube, sphere, cone, rod, mesh, material, emissive

PIVOT = (128, 178)
STATES = {'desligada': (0, 3), 'ativando': (4, 7), 'ativa': (8, 11)}


def build():
    base = root('Antena da missão — base fixa')
    dark = material('Antena — estrutura grafite', (.055, .085, .11, 1), .65, .4)
    metal = material('Antena — aço azulado', (.24, .34, .4, 1), .7, .3)
    shell = material('Antena — cerâmica clara', (.7, .76, .71, 1), .35, .4)
    blue = material('Antena — setores azuis', (.045, .29, .39, 1), .45, .35)
    orange = material('Antena — alça de manutenção', (.83, .32, .045, 1), .3)
    lamp = emissive('Antena — estado de transmissão', (.18, .23, .16, 1), .35)
    focus = emissive('Antena — foco transmissor', (.16, .22, .18, 1), .35)

    cube('Plataforma de ancoragem', (0, 0, -76), (178, 43, 12), metal, base, 3)
    cube('Saia escura', (0, -22, -78), (180, 3, 6), dark, base, 1)
    for side in (-1, 1):
        cube('Pé de concreto cerâmico', (side * 70, 0, -78), (29, 46, 8), shell, base, 2)
        rod('Estabilizador lateral', (side * 73, 4, -69), (side * 16, 4, -19), 4, metal, base)
    cube('Caixa de energia', (-58, -6, -55), (38, 29, 32), dark, base, 3)
    cube('Porta do módulo', (-58, -22, -55), (29, 3, 23), shell, base, 1)
    for x in (-66, -58, -50):
        cube('Luz de estado do módulo', (x, -24, -50), (4, 2, 8), lamp, base, .5)
    cube('Banco de baterias', (58, 0, -59), (37, 28, 23), metal, base, 2)
    for x in (47, 55, 63, 71):
        cube('Aleta de bateria', (x, -16, -59), (3, 5, 20), dark, base, .4)
    cone('Coluna central', (0, 0, -33), 17, 12, 76, metal, base, 12)
    cone('Mancal superior', (0, 0, 7), 22, 22, 8, dark, base, 16)
    cube('Indicador vertical', (0, -18, -37), (8, 3, 32), lamp, base, 1)
    rod('Cabo de potência', (16, -5, -63), (46, -5, -60), 3, orange, base)

    dish = root('Refletor orientável')
    dish.parent = base
    dish.location = (0, 0, 24)
    radius = 45
    sectors = 16
    # Shallow concave reflector: each face and rim are original geometry.
    for index in range(sectors):
        a, b = math.tau * index / sectors, math.tau * (index + 1) / sectors
        verts = [(0, 0, 0)]
        for r, theta in ((radius * .5, a), (radius, a), (radius, b), (radius * .5, b)):
            verts.append((r * math.cos(theta), -r * r / 180, r * math.sin(theta)))
        mesh('Setor do refletor', verts, [(0, 1, 4), (1, 2, 3, 4)], blue if index % 4 == 0 else shell, dish)
        rod('Aro segmentado', verts[2], verts[3], 1.7, metal, dish)
        if index % 4 == 0:
            rod('Nervura do refletor', (0, -.5, 0), verts[2], .8, metal, dish)
    sphere('Centro do refletor', (0, -2, 0), (9, 4, 9), metal, dish)
    for x, z in ((-27, -27), (27, -27), (0, 36)):
        rod('Tripé do foco', (x, -10, z), (0, -37, 0), 1.7, orange, dish)
    sphere('Foco de transmissão', (0, -39, 0), (6, 7, 6), dark, dish)
    sphere('Olho do transmissor', (0, -45, 0), (3.7, 2, 3.7), focus, dish)

    rotor = root('Dipolo transmissor')
    rotor.parent = dish
    rotor.location = (0, -39, 0)
    rod('Eixo do dipolo', (-11, 0, 0), (11, 0, 0), 1.1, metal, rotor)
    for side in (-1, 1):
        sphere('Terminal do dipolo', (side * 11, 0, 0), (1.7, 1.7, 1.7), focus, rotor, 1)

    def animate(frame):
        if frame < 4:
            # Only a tiny idle tremor; low-power lamps remain visibly dim.
            dish.rotation_euler = (-.60 + .002 * frame, 0, -.34)
            rotor.rotation_euler[1] = 0
            color = (.16, .23 + frame * .008, .17, 1)
            strength = .26 + .025 * frame
        elif frame < 8:
            progress = (frame - 3) / 4
            eased = progress * progress * (3 - 2 * progress)
            dish.rotation_euler = (-.60 + .71 * eased, 0, -.34 + .42 * eased)
            rotor.rotation_euler[1] = progress * math.pi / 2
            color = (1, .42 + .06 * progress, .035, 1)
            strength = .9 + .65 * progress
        else:
            phase = math.tau * (frame - 8) / 4
            dish.rotation_euler = (.11 + .035 * math.sin(phase), 0, .08 + .07 * math.cos(phase))
            rotor.rotation_euler[1] = math.pi / 2 + (frame - 8) * math.pi / 4
            color = (.08, .92, .32, 1)
            strength = 1.55 + .2 * math.sin(phase + .3)
        for mat in (lamp, focus):
            emission = mat.node_tree.nodes.get('Emission')
            emission.inputs['Color'].default_value = color
            emission.inputs['Strength'].default_value = strength

    return animate
