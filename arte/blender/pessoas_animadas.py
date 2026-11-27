"""Animações de trabalho, reação e caminhada sobre os modelos originais."""
import math
import bpy
from mathutils import Vector
from common import root, cube, sphere, cone, emissive
from pessoas import _palette, _astronaut, _engineer, _professor, _wave, postprocess


def agrupar(nome, objetos, origem, pai):
    grupo = root(nome)
    grupo.parent = pai
    grupo.location = origem
    bpy.context.view_layer.update()
    for objeto in objetos:
        matriz = objeto.matrix_world.copy()
        objeto.parent = grupo
        objeto.matrix_world = matriz
    return grupo


def posicionar_segmento(objeto, inicio, fim):
    inicio, fim = Vector(inicio), Vector(fim)
    objeto.location = (inicio + fim) / 2
    objeto.rotation_euler = (fim-inicio).to_track_quat('Z','Y').to_euler()
    objeto.scale.z = (fim-inicio).length


def construir(tipo, sequencia):
    personagem = root(tipo + ' animado')
    personagem.location.z = -8  # pés em Z=-40, borda inferior da célula de 80 px
    paleta = _palette(tipo)
    {'astronauta': _astronaut, 'engenheiro': _engineer,
     'professor': _professor}[tipo](personagem, paleta)
    acenar = _wave(personagem, paleta, tipo)
    ombro = next(o for o in personagem.children if o.name.startswith('Waving shoulder'))
    cotovelo = next(o for o in ombro.children if o.name.startswith('Waving elbow'))
    pernas = [o for o in personagem.children if o.name.startswith('Trouser leg')]
    botas = sorted([o for o in personagem.children if o.name.startswith('Flat sole boot')],
                   key=lambda o: o.location.x)
    for perna in pernas:
        perna.hide_render = True
    for objeto in personagem.children:
        if objeto.name.startswith('White knee band'):
            objeto.hide_render = True
    membros = []
    for bota in botas:
        coxa = cone('Coxa articulada',(0,0,0),3,3,1,paleta['suit'],personagem)
        canela = cone('Canela articulada',(0,0,0),2.8,2.8,1,paleta['suit'],personagem)
        membros.append((coxa,canela,bota))
    excluidos = set(pernas + botas + [o for membro in membros for o in membro[:2]])
    corpo = agrupar('Corpo sobre os quadris',
                    [o for o in personagem.children if o not in excluidos],
                    (0,0,-16), personagem)
    ferramentas = [o for o in corpo.children if any(o.name.startswith(n) for n in
                    ('Wrench','Open wrench','Tool holding','Tool arm','Yellow resting','Resting blue'))]
    registros = [o for o in corpo.children if any(o.name.startswith(n) for n in
                  ('Clipboard','Written note','Resting coat'))]
    braco = agrupar('Braço de trabalho', ferramentas or registros, (-8,0,15), corpo)
    origem_braco = braco.location.copy()
    caixa = None
    if tipo == 'engenheiro':
        caixa = cube('Caixa de ferramentas',(-13,0,-28),(12,8,7),paleta['brown'],personagem,1)
        cube('Trava amarela da caixa',(-13,-4.2,-27),(3,.6,2),paleta['yellow'],caixa,.2)
        # A trava usa coordenadas locais do objeto pai.
        caixa.children[0].location = (0,-4.2,1)
    if tipo == 'professor':
        cube('Lanterna de sinalização',(0,-1,9),(3.5,4,7),paleta['metal'],cotovelo,.5)
        sphere('Lente da lanterna',(0,-3.2,12),(2,1,1.4),
               emissive('Luz âmbar',(.9,.65,.2,1),1.4),cotovelo,2)
    objetos_corpo = [(o, o.location.copy(), o.rotation_euler.copy()) for o in corpo.children]
    def animar(i):
        for o, local, rotacao in objetos_corpo:
            o.location = local
            o.rotation_euler = rotacao
        fase = 2*math.pi*i/(16 if sequencia=='espera' else 12)
        progresso = i/11
        suavizado = progresso*progresso*(3-2*progresso)
        trabalho = (1-math.cos(fase))/2
        agachar = 0
        subir = 0
        if sequencia == 'espera':
            ombro.rotation_euler[1] = 2.7 + .06*math.sin(fase)
            cotovelo.rotation_euler[1] = -.35
            if tipo == 'engenheiro':
                agachar = 8*trabalho
                braco.rotation_euler[1] = -.24*trabalho
                braco.location.z = origem_braco.z + 4*trabalho
            elif tipo == 'professor':
                braco.rotation_euler[1] = -.2*trabalho
                braco.location.z = origem_braco.z + 3*trabalho
                corpo.rotation_euler[1] = .04*math.sin(fase)
            else:
                ombro.rotation_euler[1] = 2.5 - .5*trabalho
                cotovelo.rotation_euler[1] = -.35 - .3*trabalho
                corpo.rotation_euler[1] = .03*math.sin(fase)
        elif sequencia == 'reacao':
            agachar = 8*(1-suavizado) if tipo == 'engenheiro' else 0
            ombro.rotation_euler[1] = 2.7*(1-suavizado)
            cotovelo.rotation_euler[1] = -.35*(1-suavizado) - .8*math.sin(math.pi*suavizado)
            if tipo == 'professor':
                braco.rotation_euler[1] = .65*suavizado
                braco.location.z = origem_braco.z + 3*(1-suavizado)
        elif sequencia == 'proteger':
            agachar = 4 + .6*math.sin(fase)
            ombro.rotation_euler[1] = -.65 + .06*math.sin(fase)
            cotovelo.rotation_euler[1] = -.7
            corpo.rotation_euler[1] = -.06 + .02*math.cos(fase)
        elif sequencia == 'comemorar':
            acenar(i*8/12)
            subir = max(0, math.sin(fase))*3
        elif sequencia == 'sinalizar':
            acenar(i*8/12)
            subir = .2*math.sin(fase)
        else:
            ombro.rotation_euler[1] = 2.7 + .12*math.sin(fase)
            cotovelo.rotation_euler[1] = -.35
            subir = abs(math.sin(fase))*.8
            corpo.rotation_euler[1] = .025*math.cos(fase)
            braco.rotation_euler[1] = .1*math.sin(fase+math.pi)
        corpo.location.z = -16 - agachar + subir
        if caixa:
            caixa.hide_render = sequencia != 'espera'
            for o in caixa.children: o.hide_render = caixa.hide_render
        for indice, (coxa,canela,bota) in enumerate(membros):
            sinal = -1 if indice==0 else 1
            passo = math.sin(fase + indice*math.pi) if sequencia=='andar' else 0
            pe = Vector((sinal*4.8 + 3*passo,0,-29+max(0,passo)*3))
            quadril = Vector((sinal*4.8,0,-16-agachar+subir))
            tornozelo = pe + Vector((0,0,3))
            vetor = tornozelo - quadril
            meio = (quadril+tornozelo)/2
            perpendicular = Vector((-vetor.z,0,vetor.x)).normalized()
            altura = math.sqrt(max(0, 6.4**2 - (vetor.length/2)**2))
            joelho = meio + perpendicular*altura*sinal
            posicionar_segmento(coxa,quadril,joelho)
            posicionar_segmento(canela,joelho,tornozelo)
            bota.location = pe + Vector((0,-1.3,0))
    return animar
