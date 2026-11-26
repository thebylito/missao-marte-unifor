"""Padrão IV: faixas acesas, anel emissivo e cabine iluminada."""
from nave_modelo import parts_radius_bound, ship_materials
from naves.comum import acrescentar, anel_bocal, animacao
from naves.padrao_3_modelo import PALETA as ANTERIOR, partes as partes_anteriores

PALETA = {
    **ANTERIOR,
    'cyan': ('Cabine ciano iluminada', (.018, .45, .62, 1), 0, .15, .95),
    'cyan_band': ('Faixa ciano iluminada', (.018, .45, .62, 1), 0, .15, .70),
    'sinal': ('Faixas de navegação ciano', (.025, .38, .67, 1), .08, .25, 1.05),
    'anel': ('Anel âmbar', (1, .26, .032, 1), .22, .3, 1.15),
}


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    anel_bocal(add, 'anel')
    for lado in (-1, 1):
        add('rod', 'Faixa luminosa diagonal', 'sinal',
            (lado*19, -12.3, 5), (lado*23, -12.3, 8), 1.2)
        add('rod', 'Faixa luminosa vertical', 'sinal',
            (lado*13, -15.8, -6), (lado*13, -15.8, 5), 1.25)
        add('cube', 'Baliza dianteira', 'sinal',
            (lado*14, -3, 24), (2.1, 1.5, 2.7), bevel=.35)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Padrão IV', partes(), PALETA, ship_materials)
