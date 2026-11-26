"""Ambulância com cápsulas ovais e uma escotilha de triagem."""
from nave_modelo import _rings, parts_radius_bound
from ..comum import chassis, animacao


def partes():
    parts, add = chassis()
    add('mesh', 'Quilha violeta', 'ameixa', *_rings((
        (-18, 9, 8), (-8, 15, 12), (7, 17, 13), (20, 12, 10),
        (31, 6, 6), (43, 1, 1)), segments=16), bevel=.6)
    add('sphere', 'Domo de observação', 'estrutura', (0,-8,20), (10,7,14))
    add('sphere', 'Bolha de vidro', 'vidro', (0,-10,21), (8.6,6.4,11.7))
    for lado in (-1, 1):
        x = lado*20
        add('sphere', 'Cápsula de evacuação', 'perola', (x,0,1), (9,9,21))
        add('sphere', 'Tampa da maca', 'ameixa', (x,-5.8,1), (6.5,4.3,15))
        add('rod', 'Friso de latão', 'latao', (x,-10, -5), (x,-10,7), 1.2)
        add('rod', 'Arco da suspensão', 'latao', (lado*12,0,21), (x,0,10), 2)
    add('sphere', 'Escotilha de triagem', 'latao', (0,-12,-6), (7,3,7))
    add('sphere', 'Interior da escotilha', 'estrutura', (0,-14,-6), (5.4,2.4,5.4))
    add('cube', 'Sinal de triagem vertical', 'perola', (0,-16,-6), (2,1,7))
    add('cube', 'Sinal de triagem horizontal', 'perola', (0,-16,-6), (7,1,2))
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Bruma', partes(), {
        'ameixa': ((.32,.037,.40,1), .38, .29),
        'perola': ((.88,.72,.84,1), .25, .28),
        'latao': ((.90,.49,.12,1), .62, .28),
    })
