"""Padrão V: arco de comando dourado, cromo e pulsos de luz mais fortes."""
from nave_modelo import parts_radius_bound, ship_materials
from naves.comum import acrescentar, animacao, painel
from naves.padrao_4_modelo import PALETA as ANTERIOR, partes as partes_anteriores

PALETA = {
    **ANTERIOR,
    'cyan': ('Cabine ciano de alta intensidade', (.016, .45, .62, 1), 0, .15, 1.35),
    'cyan_band': ('Faixa ciano de alta intensidade', (.016, .45, .62, 1), 0, .15, 1.05),
    'sinal': ('Faixas ciano de alta intensidade', (.018, .38, .67, 1), .08, .25, 1.45),
    'anel': ('Anel âmbar de alta intensidade', (1, .26, .032, 1), .22, .3, 1.85),
    'ouro': ('Ouro escovado', (.84, .46, .072, 1), .72, .27),
    'cromo': ('Cromo polido', (.73, .79, .84, 1), .83, .22),
}


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    for lado in (-1, 1):
        painel(add, 'Arco dourado de comando', 'ouro', lado,
               [(0, 41), (8, 36), (13, 26), (13, 17),
                (10, 18), (9, 26), (5, 34), (0, 37)],
               -12.5, 1)
        add('rod', 'Friso cromado de comando', 'cromo',
            (lado*8, -12.65, 32), (lado*11.5, -12.65, 24), .75)
        painel(add, 'Guarnição dourada da aleta', 'ouro', lado,
               [(24, 8), (29, 4), (30, -5), (26, -9),
                (23, -8), (27, -4), (26, 3), (23, 6)],
               -12.9, -11.6)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Padrão V', partes(), PALETA, ship_materials)
