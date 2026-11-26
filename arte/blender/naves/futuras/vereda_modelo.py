"""Abre caminho entre dunas marcianas com duas lâminas que afastam o regolito."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, cabine, casco, chassis, painel


def partes():
    parts, add = chassis()
    casco(add, 'Quilha estreita', 'musgo', [(-18,11,9), (-6,16,13), (17,12,10), (39,4,4)], 8)
    cabine(add, 20, 8, 11)
    for s in (-1, 1):
        painel(add, 'Lâmina de regolito', 'musgo', s,
               [(9,24), (22,6), (34,-12), (34,-18), (14,-12)], -4, 7)
        add('rod', 'Gume de aço', 'metal', (s*12,-5,21), (s*33,-5,-13), 1.4)
        add('rod', 'Nervura da lâmina', 'palha', (s*14,-5,4), (s*25,-5,-11), 1.2)
    add('cube', 'Filtro de areia', 'palha', (0,-12,-5), (17,4,9), bevel=1)
    for x in (-5, 0, 5):
        add('cube', 'Ranhura do filtro', 'estrutura', (x,-14,-5), (2,1,6))
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Vereda', partes(), {
        'musgo': ((.14,.33,.043,1), .16, .54),
        'palha': ((.81,.69,.32,1), .22, .47),
    })
