import math
from common import root,sphere,emissive

def build():
    r=root('Projétil âmbar')
    luz=emissive('Energia',(1,.3,.025,1),2)
    bola=sphere('Núcleo',(0,0,0),(3,2,3),luz,r,2)
    def animar(i):
        bola.scale=(3+i*.45,2,3+i*.45)
    return animar
