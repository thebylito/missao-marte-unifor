# Arte do Capítulo 1

Gerada em 27/11/2026 com Blender 5.2.3 LTS, Cycles CPU, 64 amostras. Todas as execuções usaram `--background`; nenhuma janela do Blender foi aberta. Os modelos são geometria procedural original.

## Resultado

| Folha | Célula | Grade | Quadros | Pivô (origem superior esquerda) |
|---|---|---|---|---|
| `sentinela.png` | 128×128 | 4×4 | 16 | 64,118 |
| `antena_missao.png` | 256×192 | 4×3 | 12 | 128,178 |

A torre tem coluna e sapatas fixas, com cabeça blindada e lente âmbar que varrem os dois lados. A antena tem refletor côncavo, tripé, foco, dipolo e módulos de energia próprios. Seus quadros 0–3 mostram o estado desligado, 4–7 a orientação durante ativação e 8–11 a transmissão verde.

Para o desenho em coordenadas de mundo com Y crescente para cima: `drawX = x - pivotX`, `drawY = y - (cellH - pivotY)`. Assim, a margem abaixo do apoio é de 10 px na sentinela e 14 px na antena.

## Verificação realizada

- `arte/blender/verify.py`: 79 resultados aprovados, zero erros, incluindo as 76 folhas e os três artefatos de catálogo/atlas. Saída integral em [verificacao.log](verificacao.log).
- [verificar_arte.py](verificar_arte.py): bordas completamente transparentes em todos os quadros, presença de antisserrilha, 16 e 12 quadros distintos, nenhuma mudança da silhueta da base. Dados em [geometria.json](geometria.json).
- A sentinela ocupa X=27…100, Y=17…117, variando apenas o topo por animação. A antena ocupa X=38…217, Y=24…177. Ambos têm folga em todas as bordas.
- Os indicadores da antena são medidos em uma região fixa do mastro: baixa intensidade desligada, predominância vermelha/âmbar na ativação e predominância verde na transmissão.
- As duas folhas foram inspecionadas visualmente pelo agente em imagem: silhuetas distintas, corpo e apoio visíveis, leitura dos estados e ausência de recorte. Isso não equivale a aprovação humana nem substitui a inspeção durante a partida.

## Reproduzir

Na raiz do projeto:

```bash
rtk proxy /Applications/Blender.app/Contents/MacOS/Blender --background --python-exit-code 1 --python arte/blender/render_all.py -- --only sentinela antena_missao --engine cycles --samples 64
rtk proxy /Applications/Blender.app/Contents/MacOS/Blender --background --python-exit-code 1 --python arte/blender/verify.py
rtk proxy /Applications/Blender.app/Contents/MacOS/Blender --background --python-exit-code 1 --python docs/evidencias/historia/capitulo-1/arte/verificar_arte.py
```

[previa.html](previa.html) oferece uma animação local de cada folha, controles dos três estados da antena e linha do contato com o chão. Nenhuma dependência ou recurso remoto é necessário. O ciclo de ativação termina no último quadro; os estados desligado e ativo repetem seus quatro quadros.

O relatório de renderização corrente está em `arte/blender/render_report.json`; o manifesto e a origem foram atualizados. A validação de integração e jogabilidade pertence às evidências gerais do capítulo.
