"""Leva água de degelo aos abrigos isolados no inverno marciano."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, casco, chassis


def partes():
    parts, add = chassis()
    casco(add, 'Cisterna bojuda', 'barro', [(-19,12,10), (-13,23,14), (0,28,17),
          (14,23,15), (23,12,9), (28,9,7)], 24)
    add('cube', 'Torre de comando', 'creme', (0,0,28), (18,16,23), bevel=4)
    add('cube', 'Visor da torre', 'vidro', (0,-8,29), (14,4,9), bevel=2)
    for z, largura in ((-8,25), (9,26)):
        casco(add, 'Cinta da cisterna', 'creme', [(z-1.4,largura,16.9),
              (z+1.4,largura,17.2)], 24)
    add('rod', 'Registro frontal', 'estrutura', (0,-15,0), (0,-20,0), 5)
    add('cube', 'Manopla do registro', 'creme', (0,-21,0), (13,2,2), bevel=.5)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Cantil', partes(), {
        'barro': ((.49,.061,.026,1), .11, .65),
        'creme': ((.91,.70,.44,1), .12, .42),
    })
