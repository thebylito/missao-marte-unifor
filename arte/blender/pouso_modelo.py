"""Compressão da suspensão mantendo as sapatas no solo."""
import importlib
import bpy
from pessoas_animadas import agrupar

def construir(skin):
    modelo = importlib.import_module('nave' if skin == 'padrao' else 'naves.' + skin + '_modelo')
    modelo.build()(0)
    nave = bpy.data.objects['Porta esquerda'].parent
    fixos = ('landing pad', 'Sapata')
    hastes = ('landing strut', 'triangular brace', 'Pistão de pouso', 'Contraventamento',
              'Reforço de pouso')
    pernas = [o for o in nave.children if any(n in o.name for n in hastes)]
    corpo = agrupar('Casco suspenso',
                    [o for o in nave.children if o not in pernas and not any(n in o.name for n in fixos)],
                    (0,0,0), nave)
    bpy.context.view_layer.update()
    vertices = [(o, o.matrix_world.copy(), o.matrix_world.inverted(),
                 [v.co.copy() for v in o.data.vertices]) for o in pernas]
    def animar(i):
        compressao = 4 * i / 7
        corpo.location.z = -compressao
        for objeto, matriz, inversa, originais in vertices:
            for vertice, original in zip(objeto.data.vertices, originais):
                mundo = matriz @ original
                mundo.z -= compressao * max(0, min(1, (mundo.z+38)/28))
                vertice.co = inversa @ mundo
            objeto.data.update()
    return animar
