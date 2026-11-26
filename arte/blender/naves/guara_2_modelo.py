"""Guará II: antenas de resgate, módulos laterais e tirantes do guincho."""
from nave_modelo import parts_radius_bound
from naves.comum import acrescentar, animacao, painel
from naves.guara_modelo import PALETA, partes as partes_anteriores


def partes():
    parts = partes_anteriores()
    add = acrescentar(parts)
    for lado in (-1, 1):
        painel(add, 'Módulo lateral de ancoragem', 'estrutura', lado,
               [(19, 1), (30, 3), (34, -1), (34, -9), (29, -13), (19, -11)],
               -10.5, -3)
        add('rod', 'Antena de resgate', 'metal',
            (lado*20, -1, 7), (lado*18, -1, 21), 1.4)
        add('sphere', 'Ponta de baliza', 'marfim',
            (lado*18, -1, 21), (2, 2, 2))
        add('rod', 'Tirante do guincho', 'metal',
            (lado*10, -14, -7), (lado*20, -9, -19), 2.1)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Guará II', partes(), PALETA)
