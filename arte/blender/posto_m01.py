"""Módulo da base de resgate e antena móvel, original da campanha."""
import math
from common import root, cube, sphere, rod, material, emissive

def build():
    r=root('Posto Expedição - apoio na borda inferior')
    parede=material('Painéis cerâmicos',(.55,.61,.59,1),.3)
    escuro=material('Porta e vedação',(.07,.10,.13,1),.35)
    metal=material('Estrutura do posto',(.20,.25,.28,1),.6)
    vidro=material('Janela azul',(.02,.28,.39,1),.3,.18)
    luz=emissive('Sinal da base',(.12,.85,.55,1),1.7)
    cube('Fundação',(-18,0,-57),(154,54,10),metal,r,2)
    cube('Habitação',(-25,0,-23),(130,48,60),parede,r,7)
    cube('Teto',(-25,0,9),(141,54,8),metal,r,3)
    cube('Porta',(-55,-25,-31),(25,2,46),escuro,r,3)
    cube('Janela',(-5,-25,-19),(38,2,24),vidro,r,3)
    for x in (-15,5): rod('Grade da janela',(x,-27,-30),(x,-27,-8),1,metal,r)
    cube('Indicador',(-55,-27,-4),(14,2,4),luz,r,1)
    rod('Mastro de comunicação',(51,0,-52),(51,0,34),3,metal,r)
    antena=root('Antena articulada');antena.parent=r;antena.location=(51,0,34)
    sphere('Receptor',(0,0,0),(19,6,12),parede,antena)
    rod('Foco',(0,-5,0),(0,-16,0),1.5,metal,antena)
    sphere('Sinal do receptor',(0,-17,0),(3,2,3),luz,antena)
    def animar(i):
        antena.rotation_euler[2]=.22*math.sin(i*math.tau/8)
        antena.rotation_euler[1]=.17*math.cos(i*math.tau/8)
    return animar
