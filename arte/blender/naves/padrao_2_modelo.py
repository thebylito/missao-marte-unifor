"""Padrão II: aletas instrumentadas, antenas e reforços de pouso."""
from nave_modelo import _parts, parts_radius_bound, ship_materials
from naves.comum import acrescentar, animacao, painel

PALETA = {}


def partes():
    parts = _parts()
    add = acrescentar(parts)
    for lado in (-1, 1):
        painel(add, 'Aleta de serviço', 'graphite', lado,
               [(17, 8), (23, 12), (31, 5), (32, -6), (26, -12), (17, -8)],
               -10.5, -5)
        add('rod', 'Antena de telemetria', 'metal',
            (lado*16, -1, 8), (lado*15, -1, 24), 1.5)
        add('sphere', 'Ponta da antena', 'ceramic',
            (lado*15, -1, 24), (2, 2, 2))
        add('rod', 'Reforço de pouso', 'graphite',
            (lado*16, -4, -10), (lado*24, -4, -29), 2.8)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Padrão II', partes(), PALETA, ship_materials)
