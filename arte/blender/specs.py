"""Literal contract metadata; coordinates and pivots use top-left image origin."""
from typing import NamedTuple


class Spec(NamedTuple):
    module: str
    output: str
    grid: tuple


SPECS = {
    'nave': Spec('nave', 'nave.png', (128,128,64,8,8)),
    'chama': Spec('chama', 'chama.png', (64,96,4,4,1)),
    'explosao': Spec('explosao', 'explosao.png', (128,128,16,4,4)),
    'asteroide': Spec('asteroide', 'asteroide.png', (96,96,32,8,4)),
    'plataforma_base': Spec('plataforma_base', 'plataforma_base.png', (256,64,1,1,1)),
    'plataforma_passageiro': Spec('plataforma_passageiro', 'plataforma_passageiro.png', (160,48,1,1,1)),
    'astronauta': Spec('astronauta', 'astronauta.png', (48,64,8,8,1)),
    'engenheiro': Spec('engenheiro', 'engenheiro.png', (48,64,8,8,1)),
    'professor': Spec('professor', 'professor.png', (48,64,8,8,1)),
    'rocha_tile': Spec('rocha_tile', 'rocha_tile.png', (256,256,1,1,1)),
    'fundo': Spec('fundo', 'fundo.png', (1200,900,1,1,1)),
    'icone': Spec('icone', 'icone.png', (256,256,1,1,1)),
    'inimigo': Spec('inimigo', 'inimigo.png', (96,96,8,8,1)),
}

CATALOGO = [
    dict(id='padrao', nome='Padrão', descricao='Nave de reconhecimento e resgate com casco cerâmico e cabine ciano.',
         linha='padrao', nivel=1),
    dict(id='padrao_2', nome='Padrão II', descricao='Acrescenta aletas de serviço, antenas e reforços de pouso.',
         linha='padrao', nivel=2),
    dict(id='padrao_3', nome='Padrão III', descricao='Acrescenta placas de titânio azul e filetes de níquel à cerâmica clara.',
         linha='padrao', nivel=3),
    dict(id='padrao_4', nome='Padrão IV', descricao='Acende as faixas de navegação, o anel do bocal e a cabine ciano.',
         linha='padrao', nivel=4),
    dict(id='padrao_5', nome='Padrão V', descricao='Acrescenta um arco dourado com frisos cromados e intensifica as luzes.',
         linha='padrao', nivel=5),
    dict(id='guara', nome='Guará', descricao='Rebocador de cânions com guincho frontal e dois cofres de ancoragem.',
         linha='guara', nivel=1),
    dict(id='guara_2', nome='Guará II', descricao='Acrescenta antenas de resgate, módulos de ancoragem e tirantes no guincho.',
         linha='guara', nivel=2),
    dict(id='guara_3', nome='Guará III', descricao='Acrescenta chapas de aço escovado e faixas marfim à pintura ocre.',
         linha='guara', nivel=3),
    dict(id='guara_4', nome='Guará IV', descricao='Acende as faixas de resgate, as balizas e o anel do bocal e reforça o brilho da cabine.',
         linha='guara', nivel=4),
]
SKINS = tuple(item['id'] for item in CATALOGO[1:])
for skin in SKINS:
    SPECS[skin] = Spec(f'naves.{skin}_modelo', f'naves/{skin}.png', SPECS['nave'].grid)

SPECS['patrulheiro'] = Spec('patrulheiro','patrulheiro.png',(96,64,8,8,1))
SPECS['disparo_historia'] = Spec('disparo_historia','disparo_historia.png',(24,24,4,4,1))
SPECS['pulso_historia'] = Spec('pulso_historia','pulso_historia.png',(96,96,8,4,2))
