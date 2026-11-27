"""Baliza original do pedido de socorro da M01, com pulso azul."""
import math
from common import root, cube, sphere, rod, material, emissive

def build():
    r=root('Baliza SOS - apoio na borda inferior')
    metal=material('Aço da baliza',(.25,.32,.36,1),.65)
    marfim=material('Carcaça de emergência',(.78,.81,.70,1),.2)
    luz=emissive('Sinal azul',(.015,.58,1,1),1.7)
    cube('Base',(0,0,-58),(28,17,8),metal,r,2)
    rod('Mastro',(0,0,-55),(0,0,30),3,metal,r)
    for lado in (-1,1):
        rod('Apoio',(0,0,-38),(lado*16,0,-61),2,metal,r)
    cube('Transmissor',(0,0,23),(23,17,21),marfim,r,3)
    lente=sphere('Luz',(0,-10,25),(7,3,7),luz,r)
    for lado in (-1,1):
        rod('Antena',(lado*8,0,32),(lado*19,0,46),1.4,metal,r)
    def animar(i):
        fase=i*math.tau/8
        luz.node_tree.nodes.get('Emission').inputs['Strength'].default_value=.6+.5*(1+math.sin(fase+.23))/2
        lente.scale=(7*(1+.10*math.cos(fase)),3,7*(1+.10*math.cos(fase)))
    return animar
