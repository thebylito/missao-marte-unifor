# Missão Marte Unifor

Projeto integrador: o jogo de console **Missão Marte** (Exercício 10) evolui em fases até virar um jogo gráfico
multiplataforma, com servidor de ranking. Cada fase validada recebe uma tag `fase-N`. Cada sprint é um PR.

| Fase | Situação | Tag | Documentação |
|---|---|---|---|
| 0 — Projeto inicial (console) | validada | `fase-0` | [comportamento de referência](docs/fase-00-comportamento-referencia.md) |
| 1 — Refatoração arquitetural | validada | `fase-1` | [tutorial](docs/fase-01-tutorial.md) |
| 2 — Gradle | planejada | | |
| 3 — libGDX | planejada | | |
| 4 — Interface gráfica | planejada | | |
| 5 — Servidor e ranking global | planejada | | |
| 6 — Android | planejada | | |
| 7 — Evolução livre | planejada | | |

Decisões de arquitetura: [docs/decisoes-arquiteturais.md](docs/decisoes-arquiteturais.md).
Kanban: [GitHub Project](https://github.com/users/thebylito/projects/2).

## Executar (estado atual: Fase 1)

Requisito: **JDK 17**. Se `java -version` mostrar outra versão, aponte `JAVA_HOME` para o JDK 17.

```bash
./build.sh          # compila e roda os testes
./build.sh run      # compila, testa e inicia o jogo
```

```powershell
.\build.ps1
.\build.ps1 -Run
```

Controles: `w/a/s/d` movem a nave, `c` embarca o passageiro da célula, `q` aborta a missão.
O ranking fica em `ranking.json`, no diretório de onde o jogo é iniciado.
Opção `-Dmissaomarte.semente=N` (por exemplo, `java -Dmissaomarte.semente=7 -cp build/classes br.edu.unifor.missaomarte.Main`):
mesmo mapa e mesmos movimentos dos inimigos a cada execução.

Para ver uma versão anterior: `git checkout fase-0` (compilar com `javac -encoding UTF-8 -d out src/*.java`
e executar com `java -cp out Main`).

## Validação

Cada fase só é encerrada depois de provada (as evidências ficam em `docs/evidencias/fase-XX/`). As ferramentas em
[`validacao/`](validacao) recebem o comando do jogo como argumento e servem para todas as fases de console:

- `executar_roteiros.py`: entradas fixas comparadas com um gabarito (`validacao/gabarito/fase-XX`);
- `piloto_automatico.py`: joga partidas inteiras (vencer, colidir, esgotar os pontos) e confere as regras a cada turno;
- `comparar_com_semente.py`: joga as mesmas partidas, com a mesma semente, em duas versões e compara tudo.
