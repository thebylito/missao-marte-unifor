import numpy as np
import fundo
from fundo import postprocess

def build():
    linear = fundo._linear
    relevo = fundo._ridge
    def cor(rgb):
        c = np.asarray(rgb)
        return linear(c * np.array([.78, .80, .92]))
    fundo._linear = cor
    fundo._ridge = lambda xs, seed, base, amplitude, mesa=False: relevo(xs, seed+219, base, amplitude*1.18, mesa)
    try:
        return fundo.build()
    finally:
        fundo._linear = linear
        fundo._ridge = relevo
