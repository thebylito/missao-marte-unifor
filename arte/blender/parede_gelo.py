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
    a[:,:3]=a[:,:3][:,[2,1,0]]*np.array([1.5,1.8,1.4])
    cores.data.foreach_set('color',a.ravel())
    return animar
