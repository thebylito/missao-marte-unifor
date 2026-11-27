"""Equipamentos abandonados que identificam as cavernas, em coordenadas XZ."""
import math
import bpy
from common import root, cube, rod, sphere, material, emissive
from pessoas_animadas import agrupar

def construir(tipo):
    r=root(tipo)
    metal=material('Estrutura envelhecida',(.16,.20,.23,1),.65,.48)
    claro=material('Painel cerâmico',(.58,.65,.66,1),.18,.6)
    laranja=material('Faixas de resgate',(.72,.21,.035,1),.1,.7)
    azul=material('Gelo da sonda',(.15,.38,.49,1),.1,.4)
    if tipo=='guindaste':
        for x in (-90,-45):
            rod('Torre', (x,0,-105),(x,0,85),5,metal,r)
        rod('Lança',(-105,0,85),(145,0,85),6,laranja,r)
        for z in range(-90,80,32):
            rod('Treliça',(-90,0,z),(-45,0,z+32),2,metal,r)
        rod('Cabo', (112,0,85),(112,0,-45),1.3,metal,r)
        rod('Gancho',(112,0,-45),(123,0,-54),3,metal,r)
        cube('Base',(-65,0,-112),(100,40,16),metal,r,3)
    elif tipo=='sonda':
        cube('Corpo da sonda',(0,0,-25),(65,35,70),claro,r,5)
        cube('Faixa',(0,-19,-25),(66,3,13),laranja,r,1)
        rod('Mastro',(0,0,10),(8,0,86),2,metal,r)
        sphere('Antena',(8,0,89),(12,6,7),metal,r)
        for x in (-1,1):
            cube('Painel solar',(x*85,0,-8),(95,8,55),azul,r,1)
            rod('Suporte',(x*20,0,-55),(x*62,0,-110),3,metal,r)
        for x in range(-72,85,36):
            sphere('Gelo preso à base',(x,-8,-108),(24,15,16),azul,r,1)
    else:
        rod('Mastro',(-45,0,-110),(-45,0,78),4,metal,r)
        for z in range(-90,70,35):
            rod('Antena transversal',(-100,0,z),(12,0,z+12),2,claro,r)
        cube('Caixa de rádio',(-25,-4,-75),(65,30,50),laranja,r,4)
        rod('Cabo caído',(-5,0,-98),(120,0,-110),2,metal,r)
    if tipo == 'guindaste':
        nomes, origem = ('Cabo', 'Gancho'), (112,0,85)
    elif tipo == 'sonda':
        nomes, origem = ('Mastro', 'Antena'), (0,0,10)
    else:
        nomes, origem = ('Mastro', 'Antena transversal'), (-45,0,-110)
    movimento = agrupar('Equipamento ao vento',
                        [o for o in r.children if o.name.startswith(nomes)], origem, r)
    lampada = emissive('Luz de presença',(.35,.8,.95,1),1)
    sphere('Indicador de funcionamento',(-20,-22,-70),(3,2,3),lampada,r)
    def animar(i):
        fase = 2*math.pi*i/8
        movimento.rotation_euler[1] = .035*math.sin(fase)
        lampada.node_tree.nodes.get('Emission').inputs['Strength'].default_value = .4+.6*(1+math.cos(fase))
    return animar
