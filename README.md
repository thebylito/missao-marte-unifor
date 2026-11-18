# Missão Marte Unifor

Projeto integrador: o jogo de console **Missão Marte** (Exercício 10) evolui em fases até virar um jogo gráfico
multiplataforma, com servidor de ranking. Cada fase validada recebe uma tag `fase-N`. Cada sprint é um PR.

| Fase | Situação | Tag | Documentação |
|---|---|---|---|
| 0 — Projeto inicial (console) | validada | `fase-0` | [comportamento de referência](docs/fase-00-comportamento-referencia.md) |
| 1 — Refatoração arquitetural | validada | `fase-1` | [tutorial](docs/fase-01-tutorial.md) |
| 2 — Gradle | validada | `fase-2` | [tutorial](docs/fase-02-tutorial.md) |
| 3 — libGDX (janela desktop) | validada | `fase-3` | [tutorial](docs/fase-03-tutorial.md) |
| 4 — Interface gráfica (sprites, sons, HUD) | validada | `fase-4` | [tutorial](docs/fase-04-tutorial.md) |
| 5 — Servidor e ranking global | planejada | | |
| 6 — Android | planejada | | |
| 7 — Evolução livre | planejada | | |

Decisões de arquitetura: [docs/decisoes-arquiteturais.md](docs/decisoes-arquiteturais.md).
Kanban: [GitHub Project](https://github.com/users/thebylito/projects/2).

## Executar (estado atual: Fase 4)

Requisito: **um JDK 17 instalado**. O Gradle o encontra sozinho (toolchain), mesmo que o `java` padrão seja outro.
Não é preciso instalar o Gradle: o wrapper (`gradlew`) baixa a versão certa (8.14.3).

O projeto tem quatro módulos: `nucleo` (as regras, sem libGDX), `console` (o jogo de texto), `core` (o jogo
gráfico com libGDX 1.14.2) e `lwjgl3` (a janela desktop). Os sprites, sons e fontes ficam em `assets/` (origem em
[`assets/ORIGEM.md`](assets/ORIGEM.md)) e são gerados pelos scripts de [`arte/`](arte): Blender para os sprites e
Python para os sons. Para compilar e jogar não é preciso ter o Blender.

```bash
./gradlew build                              # compila os 4 módulos e roda os testes (JUnit 5)
./gradlew :lwjgl3:run                        # joga na janela
./gradlew :console:run -q --console=plain    # joga no terminal (-q esconde as mensagens do Gradle)
```

```powershell
.\gradlew.bat build
.\gradlew.bat :lwjgl3:run
.\gradlew.bat :console:run -q --console=plain
```

Use sempre o nome do módulo (`:lwjgl3:run` ou `:console:run`): um `./gradlew run` sem módulo executaria os dois.

Outras formas: `./gradlew installDist` gera `lwjgl3/build/install/missao-marte/bin/missao-marte` (janela) e
`console/build/install/missao-marte-console/bin/missao-marte-console` (terminal). Os jars ficam na pasta `lib` ao
lado e rodam com `java -jar` (Java 17 no PATH). No macOS a janela precisa de `-XstartOnFirstThread`: o
`./gradlew :lwjgl3:run` já passa essa opção, e o script e o jar reiniciam a JVM com ela sozinhos. Relatório dos
testes: `<módulo>/build/reports/tests/test/index.html`.

Controles na janela: `1` a `4` no menu; na nova partida, digite o nome, setas `←`/`→` escolhem a dificuldade,
`↑`/`↓` o tamanho do mapa e `Enter` decola; na partida, `WASD` ou setas movem, `C` ou espaço embarcam, `Q` ou
`Esc` abortam, `M` liga e desliga o som. No terminal: `w/a/s/d` movem a nave, `c` embarca o passageiro da célula, `q` aborta a missão.

O ranking fica em `ranking.json`, na pasta de trabalho do jogo, e é o mesmo para a janela e o terminal: com
`./gradlew`, a pasta do projeto (outra pasta: `-PpastaDoJogo=/caminho`); com o script ou o jar, a pasta de onde
foram chamados. Opção `-Dmissaomarte.semente=N` (por exemplo, `./gradlew :lwjgl3:run -Dmissaomarte.semente=7`):
mesmo mapa e mesmos movimentos dos inimigos a cada execução.

Para ver uma versão anterior: `git checkout fase-0` (`javac -encoding UTF-8 -d out src/*.java` e `java -cp out Main`),
`git checkout fase-1` (`./build.sh run`), `git checkout fase-2` (`./gradlew run -q --console=plain`) ou
`git checkout fase-3` (a janela só com formas: `./gradlew :lwjgl3:run`).

## Validação

Cada fase só é encerrada depois de provada (as evidências ficam em `docs/evidencias/fase-XX/`). As ferramentas em
[`validacao/`](validacao) recebem o comando do jogo como argumento e servem para todas as fases de console:

- `executar_roteiros.py`: entradas fixas comparadas com um gabarito (`validacao/gabarito/fase-XX`);
- `piloto_automatico.py`: joga partidas inteiras (vencer, colidir, esgotar os pontos) e confere as regras a cada turno;
- `comparar_com_semente.py`: joga as mesmas partidas, com a mesma semente, em duas versões e compara tudo;
- `janela/PilotoDaJanela.java`: joga na janela real sem teclado (as teclas entram pelo `InputProcessor`) e salva um
  print de cada tela a partir do OpenGL. Tem os roteiros `vitoria` e `colisao` e serve quando o sistema não deixa
  enviar teclas nem capturar a tela.
