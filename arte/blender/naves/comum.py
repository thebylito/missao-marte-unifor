"""Componentes físicos compartilhados pelas naves de resgate."""
import math
from nave_modelo import _rings, assemble, parts_radius_bound, FOOT_X, FOOT_BOTTOM_Z, NOZZLE_EXIT_Z


def acrescentar(parts):
    def add(kind, nome, acabamento, *args, bevel=0):
        parts.append((kind, nome, acabamento, args, bevel))

    return add


def chassis():
    parts = []
    add = acrescentar(parts)
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


def animacao(nome, parts, paleta, preparar_materiais=None):
    from common import root, material
    if preparar_materiais is None:
        acabamentos = {
            'estrutura': ((.085, .077, .073, 1), .45, .34),
            'metal': ((.48, .43, .36, 1), .65, .31),
            'vidro': ((.01, .52, .73, 1), .12, .15, .35),
            **paleta,
        }
        mats = {k: material(nome+' '+k, *v) for k, v in acabamentos.items()}
    else:
        mats = preparar_materiais(paleta)
    bound, _ = parts_radius_bound(parts)
    assert bound < 56
    ship = assemble(root(nome), parts, mats, bound)

    def animate(i):
        ship.rotation_euler[1] = -math.tau*i/64
    return animate


def anel_bocal(add, acabamento):
    # A faixa externa deixa intactos a abertura e o plano de saída.
    add('mesh', 'Anel externo do bocal', acabamento, *_rings((
        (NOZZLE_EXIT_Z + .2, 8.2, 8.2),
        (-25, 7.25, 7.25),
        (-24.8, 6.75, 6.75),
        (NOZZLE_EXIT_Z + .2, 7.5, 7.5),
        (NOZZLE_EXIT_Z + .2, 8.2, 8.2)), caps=False))


def casco(add, nome, material, perfil, segmentos):
    add('mesh', nome, material, *_rings(perfil, segments=segmentos), bevel=.65)


def cabine(add, z, largura, altura):
    add('sphere', 'Moldura do visor', 'estrutura', (0,-8,z), (largura+1.5,6,altura+1.5))
    add('sphere', 'Vidro da cabine', 'vidro', (0,-10,z), (largura,5.5,altura))


def painel(add, nome, material, lado, contorno, frente, fundo):
    """Extruda um contorno no plano XZ, mantendo as normais nos dois lados."""
    pontos = [(lado*x,z) for x,z in contorno]
    area = sum(x*w-u*z for (x,z),(u,w) in zip(pontos, pontos[1:]+pontos[:1]))
    if area < 0:
        pontos.reverse()
    n = len(pontos)
    verts = [(x,y,z) for y in (frente,fundo) for x,z in pontos]
    faces = [tuple(range(n)), tuple(reversed(range(n,2*n)))]
    for i in range(n):
        j = (i+1)%n
        faces.append((j,i,i+n,j+n))
    add('mesh', nome, material, verts, faces, bevel=.7)
