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
| 6 — Android e toque | validada | `fase-6` | [tutorial](docs/fase-06-tutorial.md) · [publicação do servidor](servidor/PUBLICACAO.md) |
| 7 — Evolução livre: Modo Pilotagem | validada | `fase-7` | [análise](docs/fase-07-analise.md) · [tutorial](docs/fase-07-tutorial.md) |

**No ar:** https://missao-marte.thebylito.dev: ranking global ao vivo, downloads do APK e do jogo desktop (já
apontando para o servidor) e a API. Publicado no Dokploy pelo GitHub Actions a cada merge na `main`
([como funciona](servidor/PUBLICACAO.md#10-publicação-no-dokploy-com-github-actions)).

Decisões de arquitetura: [docs/decisoes-arquiteturais.md](docs/decisoes-arquiteturais.md).
Kanban: [GitHub Project](https://github.com/users/thebylito/projects/2).

Entrega final (Sprint 5): [relatório técnico](docs/relatorio-tecnico.md) ([PDF](docs/relatorio-tecnico.pdf)) ·
[roteiro da demonstração](docs/demonstracao.md) · [retrospectiva](docs/retrospectiva.md) ·
[revisão final](docs/evidencias/sprint-5/revisao-final.txt).

## Executar (estado atual: Fase 7)

Requisito: **um JDK 17 instalado**. O Gradle o encontra sozinho (toolchain), mesmo que o `java` padrão seja outro.
Não é preciso instalar o Gradle: o wrapper (`gradlew`) baixa a versão certa (8.14.3).

O projeto tem cinco módulos: `nucleo` (as regras, sem libGDX), `console` (o jogo de texto), `core` (o jogo
gráfico com libGDX 1.14.2), `lwjgl3` (a janela desktop) e `android` (o app para celular, que só entra no build
quando há um Android SDK; veja abaixo). Os sprites, sons e fontes ficam em `assets/` (origem em
[`assets/ORIGEM.md`](assets/ORIGEM.md)) e são gerados pelos scripts de [`arte/`](arte): Blender para os sprites e
Python para os sons. Para compilar e jogar não é preciso ter o Blender.

```bash
./gradlew build                              # compila os módulos e roda os testes (JUnit 5); com SDK, gera o APK
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

Controles na janela: na entrada, `1` entrar, `2` criar conta, `3` convidado, `4` endereço do servidor; `1` a `6` no menu; na nova partida, digite o nome, setas `←`/`→` escolhem a dificuldade,
`↑`/`↓` o tamanho do mapa e `Enter` decola; na partida, `WASD` ou setas movem, `C` ou espaço embarcam, `Q` ou
`Esc` abortam, `M` liga e desliga o som. No terminal: `w/a/s/d` movem a nave, `c` embarca o passageiro da célula, `q` aborta a missão.
Para testar os controles de toque com o mouse: `./gradlew :lwjgl3:run -Dmissaomarte.toque=true`.

### Modo Pilotagem (Fase 7)

A nova missão começa no modo **Pilotagem** (o padrão desde 2026-11-25). `Tab` (ou o botão "Trocar") muda para o
**Clássico**, a grade por turnos de sempre. Na **Pilotagem**:
- **Como se joga:** você pilota a nave numa caverna marciana, com gravidade, combustível e pouso. Pouse devagar e reto
  na plataforma de um passageiro, espere o embarque (cabem 2) e leve-os à base, que também reabastece.
- **Teclado:** `←`/`→` (ou `A`/`D`) giram, `↑` (ou `W`, Espaço) liga o motor, `Esc` aborta.
- **Toque:** ◄ ► à esquerda e MOTOR à direita, que valem enquanto apertados (dá para usar dois dedos), e Abortar no
  alto.
- **Pontos:** os do clássico por passageiro (20, 15 e 10) mais um bônus pelo combustível que sobrou (até 30).
- **Ranking:** cada modo tem o seu Top 5 local (`ranking.json` e `ranking-pilotagem.json`) e o seu Top 10 global. Na
  tela de ranking, `↑`/`↓` trocam o modo.
- **Modo shadow:** com login e o servidor no ar, você vê em tempo real as naves dos outros pilotos conectados,
  translúcidas e com o nome (sem colisão: cada um joga a sua partida). Para ver uma sombra com um aparelho só:
  `python3 validacao/sombras/piloto_fantasma.py --segundos 60`.

O console continua só com o modo clássico.

O ranking fica em `ranking.json`, na pasta de trabalho do jogo, e é o mesmo para a janela e o terminal: com
`./gradlew`, a pasta do projeto (outra pasta: `-PpastaDoJogo=/caminho`); com o script ou o jar, a pasta de onde
foram chamados. Opção `-Dmissaomarte.semente=N` (por exemplo, `./gradlew :lwjgl3:run -Dmissaomarte.semente=7`):
mesmo mapa e mesmos movimentos dos inimigos a cada execução.

### Android (Fase 6)

O mesmo jogo no celular, com layout para retrato e paisagem e controles de toque (direcional, Embarcar, Som e
Abortar). Na partida, o botão Voltar do Android não aborta; nas outras telas, ele volta. Requisitos para gerar o
APK: o Android SDK com a plataforma 36 e o Platform-Tools (o Android Studio instala os dois), e `ANDROID_HOME`
apontando para ele (ou `sdk.dir=...` num `local.properties` na raiz, que o Git ignora). Sem SDK, o módulo fica de
fora com um aviso, e o resto do build não muda.

```bash
./gradlew :android:assembleDebug    # android/build/outputs/apk/debug/android-debug.apk
./gradlew :android:installDebug     # instala no emulador ou no celular (depuração USB ligada)
```

Android 8.0 (API 26) ou mais novo. No emulador, o servidor do computador já é o padrão (`http://10.0.2.2:8080`).
Num celular na mesma rede Wi-Fi, toque em **4. Servidor** na tela de entrada e digite
`http://<IP do computador>:8080`; o endereço fica guardado no aparelho. Detalhes e limitações no
[tutorial da Fase 6](docs/fase-06-tutorial.md#11-limitações-de-dispositivo).

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
| `MISSAOMARTE_URL_PUBLICA` | endereço público (Swagger), só na publicação | `http://localhost:8080` |

Fase 7: a API ganhou o modo de jogo. O campo `modo` (`CLASSICO` ou `PILOTAGEM`) é opcional em `POST /api/partidas`
(sem ele, clássico) e vem no histórico; o ranking aceita `GET /api/ranking?modo=PILOTAGEM` (sem o parâmetro, o
clássico). Os clientes das Fases 5 e 6 continuam funcionando, e as partidas já gravadas contam como clássicas.
O modo shadow usa o WebSocket `ws://<servidor>/ws/sombras` (com `https://`, `wss://`), com o mesmo token no cabeçalho
`Authorization`.

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

Para publicar (variáveis, saúde, volume, cópia de segurança e HTTPS por proxy): [`servidor/PUBLICACAO.md`](servidor/PUBLICACAO.md).

Documentação interativa em `http://localhost:8080/swagger-ui.html` (contrato em [`docs/api/openapi.json`](docs/api/openapi.json)).
Saúde: `http://localhost:8080/actuator/health`.

No jogo, a primeira tela oferece **1 entrar**, **2 criar conta** ou **3 jogar como convidado**. O convidado joga sem
servidor, só com o ranking local, como nas fases anteriores. Com uma conta, cada partida vai para o histórico, e as
vitórias entram no ranking global (Top 10, a melhor de cada jogador). O endereço do servidor é configurável na
opção **4** da entrada (fica guardado) ou na linha de comando, que tem prioridade:
`./gradlew :lwjgl3:run -Dmissaomarte.servidor=http://outra-maquina:8080` (padrão `http://localhost:8080`). Com o
servidor desligado, o jogo avisa e continua.

Para ver uma versão anterior: `git checkout fase-0` (`javac -encoding UTF-8 -d out src/*.java` e `java -cp out Main`),
`git checkout fase-1` (`./build.sh run`), `git checkout fase-2` (`./gradlew run -q --console=plain`),
`git checkout fase-3` (a janela só com formas: `./gradlew :lwjgl3:run`), `git checkout fase-4` (sprites e sons, sem
servidor), `git checkout fase-5` (servidor e ranking global, só desktop) ou `git checkout fase-6` (Android e toque, só
o modo clássico).

## Validação

Cada fase só é encerrada depois de provada (as evidências ficam em `docs/evidencias/fase-XX/`). As ferramentas em
[`validacao/`](validacao) recebem o comando do jogo como argumento e servem para todas as fases de console:

- `executar_roteiros.py`: entradas fixas comparadas com um gabarito (`validacao/gabarito/fase-XX`);
- `piloto_automatico.py`: joga partidas inteiras (vencer, colidir, esgotar os pontos) e confere as regras a cada turno;
- `comparar_com_semente.py`: joga as mesmas partidas, com a mesma semente, em duas versões e compara tudo;
- `janela/PilotoDaJanela.java`: joga na janela real sem teclado (as teclas entram pelo `InputProcessor`) e salva um
  print de cada tela a partir do OpenGL. Tem os roteiros `vitoria`, `colisao`, `online` e `offline` (Fase 5) e
  `toque` (Fase 6, cliques nos botões de toque) e `pilotagem` (Fase 7, o piloto automático voando a caverna) e serve
  quando o sistema não deixa enviar teclas nem capturar a tela;
- `android/pilotar_android.py` (Fase 6): joga no emulador ou num celular pelo `adb` (toques, caixas de texto,
  rotação, segundo plano) e salva prints e o log; o roteiro `pilotagem` (Fase 7) usa toques longos;
- `android/conferir_api_minima.py` (Fase 6): confere no APK pronto se o código do jogo só chama APIs que existem
  no `minSdk` (o lint do Android não analisa `core` nem `nucleo`);
- `sombras/piloto_fantasma.py` (Fase 7): um segundo piloto do modo shadow só pela rede, que voa perto da base e
  registra as sombras que recebe.

## Não verificado

O que não foi possível verificar, com o motivo e a evidência alternativa:

- **Windows e Linux:** o desenvolvimento foi todo no macOS (arm64). Os comandos `.\gradlew.bat` estão no README, mas
  não foram executados.
- **Android abaixo da API 34 e tablets de verdade:** só havia o emulador (API 36) e um celular (API 37). A API mínima
  26 foi conferida no bytecode do APK (`validacao/android/conferir_api_minima.py`), e o layout 4:3, em teste de unidade.
- **APK de release assinado:** só o APK de depuração é gerado. O `android-release-unsigned.apk` não instala.
- **Servidor publicado com HTTPS:** testado só em contêiner local. O guia está em `servidor/PUBLICACAO.md`.
- **Imagem do servidor em amd64 e o `docker compose up --watch` na prática.**
- **Partida do Modo Pilotagem jogada à mão no celular chegando ao ranking global (CT-32):** registrada pelo relato
  do integrante (`docs/evidencias/fase-07/partida-a-mao-celular.txt`). A gravação no ranking da pilotagem está
  coberta pelos testes do `core` e pelo roteiro `pilotagem` na janela real.
