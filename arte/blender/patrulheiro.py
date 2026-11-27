"""Drone de patrulha original, com dois rotores e sensor frontal."""
import math
from common import root,cube,sphere,rod,material,emissive

def build():
    corpo=root('Patrulheiro de corredores')
    metal=material('Carcaça clara',(.55,.60,.61,1),.55,.38)
    escuro=material('Estrutura violeta',(.12,.08,.19,1),.35,.5)
    luz=emissive('Sensor âmbar',(1,.45,.035,1),1)
    cube('Corpo central',(0,0,0),(26,18,18),metal,corpo,3)
    cube('Sensor frontal',(0,-11,1),(12,3,5),escuro,corpo,1)
    sphere('Lente',(0,-13,1),(3,2,2),luz,corpo)
    rotores=[]
    for lado in (-1,1):
        rod('Braço',(lado*9,0,0),(lado*28,0,1),2.5,escuro,corpo)
        suporte=root('Rotor');suporte.parent=corpo;suporte.location=(lado*29,0,0)
        sphere('Cubo',(0,0,0),(4,3,4),metal,suporte)
        for n in range(3):
            a=n*math.tau/3
            rod('Pá',(2*math.cos(a),0,2*math.sin(a)),
                (12*math.cos(a),0,12*math.sin(a)),1.6,escuro,suporte)
        rotores.append(suporte)
    def animar(i):
        for lado,rotor in enumerate(rotores):
            rotor.rotation_euler[1]=(1 if lado else -1)*math.tau*i/8
        luz.node_tree.nodes.get('Emission').inputs['Strength'].default_value=1+.25*math.sin(i*math.tau/8)
    return animar
