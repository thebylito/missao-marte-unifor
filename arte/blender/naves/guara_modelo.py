"""Rebocador de cânion com guincho exposto e dois cofres de ancoragem."""
from nave_modelo import _rings, parts_radius_bound
from naves.comum import chassis, animacao


def partes():
    parts, add = chassis()
    add('mesh', 'Casco sextavado', 'ocre', *_rings((
        (-17, 13, 10), (-10, 18, 14), (13, 16, 13),
        (26, 10, 9), (36, 4, 4)), segments=6), bevel=.8)
    add('rod', 'Mastro de baliza', 'estrutura', (0, 0, 35), (0, 0, 42), 1.4)
    add('sphere', 'Baliza de busca', 'marfim', (0, 0, 42), (2.2, 2.2, 2.2))
    add('cube', 'Aro do para-brisa', 'estrutura', (0, -10.8, 18), (22, 6, 14), bevel=2.5)
    add('cube', 'Para-brisa panorâmico', 'vidro', (0, -13, 18), (18.5, 4, 10.5), bevel=2)
    for lado in (-1, 1):
        x = lado*21
        add('cube', 'Cofre de âncoras', 'ocre', (x, 0, -1), (13, 17, 27), bevel=2.6)
        add('cube', 'Faixa de visibilidade', 'marfim', (x, 0, 5), (13.3, 17.4, 4), bevel=.6)
        for z in (-8, -4):
            add('cube', 'Veneziana', 'estrutura', (x, -8.7, z), (8, 1.5, 1.7), bevel=.4)
        add('cube', 'Mancal', 'estrutura', (lado*9, -14, -3), (4, 7, 13), bevel=1)
    add('rod', 'Tambor do guincho', 'metal', (-8,-15,-3), (8,-15,-3), 4)
    for x in (-5, -2, 1, 4):
        add('rod', 'Espira do cabo', 'marfim', (x,-15,-3), (x+1.5,-15,-3), 4.3)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Guará', partes(), {
        'ocre': ((.91,.21,.023,1), .26, .37),
        'marfim': ((.98,.83,.54,1), .13, .4),
    })
