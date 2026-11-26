"""Triangula pedidos de socorro com um refletor aberto sobre a cabine."""
from nave_modelo import parts_radius_bound
from ..comum import animacao, cabine, casco, chassis, painel


def partes():
    parts, add = chassis()
    casco(add, 'Casco de telecomunicações', 'vinho', [(-18,11,9), (-8,22,13),
          (4,25,14), (15,15,11), (24,8,7)], 12)
    cabine(add, 16, 9, 7)
    add('rod', 'Suporte do refletor', 'metal', (0,0,19), (0,0,31), 3)
    for s in (-1, 1):
        painel(add, 'Pétala parabólica', 'aluminio', s,
               [(0,25), (13,29), (28,43), (16,37), (0,32)], -2, 3)
        add('rod', 'Haste do receptor', 'metal', (s*20,0,37), (0,0,44), .8)
        add('sphere', 'Conversor lateral', 'aluminio', (s*22,0,-5), (6,7,9))
    add('sphere', 'Receptor focal', 'vinho', (0,0,44), (2.6,2.6,2.6))
    for z in (-9,-4,1):
        add('cube', 'Aleta do transmissor', 'aluminio', (0,-14,z), (17,3,1.6), bevel=.5)
    return parts


def geometry_radius_bound():
    return parts_radius_bound(partes())


def build():
    return animacao('Escuta', partes(), {
        'vinho': ((.47,.022,.104,1), .42, .27),
        'aluminio': ((.73,.70,.67,1), .71, .26),
    })
