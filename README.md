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
| 5 — Servidor e ranking global (Spring Boot) | validada | `fase-5` | [tutorial](docs/fase-05-tutorial.md) · [API (OpenAPI)](docs/api/openapi.json) |
| 6 — Android | planejada | | |
| 7 — Evolução livre | planejada | | |

Decisões de arquitetura: [docs/decisoes-arquiteturais.md](docs/decisoes-arquiteturais.md).
Kanban: [GitHub Project](https://github.com/users/thebylito/projects/2).

## Executar (estado atual: Fase 5)

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

Controles na janela: na entrada, `1` entrar, `2` criar conta, `3` convidado; `1` a `6` no menu; na nova partida, digite o nome, setas `←`/`→` escolhem a dificuldade,
`↑`/`↓` o tamanho do mapa e `Enter` decola; na partida, `WASD` ou setas movem, `C` ou espaço embarcam, `Q` ou
`Esc` abortam, `M` liga e desliga o som. No terminal: `w/a/s/d` movem a nave, `c` embarca o passageiro da célula, `q` aborta a missão.

O ranking fica em `ranking.json`, na pasta de trabalho do jogo, e é o mesmo para a janela e o terminal: com
`./gradlew`, a pasta do projeto (outra pasta: `-PpastaDoJogo=/caminho`); com o script ou o jar, a pasta de onde
foram chamados. Opção `-Dmissaomarte.semente=N` (por exemplo, `./gradlew :lwjgl3:run -Dmissaomarte.semente=7`):
mesmo mapa e mesmos movimentos dos inimigos a cada execução.

### Servidor de ranking (Fase 5)

O servidor fica em [`servidor/`](servidor), um projeto Gradle separado, que não depende do código do jogo. Spring
Boot 4.1.2, H2 em arquivo e token JWT. Também precisa só de um JDK 17 instalado: o arquivo
`servidor/gradle/gradle-daemon-jvm.properties` faz o Gradle rodar nele, porque o plugin do Spring Boot 4 não aceita
um Gradle em Java mais antigo. Rode em outro terminal:

```bash
cd servidor
export MISSAOMARTE_JWT_SEGREDO="$(openssl rand -base64 48)"   # opcional em desenvolvimento (sem ele, um temporário)
./gradlew bootRun                                            # http://localhost:8080
./gradlew build                                              # 21 testes
```

```powershell
cd servidor; .\gradlew.bat bootRun
```

| Variável do servidor | Para quê | Padrão |
|---|---|---|
| `MISSAOMARTE_JWT_SEGREDO` | segredo do token (32 bytes ou mais); nunca versionar | temporário a cada início |
| `MISSAOMARTE_DADOS` | arquivo do banco H2 (sem `.mv.db`) | `servidor/dados/missaomarte` |
| `PORT` | porta HTTP | `8080` |

**Em contêiner** (Docker; não precisa de Java na máquina):

```bash
cd servidor
cp .env.exemplo .env          # preencha MISSAOMARTE_JWT_SEGREDO (openssl rand -base64 48); o .env não vai para o Git
docker compose up --build     # http://localhost:8080; os dados ficam no volume "dados"
docker compose up --build --watch   # modo dev: reconstrói sozinho quando src/ ou o build.gradle mudam
docker compose down           # para (docker compose down -v apaga também os dados)
```

A imagem (`servidor/Dockerfile`) compila com o wrapper num JDK 17 e roda só com o JRE 17, com um usuário sem
privilégios, o banco em `/dados` e verificação de saúde pelo `/actuator/health`.

Documentação interativa em `http://localhost:8080/swagger-ui.html` (contrato em [`docs/api/openapi.json`](docs/api/openapi.json)).
Saúde: `http://localhost:8080/actuator/health`.

No jogo, a primeira tela oferece **1 entrar**, **2 criar conta** ou **3 jogar como convidado**. O convidado joga sem
servidor, só com o ranking local, como nas fases anteriores. Com uma conta, cada partida vai para o histórico, e as
vitórias entram no ranking global (Top 10, a melhor de cada jogador). O endereço do servidor é configurável:
`./gradlew :lwjgl3:run -Dmissaomarte.servidor=http://outra-maquina:8080` (padrão `http://localhost:8080`). Com o
servidor desligado, o jogo avisa e continua.

Para ver uma versão anterior: `git checkout fase-0` (`javac -encoding UTF-8 -d out src/*.java` e `java -cp out Main`),
`git checkout fase-1` (`./build.sh run`), `git checkout fase-2` (`./gradlew run -q --console=plain`),
`git checkout fase-3` (a janela só com formas: `./gradlew :lwjgl3:run`) ou `git checkout fase-4` (sprites e sons, sem
servidor).

## Validação

Cada fase só é encerrada depois de provada (as evidências ficam em `docs/evidencias/fase-XX/`). As ferramentas em
[`validacao/`](validacao) recebem o comando do jogo como argumento e servem para todas as fases de console:

- `executar_roteiros.py`: entradas fixas comparadas com um gabarito (`validacao/gabarito/fase-XX`);
- `piloto_automatico.py`: joga partidas inteiras (vencer, colidir, esgotar os pontos) e confere as regras a cada turno;
- `comparar_com_semente.py`: joga as mesmas partidas, com a mesma semente, em duas versões e compara tudo;
- `janela/PilotoDaJanela.java`: joga na janela real sem teclado (as teclas entram pelo `InputProcessor`) e salva um
  print de cada tela a partir do OpenGL. Tem os roteiros `vitoria`, `colisao`, `online` e `offline` (Fase 5) e serve
  quando o sistema não deixa enviar teclas nem capturar a tela.
