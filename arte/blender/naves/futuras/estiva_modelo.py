"""Entrega caixas de mantimentos às equipes de busca nas encostas de Marte."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, cabine, casco, chassis


def partes():
    parts, add = chassis()
    casco(add, 'Coluna de carga', 'grafite', [(-19,11,9), (-12,15,11),
          (16,13,11), (27,9,8), (42,2,2)], 8)
    cabine(add, 23, 8, 9)
    for s in (-1, 1):
        for z, x, largura in ((-9,20,16), (5,19,15), (19,15,11)):
            add('cube', 'Contentor empilhável', 'enxofre', (s*x,0,z),
                (largura,15,13), bevel=1.7)
            add('cube', 'Cinta do contentor', 'grafite', (s*x,-.3,z),
                (3,15.8,13.2), bevel=.6)
    add('cube', 'Travessa do elevador', 'metal', (0,-11,-10), (28,4,5), bevel=.8)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Estiva', partes(), {
        'enxofre': ((.89,.57,.014,1), .09, .5),
        'grafite': ((.14,.13,.12,1), .55, .29),
    })
