"""Guará IV: balizas âmbar, anel emissivo e para-brisa mais luminoso."""
from nave_modelo import parts_radius_bound
from naves.comum import acrescentar, anel_bocal, animacao
from naves.guara_3_modelo import PALETA as ANTERIOR, partes as partes_anteriores

PALETA = {
    **ANTERIOR,
    'vidro': ((.012, .52, .73, 1), .12, .15, .95),
    'sinal': ((1, .31, .035, 1), .12, .22, 1.25),
}


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    anel_bocal(add, 'sinal')
    add('sphere', 'Baliza âmbar no mastro', 'sinal', (0, -1.6, 42), (1.6, 1.3, 1.6))
    for lado in (-1, 1):
        add('rod', 'Faixa emissiva de resgate', 'sinal',
            (lado*21, -12.5, -4.5), (lado*30, -12.5, -1.5), 1.25)
        add('cube', 'Luz no cofre de ancoragem', 'sinal',
            (lado*21, -9.05, 5), (10, 1.4, 2.5), bevel=.5)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Guará IV', partes(), PALETA)
