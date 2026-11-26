"""Enche os abrigos de emergência com ar levado em dois tanques de pressão."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, cabine, casco, chassis


def partes():
    parts, add = chassis()
    casco(add, 'Cabine pressurizada', 'ardosia', [(-17,10,8), (-7,13,10),
          (14,11,9), (27,7,6), (35,3,3)], 10)
    cabine(add, 19, 7, 10)
    for s in (-1, 1):
        x = s*21
        add('rod', 'Ponte do coletor', 'metal', (s*8,0,5), (x,0,5), 4)
        add('rod', 'Garrafa de pressão', 'ardosia', (x,0,-12), (x,0,26), 6)
        for z in (-12,26):
            add('sphere', 'Calota de pressão', 'bronze', (x,0,z), (6,6,6))
        for z in (-4,13):
            add('rod', 'Abraçadeira', 'bronze', (x,0,z), (x,0,z+3), 6.8)
        add('rod', 'Linha de ar', 'bronze', (x,-5,26), (s*7,-5,30), 1.4)
    for z in range(-12, 5, 4):
        add('cube', 'Prega do fole', 'bronze', (0,-9,z), (16,4,2), bevel=.6)
    add('rod', 'Respiro', 'metal', (0,0,31), (0,0,40), 2)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Fole', partes(), {
        'ardosia': ((.038,.071,.24,1), .38, .33),
        'bronze': ((.67,.37,.19,1), .65, .26),
    })
