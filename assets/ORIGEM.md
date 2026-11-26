# Origem dos assets

Todos os arquivos desta pasta foram **gerados pelo próprio projeto**, exceto a fonte, que tem licença livre.
Nada foi baixado de bancos de imagens ou de som, e nada imita um jogo existente.

## Sprites (`sprites/`): gerados no Blender

Modelos 3D feitos do zero, só com primitivas e materiais procedurais, por scripts Python (`bpy`) em
[`arte/blender/`](../arte/blender). As imagens são renderizações ortográficas com a luz fixa vinda de cima e da esquerda,
fundo transparente e alfa reto. Cada folha é uma grade de quadros, lida da esquerda para a direita e de cima para baixo.
A geometria de cada folha está em `sprites/manifest.json`.

Para regerar tudo (Blender 5.2.3 LTS; termina conferindo tamanhos, alfa, pivôs e orientação):

```bash
/Applications/Blender.app/Contents/MacOS/Blender -b --factory-startup -P arte/blender/render_all.py -- --engine cycles --samples 128
```

| Arquivo | Quadros | Script | Usado em |
|---|---|---|---|
| `sprites/nave.png` | 64 ângulos (8x8, 128 px) | `nave.py` + `nave_modelo.py` | nave na partida (4 direções), vidas do HUD, menu e fim |
| `sprites/explosao.png` | 16 (4x4, 128 px) | `explosao.py` | colisão, nave destruída e tela de derrota |
| `sprites/asteroide.png` | 32 (8x4, 96 px) | `asteroide.py` | asteroides da partida e menu |
| `sprites/inimigo.png` | 8 (8x1, 96 px) | `inimigo.py` (criado na Fase 4) | inimigos da partida |
| `sprites/astronauta.png` | 8 (8x1, 48x64) | `astronauta.py` + `pessoas.py` | passageiro Astronauta |
| `sprites/engenheiro.png` | 8 (8x1, 48x64) | `engenheiro.py` + `pessoas.py` | passageiro Engenheiro |
| `sprites/professor.png` | 8 (8x1, 48x64) | `professor.py` + `pessoas.py` | passageiro Professor |
| `sprites/plataforma_base.png` | 1 (256x64) | `plataforma_base.py` + `cenario_modelo.py` | plataforma de pouso L |
| `sprites/fundo.png` | 1 (1200x900) | `fundo.py` | fundo de todas as telas |
| `sprites/icone.png` | 1 (256x256) | `icone.py` (+ `nave_modelo.py`, `chama.py`) | ícone da janela (Windows e Linux) |
| `sprites/chama.png` | 4 (4x1, 64x96) | `chama.py` | Fase 7: a chama do motor na pilotagem |
| `sprites/plataforma_passageiro.png` | 1 (160x48) | `plataforma_passageiro.py` | Fase 7: as plataformas dos passageiros na pilotagem |
| `sprites/rocha_tile.png` | 1 (256x256) | `rocha_tile.py` | Fase 7: as paredes da caverna na pilotagem (ladrilho repetido) |

Os scripts nasceram na prova de conceito da equipe (POC 2.5D, novembro de 2026) e foram trazidos para o
repositório na Fase 4. Mudanças em relação à POC: fundo em 1200x900 (proporção 4:3 da janela, em vez de 1920x1080),
saída em `assets/sprites/` e o novo `inimigo.py`.

### Skins da loja

O nível I da linha Padrão usa `assets/sprites/nave.png`. O nível I da linha Guará
usa a folha já renderizada em `assets/sprites/naves/guara.png`. Para renderizar somente
as sete evoluções da Parte 3, recompor os atlas e verificar o conjunto, execute na raiz:

```bash
/Applications/Blender.app/Contents/MacOS/Blender -b --factory-startup --python-exit-code 1 -P arte/blender/render_all.py -- --engine cycles --samples 128 --render-only --only padrao_2 padrao_3 padrao_4 padrao_5 guara_2 guara_3 guara_4
/Applications/Blender.app/Contents/MacOS/Blender -b --factory-startup --python-exit-code 1 -P arte/blender/render_all.py -- --atlas-only
/Applications/Blender.app/Contents/MacOS/Blender -b --factory-startup --python-exit-code 1 -P arte/blender/verify.py
```

