import bpy
import numpy as np
from rocha_tile import build as rocha, postprocess

def build():
    animar=rocha()
    dados=bpy.data.objects['Three by three periods of rock'].data
    cores=dados.color_attributes['RockColour']
    a=np.empty(len(cores.data)*4,dtype=np.float32)
    cores.data.foreach_get('color',a)
    a=a.reshape(-1,4)
    a[:,:3]=a[:,:3]*np.array([.75,1.4,1.8])
    cores.data.foreach_set('color',a.ravel())
    return animar
