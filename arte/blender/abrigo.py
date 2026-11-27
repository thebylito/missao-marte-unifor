"""Abrigo de resgate com luz de recepção."""
import math
from common import root,cube,sphere,material,emissive

def build():
    r=root('Abrigo da base')
    corpo=material('Cerâmica do abrigo',(.28,.32,.35,1),.2,.7)
    metal=material('Moldura do abrigo',(.09,.12,.14,1),.5,.4)
    luz=emissive('Recepção verde',(.2,.8,.6,1),1)
    cube('Módulo do abrigo',(0,0,-12),(80,40,70),corpo,r,8)
    cube('Porta do abrigo',(0,-22,-19),(26,3,48),metal,r,3)
    cube('Luz da recepção',(0,-24,8),(22,2,3),luz,r,1)
    for x in (-28,28): cube('Janela',(x,-22,-8),(14,2,12),metal,r,2)
    def animar(i):
        luz.node_tree.nodes.get('Emission').inputs['Strength'].default_value=1+.25*math.sin(i*math.pi/4)+.12*math.cos(i*math.pi/4)
    return animar
