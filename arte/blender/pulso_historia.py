import math
from common import root,rod,emissive

def build():
    r=root('Frente do pulso')
    luz=emissive('Plasma ciano',(.15,.8,1,1),1.4)
    segmentos=[]
    for i in range(16):
        a=math.pi/6+i*math.pi*2/3/16
        b=a+math.pi*2/3/16
        segmentos.append(rod('Arco',(math.cos(a),0,math.sin(a)),
                             (math.cos(b),0,math.sin(b)),.06,luz,r))
    def animar(i):
        escala=8+i*4
        r.scale=(escala,escala,escala)
        r.location.z=-24
    return animar
