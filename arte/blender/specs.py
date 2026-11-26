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
    dict(id='padrao', nome='Padrão', descricao='Nave de reconhecimento e resgate em Marte.', raridade='comum'),
    dict(id='guara', nome='Guará', descricao='Rebocador de cânions com guincho frontal e dois cofres de ancoragem.', raridade='comum'),
    dict(id='bruma', nome='Bruma', descricao='Ambulância de altitude com duas cápsulas laterais de evacuação.', raridade='rara'),
]
SKINS = tuple(item['id'] for item in CATALOGO[1:])
for skin in SKINS:
    SPECS[skin] = Spec(f'naves.{skin}_modelo', f'naves/{skin}.png', SPECS['nave'].grid)
