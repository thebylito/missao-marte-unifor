"""Guia comboios na noite marciana com arcos de cobre e balizas de luz âmbar."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, cabine, casco, chassis, painel


def partes():
    parts, add = chassis()
    casco(add, 'Espinha de obsidiana', 'obsidiana', [(-19,10,9), (-7,16,13),
          (9,13,11), (22,11,8), (40,4,4), (47,0.8,1)], 8)
    cabine(add, 23, 7, 12)
    for s in (-1, 1):
        arco = [(9,-16), (24,-10), (31,4), (28,18), (15,34),
                (20,16), (22,5), (17,-3), (10,-6)]
        painel(add, 'Arco de balizamento', 'cobre', s, arco, -3, 5)
        trilha = [(12,-12), (22,-7), (27,5), (25,17), (18,27)]
        for a,b in zip(trilha, trilha[1:]):
            add('rod', 'Guia de luz', 'ambar', (s*a[0],-4,a[1]), (s*b[0],-4,b[1]), 1.1)
        add('rod', 'Ponte do arco', 'cobre', (s*9,1,19), (s*24,1,13), 2)
        add('sphere', 'Luz de posição', 'ambar', (s*30,-3,5), (2,2,3))
    add('sphere', 'Aro da lanterna', 'cobre', (0,-12,-4), (8,4,9))
    add('sphere', 'Lanterna de comboio', 'ambar', (0,-15,-4), (5,3,6))
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Luzeiro', partes(), {
        'obsidiana': ((.085,.039,.10,1), .72, .22),
        'cobre': ((.83,.37,.13,1), .79, .23),
        'ambar': ((1,.36,.033,1), .18, .22, 2.1),
    })