| Arquivo (a partir da raiz) | Origem |
|---|---|
| `assets/sprites/naves/guara.png` | `arte/blender/naves/guara_modelo.py` (64 ângulos). |
| `assets/sprites/naves/padrao_2.png` | `arte/blender/naves/padrao_2_modelo.py` (64 ângulos). |
| `assets/sprites/naves/padrao_3.png` | `arte/blender/naves/padrao_3_modelo.py` (64 ângulos). |
| `assets/sprites/naves/padrao_4.png` | `arte/blender/naves/padrao_4_modelo.py` (64 ângulos). |
| `assets/sprites/naves/padrao_5.png` | `arte/blender/naves/padrao_5_modelo.py` (64 ângulos). |
| `assets/sprites/naves/guara_2.png` | `arte/blender/naves/guara_2_modelo.py` (64 ângulos). |
| `assets/sprites/naves/guara_3.png` | `arte/blender/naves/guara_3_modelo.py` (64 ângulos). |
| `assets/sprites/naves/guara_4.png` | `arte/blender/naves/guara_4_modelo.py` (64 ângulos). |
| `assets/sprites/naves/catalogo.json` | `arte/blender/naves/atlas.py` grava o catálogo ordenado de `arte/blender/specs.py`. |
| `assets/sprites/naves/previas.png` | `arte/blender/naves/atlas.py` copia o quadro 0 de cada folha em células de 128 px e oito colunas. |
| `assets/sprites/naves/sombras.png` | `arte/blender/naves/atlas.py` reduz por média 2×2 os quadros 0, 4, …, 60 em uma linha por skin. |

A primeira posição dos dois atlas vem de `assets/sprites/nave.png`. As demais seguem a ordem do catálogo.

Bruma, Vereda, Cantil, Estiva, Escuta, Fole e Luzeiro foram retiradas do catálogo ativo e suas
folhas foram removidas. Os modelos, descrições e paletas estão em
[`arte/blender/naves/futuras/`](../arte/blender/naves/futuras/) para uso como bases de futuras linhas.
O manifesto mantém as entradas históricas dessas folhas. O catálogo define quais folhas entram nos atlas.

## Sons (`sons/`): sintetizados por script

Gerados por [`arte/sons/gerar_sons.py`](../arte/sons/gerar_sons.py), em Python sem dependências: senoides, ondas
quadradas e triangulares e ruído filtrado com semente fixa (WAV 16 bits, mono, 44,1 kHz). Rodar de novo produz
exatamente os mesmos bytes.

```bash
python3 arte/sons/gerar_sons.py
```

| Arquivo | Quando toca |
|---|---|
| `sons/mover.wav` | todo turno de movimento |
| `sons/embarque.wav` | passageiro embarcado |
| `sons/colisao.wav` | colisão (perdeu uma vida) |
| `sons/explosao.wav` | nave destruída |
| `sons/alerta.wav` | todos a bordo: volte à plataforma |
| `sons/invalido.wav` | ação sem efeito (sem passageiro aqui, nave cheia) |
| `sons/vitoria.wav` | tela de missão cumprida |
| `sons/derrota.wav` | tela de derrota |

## Fontes (`fontes/`): licença livre

| Arquivo | Origem | Licença |
|---|---|---|
| `fontes/ChakraPetch-Regular.ttf` | Chakra Petch (© 2018 The Chakra Petch Project Authors), baixada de `github.com/google/fonts` (`ofl/chakrapetch`, commit `e774067`, 2026-11-12). SHA-256 `98fcd638…f986` | SIL Open Font License 1.1 (`fontes/OFL-ChakraPetch.txt`) |
| `fontes/ChakraPetch-SemiBold.ttf` | mesma origem. SHA-256 `45264de3…734b` | SIL Open Font License 1.1 |

A OFL permite distribuir a fonte junto com o jogo, desde que o texto da licença acompanhe os arquivos.
