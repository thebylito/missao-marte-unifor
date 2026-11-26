"""Guará III: coletes de aço escovado e faixas marfim sobre a pintura ocre."""
from nave_modelo import parts_radius_bound
from naves.comum import acrescentar, animacao, painel
from naves.guara_2_modelo import PALETA as ANTERIOR, partes as partes_anteriores

PALETA = {
    **ANTERIOR,
    'aco': ((.43, .33, .24, 1), .69, .33),
    'marfim_faixa': ((.90, .76, .48, 1), .22, .34),
}


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    for lado in (-1, 1):
        painel(add, 'Chapa de aço do módulo', 'aco', lado,
               [(20, 0), (29, 1.5), (32.5, -1.5), (32.5, -8),
                (28, -11.5), (20, -9.5)],
               -11.8, -10.3)
        painel(add, 'Faixa marfim diagonal', 'marfim_faixa', lado,
               [(20, -3), (29, .3), (31, -2.3), (20, -7.5)],
               -12.3, -11.65)
        painel(add, 'Placa de ombro marfim', 'marfim_faixa', lado,
               [(5, 9), (14.5, 8.5), (16, 2), (11.5, -1), (5, 3)],
               -12.7, -7)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Guará III', partes(), PALETA)
