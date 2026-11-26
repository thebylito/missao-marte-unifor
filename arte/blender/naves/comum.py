"""Componentes físicos compartilhados pelas naves de resgate."""
import math
from nave_modelo import _rings, assemble, parts_radius_bound, FOOT_X, FOOT_BOTTOM_Z, NOZZLE_EXIT_Z


def chassis():
    parts = []

    def add(kind, nome, acabamento, *args, bevel=0):
        parts.append((kind, nome, acabamento, args, bevel))

    # Perfil contínuo passa pelo lábio e retorna pela parede interna do bocal.
    add('mesh', 'Bocal refratário', 'metal', *_rings((
        (-15, 6, 6), (-21, 5, 5), (NOZZLE_EXIT_Z, 8, 8),
        (NOZZLE_EXIT_Z, 6.6, 6.6), (-21, 3.8, 3.8)), caps=False))
    for lado in (-1, 1):
        x = lado * FOOT_X
        add('rod', 'Pistão de pouso', 'metal', (lado*13, 1, -10), (x, 1, -37), 2)
        add('rod', 'Contraventamento', 'estrutura', (lado*24, 0, -9), (x, 0, -37), 1.7)
        add('cube', 'Sapata', 'estrutura', (x, 0, FOOT_BOTTOM_Z+2), (12, 10, 4), bevel=.8)
    return parts, add


def animacao(nome, parts, paleta):
    from common import root, material
    acabamentos = {
        'estrutura': ((.085, .077, .073, 1), .45, .34),
        'metal': ((.48, .43, .36, 1), .65, .31),
        'vidro': ((.01, .52, .73, 1), .12, .15, .35),
        **paleta,
    }
    mats = {k: material(nome+' '+k, *v) for k, v in acabamentos.items()}
    bound, _ = parts_radius_bound(parts)
    assert bound < 56
    ship = assemble(root(nome), parts, mats, bound)

    def animate(i):
        ship.rotation_euler[1] = -math.tau*i/64
    return animate
