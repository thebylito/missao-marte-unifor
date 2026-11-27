import numpy as np
import fundo
from fundo import postprocess

def build():
    linear = fundo._linear
    relevo = fundo._ridge
    def cor(rgb):
        c = np.asarray(rgb)
        return linear(c[..., [2,1,0]] * np.array([.82, 1.08, 1.18]))
    fundo._linear = cor
    fundo._ridge = lambda xs, seed, base, amplitude, mesa=False: relevo(xs, seed+219, base, amplitude*1.18, mesa)
    try:
        return fundo.build()
    finally:
        fundo._linear = linear
        fundo._ridge = relevo
