# Missão Marte Unifor

Projeto integrador: o jogo de console **Missão Marte** (Exercício 10) evolui em fases até virar um jogo gráfico
multiplataforma, com servidor de ranking. Cada fase validada recebe uma tag `fase-N`. Cada sprint é um PR.

| Fase | Situação | Tag | Documentação |
|---|---|---|---|
| 0 — Projeto inicial (console) | validada | `fase-0` | [comportamento de referência](docs/fase-00-comportamento-referencia.md) |
| 1 — Refatoração arquitetural | validada | `fase-1` | [tutorial](docs/fase-01-tutorial.md) |
| 2 — Gradle | validada | `fase-2` | [tutorial](docs/fase-02-tutorial.md) |
| 3 — libGDX | planejada | | |
| 4 — Interface gráfica | planejada | | |
| 5 — Servidor e ranking global | planejada | | |
| 6 — Android | planejada | | |
| 7 — Evolução livre | planejada | | |

Decisões de arquitetura: [docs/decisoes-arquiteturais.md](docs/decisoes-arquiteturais.md).
Kanban: [GitHub Project](https://github.com/users/thebylito/projects/2).

## Executar (estado atual: Fase 2)

Requisito: **um JDK 17 instalado**. O Gradle o encontra sozinho (toolchain), mesmo que o `java` padrão seja outro.
Não é preciso instalar o Gradle: o wrapper (`gradlew`) baixa a versão certa (8.14.3).

```bash
./gradlew build                     # compila, roda os testes (JUnit 5) e gera o jar
./gradlew run -q --console=plain    # joga no terminal (-q esconde as mensagens do Gradle)
```

```powershell
.\gradlew.bat build
.\gradlew.bat run -q --console=plain
```

Outras formas: `./gradlew installDist` gera `build/install/missao-marte-unifor/bin/missao-marte-unifor`;
`java -jar build/libs/missao-marte-unifor-2.0.0.jar` (com Java 17 no PATH). Relatório dos testes:
`build/reports/tests/test/index.html`.

Controles: `w/a/s/d` movem a nave, `c` embarca o passageiro da célula, `q` aborta a missão.
O ranking fica em `ranking.json`, na pasta de trabalho do jogo: com `./gradlew run`, a pasta do projeto
(outra pasta: `-PpastaDoJogo=/caminho`); com o script ou o jar, a pasta de onde foram chamados.
Opção `-Dmissaomarte.semente=N` (por exemplo, `./gradlew run -q --console=plain -Dmissaomarte.semente=7`): mesmo mapa e
mesmos movimentos dos inimigos a cada execução.

Para ver uma versão anterior: `git checkout fase-0` (`javac -encoding UTF-8 -d out src/*.java` e `java -cp out Main`)
ou `git checkout fase-1` (`./build.sh run`).

## Validação

Cada fase só é encerrada depois de provada (as evidências ficam em `docs/evidencias/fase-XX/`). As ferramentas em
[`validacao/`](validacao) recebem o comando do jogo como argumento e servem para todas as fases de console:

- `executar_roteiros.py`: entradas fixas comparadas com um gabarito (`validacao/gabarito/fase-XX`);
- `piloto_automatico.py`: joga partidas inteiras (vencer, colidir, esgotar os pontos) e confere as regras a cada turno;
- `comparar_com_semente.py`: joga as mesmas partidas, com a mesma semente, em duas versões e compara tudo.
