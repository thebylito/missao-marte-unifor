"""Padrão III: cerâmica clara com placas de titânio azul e filetes."""
from nave_modelo import parts_radius_bound, ship_materials
from naves.comum import acrescentar, animacao, painel
from naves.padrao_2_modelo import PALETA as ANTERIOR, partes as partes_anteriores

PALETA = {
    **ANTERIOR,
    'titanio_azul': ('Titânio azul', (.045, .15, .23, 1), .48, .28),
    'niquel': ('Níquel acetinado', (.67, .75, .79, 1), .63, .34),
}


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    for lado in (-1, 1):
        painel(add, 'Placa azul da aleta', 'titanio_azul', lado,
               [(17.5, 6), (23, 10), (29, 4), (30, -5), (25, -10), (17.5, -6)],
               -12, -10.3)
        painel(add, 'Blindagem azul do flanco', 'titanio_azul', lado,
               [(7, -10), (16.5, -9), (18, 4), (15, 9), (7, 4)],
               -15.5, -10)
        add('cube', 'Filete de níquel', 'niquel',
            (lado*23.5, -12.35, -2), (10, 1, 3), bevel=.35)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Padrão III', partes(), PALETA, ship_materials)
