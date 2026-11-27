"""Variações originais de rocha, construídas na escala dos sprites existentes."""
import math
import random
from mathutils import Vector, Quaternion
from common import sphere, material, root, emissive

def construir(tipo):
    rng = random.Random(733 + len(tipo))
    grupo = root('Rotação ' + tipo)
    cores = {
        'craterado': (.29, .13, .065, 1),
        'gelo_polar': (.21, .53, .69, 1),
        'ferro': (.20, .23, .25, 1),
        'meteoro': (.19, .052, .015, 1),
    }
    mat = material(tipo, cores[tipo], .72 if tipo == 'ferro' else .1, .35 if tipo == 'gelo_polar' else .82)
    obj = sphere(tipo, (0,0,0), (1,1,1), mat, grupo, 3 if tipo == 'craterado' else 2)
    crateras = [Vector((.3,-.8,.4)).normalized(), Vector((-.6,-.6,-.4)).normalized()]
    for vertice in obj.data.vertices:
        d = vertice.co.normalized()
        raio = 1 + .12*math.sin(d.x*9+d.z*4) + rng.uniform(-.045,.045)
        if tipo == 'craterado':
            for centro in crateras:
                distancia = (d-centro).length
                raio -= .32*math.exp(-(distancia/.28)**4)
                raio += .06*math.exp(-((distancia-.32)/.065)**2)
        vertice.co = (d.x*raio*37, d.y*raio*30, d.z*raio*(31 if tipo == 'ferro' else 38))
    limite=max(v.co.length for v in obj.data.vertices)
    for v in obj.data.vertices: v.co *= 43/limite
    obj.data.update()
    if tipo == 'gelo_polar':
        detalhe=material('Fraturas azul profundo',(.035,.15,.24,1),.15,.3)
    elif tipo == 'ferro':
        detalhe=material('Veio de níquel',(.55,.41,.23,1),.85,.27)
    elif tipo == 'meteoro':
        detalhe=emissive('Fendas incandescentes',(1,.21,.015,1),2.5)
    else:
        detalhe=material('Interior escuro',(.095,.035,.019,1),0,.95)
    obj.data.materials.append(detalhe)
    for face in obj.data.polygons:
        if rng.random() < (.23 if tipo == 'meteoro' else .15): face.material_index=1
    eixo=Vector((.36,.84,.40)).normalized()
    grupo.rotation_mode='QUATERNION'
    def animar(i): grupo.rotation_quaternion=Quaternion(eixo,2*math.pi*i/32)
    return animar
