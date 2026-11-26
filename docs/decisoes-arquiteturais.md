# Registro de decisões arquiteturais

Cada decisão tem contexto, decisão e consequências. Uma decisão nova pode substituir uma antiga, mas a antiga
continua aqui, marcada como substituída.

## DA-01 — Evoluir no próprio lugar, com uma tag por fase e um PR por sprint (2026-11-04)

**Contexto.** O enunciado pede o código organizado por fase e o histórico preservado. Uma pasta por fase
duplicaria o projeto inteiro a cada etapa.
**Decisão.** O código evolui na raiz do repositório. Ao final de cada fase validada há uma tag `fase-N`. Cada
sprint tem uma branch `sprint-N` e um único PR para a `main`. As branches não são apagadas.
**Consequências.** `git checkout fase-0` mostra o projeto inicial intacto. `git diff fase-0 fase-1` mostra
exatamente o que a refatoração mudou. O enunciado pede cópias separadas só "quando a atividade solicitar uma
nova pasta"; até aqui, nenhuma fase pediu.

## DA-02 — A Fase 0 é o projeto inicial do professor, sem alterações (2026-11-04)

**Contexto.** O enunciado manda executar, entender e refatorar o projeto inicial, mas proíbe copiar as
soluções de referência.
**Decisão.** A tag `fase-0` contém o projeto inicial exatamente como foi fornecido. As Fases 1 em diante são
escritas pela equipe. As soluções de referência não são consultadas.
**Consequências.** Existe uma linha de base objetiva para provar a preservação do comportamento.

## DA-03 — Camadas domínio / aplicação / infraestrutura / console (Fase 1)

**Contexto.** O `Main` original concentra sete responsabilidades. As Fases 3 a 6 vão trocar a interface
(libGDX, Android) e o ranking (servidor).
**Decisão.** Quatro pacotes com dependências só para dentro. O domínio não conhece console, arquivo nem relógio.
A `Partida` devolve eventos em vez de imprimir.
**Consequências.** A mesma `Partida` vai servir ao console e à tela gráfica. As regras são testáveis sem
teclado. Custo: mais arquivos (31 contra 10), cada um pequeno.

## DA-04 — Sorteio, relógio e ranking injetados (Fase 1)

**Contexto.** Com `new Random()` e `System.currentTimeMillis()` no meio da lógica, nenhuma partida se repete,
e testar fica impossível.
**Decisão.** `GeradorDeMissao(Random)`, `Partida(..., Relogio)` e `ServicoRanking(RepositorioRanking, Relogio)`,
todos montados no `Main`. Opção `-Dmissaomarte.semente=N`.
**Consequências.** Testes determinísticos e a prova de equivalência por semente (200 partidas idênticas à Fase 0).

## DA-05 — Refatorar preservando tudo e só depois corrigir bugs, em commits separados (Fase 1)

**Contexto.** Misturar refatoração e correção torna impossível dizer se uma diferença é bug novo ou correção.
**Decisão.** O primeiro commit da Fase 1 reproduz a Fase 0 byte a byte, incluindo os bugs. Cada correção vem
depois, com um teste que falha no código antigo e a diferença no gabarito explicada.
**Consequências.** `validacao/gabarito/fase-00` e `fase-01` documentam exatamente o que mudou de propósito.

## DA-06 — Executor de testes próprio na Fase 1; JUnit 5 na Fase 2 (Fase 1)

**Contexto.** Sem gerenciador de dependências, usar o JUnit exigiria baixar jars à mão.
**Decisão.** Anotação `@Teste`, classe `Verifica` e `ExecutarTestes`, cerca de 150 linhas em `test/.../teste/`.
**Consequências.** A Fase 1 já tem 65 testes. A Fase 2 troca o executor pelo JUnit 5 via Gradle, com uma
mudança mecânica (`@Teste` → `@Test`).

## DA-07 — Gradle 8.14.3 com DSL Groovy, toolchain Java 17 e JUnit 5.10.2 (Fase 2)

**Contexto.** O referencial teórico usa Gradle 8.7 e Groovy. A máquina de desenvolvimento tem Java 11 como
padrão e JDK 17 instalado. O libGDX da Fase 3 (1.13.1) já foi usado nesta máquina com Gradle 8.14.3.
**Decisão.** Wrapper do Gradle 8.14.3 (distribuição `bin`) versionado, DSL Groovy como no referencial, toolchain
`JavaLanguageVersion.of(17)` e JUnit 5.10.2 por BOM. `build.sh`, `build.ps1` e o executor de testes próprio foram removidos.
**Consequências.** Um único comando (`./gradlew build`) em qualquer máquina com um JDK 17 instalado, provado num
clone limpo com cache vazio e Java 11 como padrão. A divergência em relação ao referencial (8.7 → 8.14.3) fica
registrada aqui e no tutorial da Fase 2. Para o Gradle 9, a sintaxe do build já está sem avisos de obsoleto.

## DA-08 — Quatro módulos: nucleo, console, core e lwjgl3 (Fase 3)

**Contexto.** O referencial usa dois módulos (`core` com o jogo e `lwjgl3` com a janela). No nosso projeto as
regras já existem e não dependem de interface (DA-03), e o enunciado exige que o domínio não dependa da libGDX e
que o comportamento anterior continue provado.
**Decisão.** `nucleo` (domínio, aplicação e infraestrutura, Java puro), `console` (o jogo de texto),
`core` (o cliente libGDX compartilhado: `Game`, telas e controladores) e `lwjgl3` (só o launcher desktop). Só
`core` e `lwjgl3` declaram a libGDX. O primeiro commit da fase apenas moveu arquivos (`git mv`).
**Consequências.** "O domínio não depende da libGDX" vira erro de compilação, e não só convenção (prova em
`docs/evidencias/fase-03/dependencia-proibida.txt`). O console continua jogável e validável pelos mesmos roteiros e
pela comparação por semente. O módulo Android da Fase 6 reaproveita o `core` como o `lwjgl3` faz. Custo: dois
módulos a mais que no referencial.

## DA-09 — libGDX 1.14.2 e reinício automático da JVM no macOS (Fase 3)

**Contexto.** O referencial usa a libGDX 1.12.1. A versão estável mais nova no Maven Central é a 1.14.2
(backend LWJGL 3.3.3), e a regra prática do próprio referencial ("Qual versão usar?") é preferir uma versão
estável recente. No macOS, a janela (GLFW) precisa rodar na primeira thread do processo, o que exige a opção
`-XstartOnFirstThread`.
**Decisão.** libGDX 1.14.2, com a versão declarada uma única vez no `build.gradle` raiz (`gdxVersion`). A tarefa
`run` passa `-XstartOnFirstThread` no macOS. Fora do Gradle (script do `installDist`, `java -jar`), o
`ReinicioNoMacOS` reinicia a JVM com essa opção, a mesma ideia do `StartupHelper` dos projetos gerados pela libGDX.
**Consequências.** `./gradlew :lwjgl3:run`, o script e o jar abrem a janela sem configuração manual. As classes
usadas (`Game`, `Screen`, `ShapeRenderer`, `FitViewport`, `InputAdapter`) são as mesmas do referencial. Para
alinhar ao referencial, basta trocar `gdxVersion`.

## DA-10 — Telas passivas, controladores testáveis e troca de tela que libera a anterior (Fase 3)

**Contexto.** As decisões de cada tela (que tecla vira qual comando, quando a partida acabou, quando gravar o
ranking, a confirmação antes de apagar) precisam de testes. Uma `Screen` da libGDX só roda com janela e OpenGL.
Além disso, `Game.setScreen` chama `hide()` na tela antiga, mas nunca `dispose()`.
**Decisão.** Padrão Humble Object: `ControladorDaPartida`, `ControladorDoMenu`, `FormularioNovaPartida`,
`MapaDeTeclas`, `QuadroDaPartida`, `GeometriaDoMapa` e `TextosDaInterface` ficam no pacote `apresentacao`, sem
OpenGL. As telas (`telas`) só repassam teclas e desenham o que eles expõem. `MissaoMarteGame.trocarTela` chama
`dispose()` na tela anterior. Nesta fase o desenho usa só formas (`ShapeRenderer`) e a fonte padrão. Sprites, sons e
a fonte própria ficam para a Fase 4.
**Consequências.** 39 testes do `core` rodam sem janela, e 7 defeitos inseridos de propósito foram todos
detectados. A janela real foi validada de ponta a ponta pelo `validacao/janela/PilotoDaJanela.java`. Limite da
apresentação: na janela, o mapa vai de 2 a 10. Com tamanho 1, o Médio e o Difícil não cabem no mapa e o sorteio
nunca termina (problema herdado do console, que continua igual).

## DA-11 — Assets gerados pelo próprio projeto, com a origem registrada e testada (Fase 4)

**Contexto.** A issue #6 pede que os assets tenham origem documentada ou sejam gerados pelo projeto. O referencial
cria texturas e sons por código (`Pixmap` e PCM). A prova de conceito da equipe já tinha um pipeline Blender (`bpy`)
com modelos inéditos, aprovado em 04/11.
**Decisão.** Sprites renderizados por scripts Blender versionados em `arte/blender/`, incluindo o inimigo, criado
nesta fase. Sons sintetizados por `arte/sons/gerar_sons.py` (Python sem dependências, semente fixa). Fonte Chakra
Petch com licença OFL, guardada junto com o texto da licença. `assets/ORIGEM.md` registra cada arquivo, e o
`CatalogoDeAssetsTeste` falha se algum arquivo do catálogo não estiver citado ali, ou se o tamanho de uma folha
divergir do código ou do `manifest.json`.
**Consequências.** Qualquer integrante regenera tudo: os sprites em cerca de 11 s, com o Blender 5.2.3, e os sons de
forma idêntica, byte a byte. São cerca de 2,8 MB de assets no repositório. A chama, a plataforma de passageiro e a
rocha ficam prontas para a Fase 7. O Blender só é necessário para alterar a arte, não para compilar.

## DA-12 — AssetManager atrás de uma fachada, com tela de carregamento (Fase 4)

**Contexto.** Texturas, sons e fontes ocupam memória de vídeo e de áudio. Precisam ser carregados depois do
`create()` e liberados exatamente uma vez. Na Fase 3 cada tela só usava os recursos do jogo, e nada era carregado
de arquivo.
**Decisão.** A classe `Recursos` (padrão Facade) concentra o `AssetManager`: põe tudo na fila no `create()`, a
`TelaCarregamento` chama `update()` a cada quadro e mostra o progresso, as folhas são recortadas em `TextureRegion`
uma vez (padrão Flyweight), e o `dispose()` do jogo libera tudo. As telas não criam recursos. Mipmaps só nas texturas
com lados potência de 2, porque o OpenGL ES 2 do Android (Fase 6) não gera mipmaps para as outras. As fontes FreeType
usam páginas próprias com 8 px entre as letras, porque com 1 px os mipmaps faziam a borda de uma letra vazar para a
vizinha. Essas páginas também são liberadas no `dispose()`.
**Consequências.** O log mostra `carregamento concluído: 21 assets` e, ao sair, `21 assets liberados; restam 0`. Ganho
de testabilidade: a geometria das folhas (`Folha`, `FolhaDeSprites`) e o catálogo são Java puro e testados sem
OpenGL.

## DA-13 — Efeitos e animação só na apresentação, dirigidos pelos eventos do domínio (Fase 4)

**Contexto.** O referencial detecta colisões na tela (`Intersector`), num jogo de tiro em tempo real. Aqui o jogo é
por turnos, e quem decide colisão, embarque e fim é a `Partida`. A issue #6 pede que "colisões e efeitos visuais não
dupliquem regras de negócio na tela".
**Decisão.** `ReacaoAosEventos` traduz os eventos de cada turno em sons e impacto (padrão Observer), sem olhar posições
nem vidas. `AnimacaoDoTurno` desliza da foto de antes para a de depois do turno, em 0,15 s. `EfeitosDaPartida` cuida
da explosão e do tremor. `CenaDaPartida` orquestra tudo (Humble Object) e só libera a tela de fim depois que a explosão
termina. As teclas não esperam a animação: um turno novo começa da foto nova. O som fica atrás da interface `Sons`,
com o objeto nulo `Sons.NENHUM` e um dublê que anota os sons nos testes.
**Consequências.** As regras continuam só no `nucleo`, que não mudou nenhuma linha. O console segue idêntico: 200/200
partidas iguais à Fase 3. A apresentação ganha 44 testes que rodam sem janela, áudio ou relógio real. Divergência
registrada: não usamos `Intersector`, porque não há colisão contínua para detectar.

## DA-14 — Servidor como build Gradle independente, com Spring Boot 4.1.2 (Fase 5)

> Substituída em parte pela DA-39 (2026-11-26): o servidor passa a incluir o `nucleo`, e só ele. E pela DA-41
> (2026-11-26): o banco passa a ser o PostgreSQL, com o esquema no Flyway.

**Contexto.** O enunciado pede um servidor independente, que não dependa do código do cliente. A issue #7 pede um
"subprojeto independente que inicia conforme o README". O README do curso trata cliente e servidor como projetos
separados.
**Decisão.** A pasta `servidor/` tem `settings.gradle`, wrapper e `build.gradle` próprios: Spring Boot 4.1.2, a
versão estável mais nova (a 4.2 está em RC), springdoc 3.2.0 (feito sobre o Boot 4.1.2), JPA com H2 em arquivo,
Java 17 e Gradle 8.14.3, os mesmos do cliente. O build do jogo não inclui o servidor, e o servidor não inclui nenhum
módulo do jogo. O contrato entre os dois é só a API, exportada em `docs/api/openapi.json`. Os nomes de resultado e de
dificuldade são repetidos de propósito nos dois lados.
O plugin do Spring Boot 4 exige que o próprio Gradle rode em Java 17 ou mais novo (descoberto na instalação limpa,
com Java 11 como padrão). Por isso o servidor tem `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=17`): o
wrapper roda o daemon num JDK 17 instalado.
**Consequências.** Dois comandos e dois terminais (`./gradlew :lwjgl3:run` e `cd servidor && ./gradlew bootRun`). O
servidor pode ser publicado sozinho (Sprint 4) e reimplementado em outra linguagem com o mesmo contrato (a versão
Node chegou a ser planejada e foi descartada; ver DA-18). Custo: o enum de dificuldade existe nos dois lados.

## DA-15 — Senha com BCrypt, token JWT sem estado e erros no formato RFC 9457 (Fase 5)

**Contexto.** O enunciado exige hash de senha, token nas operações autenticadas, validação, erros padronizados, DTOs
e nenhum segredo versionado ou em log.
**Decisão.**
- Senha guardada com `BCryptPasswordEncoder`.
- Login em `POST /api/sessoes`, que devolve um JWT HS256 com o id do jogador no `sub` e validade de 2 h (30 dias
  desde a DA-38). É validado
  pelo resource server do Spring Security (`spring-boot-starter-oauth2-resource-server`), sem sessão no servidor e
  sem biblioteca extra.
- O segredo vem só de `MISSAOMARTE_JWT_SEGREDO`. Sem a variável, o servidor gera um segredo temporário e avisa no
  log; com menos de 32 bytes, recusa iniciar.
- Nome inexistente e senha errada dão a mesma resposta 401, para não revelar quem está cadastrado.
- Erros como `ProblemDetail`, com `campos` na validação. O 401 do filtro de segurança sai no mesmo formato.
- Entidades JPA ficam internas, e a API só fala com `record`s. O `toString` dos pedidos e da sessão esconde a senha
  e o token.

**Consequências.** Os testes cobrem hash, login certo e errado, e token ausente, inválido, adulterado e expirado (o
relógio é injetado). Nenhuma senha, token ou segredo apareceu nos logs do servidor nem do cliente durante a
validação ponta a ponta.

## DA-16 — Regras do ranking global e limites do servidor (Fase 5)

> Substituída em parte pela DA-39 (2026-11-26): o servidor passa a ter as regras do jogo (`nucleo`) para gerar e
> conferir as hordas. Ele ainda não refaz as partidas: a limitação sobre a pontuação continua valendo nesta etapa.

**Contexto.** A issue #8 pede o Top 10 com a maior pontuação de cada jogador e testes de ordenação, empate, limite e
isolamento. O ranking local (Top 5) só guarda vitórias.
**Decisão.** Cada partida concluída (vitória, derrota ou abandono) vai para o histórico do dono do token, que nunca é
escolhido pelo corpo do pedido. O histórico devolve as 50 mais recentes. O ranking considera só vitórias e mostra a
melhor de cada jogador, da maior para a menor. No empate, fica na frente quem fez a pontuação primeiro. O limite é 10.
A consulta traz as vitórias já ordenadas (com `join fetch`), e o serviço pega a primeira de cada jogador.
**Consequências.** Mesma regra do ranking local, então o jogador entende os dois. **Limitação, como o enunciado pede
registrar:** o servidor não executa o jogo e não tem como conferir se a pontuação enviada é legítima; um cliente
modificado pode mandar qualquer número dentro dos limites da validação (até 10.000 pontos). Com muitos dados, a regra
do ranking caberia numa consulta com função de janela, em vez de ler todas as vitórias.

## DA-17 — Cliente: porta para o servidor, adaptador assíncrono e modo convidado (Fase 5)

**Contexto.** O plano de aulas fala em login obrigatório. O enunciado manda preservar o modo convidado e o ranking
local, e integrar a API sem bloquear a interface. A issue #9 pede que erros de rede não encerrem a partida e que os
testes do domínio não precisem do servidor.
**Decisão.**
- A primeira tela depois do carregamento oferece entrar, criar conta ou jogar como convidado, que joga exatamente
  como na Fase 4.
- As telas dependem da interface `ServidorDoJogo` (porta). `ServidorHttp` é o adaptador: usa o `Gdx.net`, que é
  assíncrono e também funciona no Android, e entrega as respostas na thread do jogo.
- Falhas viram `Falha`, com uma mensagem para o jogador.
- O fim da partida envia o resultado em segundo plano, e a tela só mostra o andamento.
- O ranking ganhou as abas local e global, e o menu ganhou as opções histórico e trocar de piloto.
- A sessão (nome e token) fica só na memória. Revisto na DA-38: agora ela também fica guardada no aparelho.
- O endereço vem de `-Dmissaomarte.servidor`, com padrão `http://localhost:8080`.
- Escolha registrada com o usuário em 2026-11-18: convidado em vez de login obrigatório.

**Consequências.** Com o servidor desligado, a entrada mostra "Servidor indisponível. Você pode jogar como
convidado.", e o resto do jogo segue normal. Os 186 testes do cliente rodam sem servidor. O adaptador é testado
contra um servidor HTTP falso do JDK em localhost. `nucleo` e `console` não mudaram.

## DA-18 — Só o servidor Java, em contêiner, com docker compose para desenvolvimento (Fase 5)

**Contexto.** Em 2026-11-17 a equipe cogitou uma segunda versão do servidor, em Node.js, para publicar. Em 2026-11-18
decidiu manter só a API Java, conteinerizada. A Sprint 4 pede o servidor pronto para publicação, com variáveis de
ambiente, porta, verificação de saúde e persistência documentadas.
**Decisão.** `servidor/Dockerfile` em dois estágios: JDK 17 para compilar com o wrapper e JRE 17 para rodar o jar,
com usuário sem privilégios, `VOLUME /dados`, `EXPOSE 8080` e `HEALTHCHECK` no `/actuator/health`.
`servidor/compose.yaml` cuida do desenvolvimento: porta configurável, volume nomeado, segredo vindo de um `.env`
ignorado pelo Git (com `.env.exemplo` versionado e vazio) e `develop.watch`, que reconstrói a imagem quando `src/` ou
o `build.gradle` mudam. O `bootJar` passou a ter nome fixo (`missao-marte-servidor.jar`), e o jar "plain" foi
desligado.
**Consequências.** Quem não tem Java sobe o servidor com `docker compose up --build`, e a mesma imagem serve para
publicar (por exemplo, no Dokploy, se a equipe decidir). A imagem tem cerca de 577 MB, quase tudo do JRE em Ubuntu;
uma base menor fica como melhoria. Não verificados: o `--watch` na prática e a imagem em amd64.

## DA-19 — Módulo Android opcional, minSdk 26 e API mínima conferida no APK (Fase 6)

**Contexto.** A Fase 6 pede um APK que reaproveite o `core`. Nem toda máquina que compila o projeto tem o Android
SDK (o professor corrigindo o console, por exemplo). O `nucleo` usa `java.nio.file` (ranking em arquivo) e
`java.time` (datas). O lint do Android não analisa `core` nem `nucleo`, que para ele são dependências externas.
**Decisão.**
- Módulo `android` com o Android Gradle Plugin 8.13.2 (o mais novo compatível com o Gradle 8.14.3),
  `compileSdk`/`targetSdk` 36 e `minSdk` 26.
- O `settings.gradle` só inclui o módulo quando há SDK (`ANDROID_HOME`, `ANDROID_SDK_ROOT` ou `sdk.dir`).
- Os assets vêm de `assets/` da raiz, sem cópia.
- As bibliotecas nativas da libGDX e do FreeType (4 ABIs) são extraídas por uma tarefa (`copiarNativas`) para
  `android/libs`, pasta gerada e ignorada pelo Git.
- O `AndroidLauncher` é só a raiz de composição: o ranking fica em `getFilesDir()`, o toque vem ligado e o servidor
  padrão é o do emulador.
- A API mínima é conferida no APK pronto por `validacao/android/conferir_api_minima.py`, que lê o bytecode final e
  procura cada chamada do jogo no `api-versions.xml`, subindo pela hierarquia das classes.

**Consequências.** Sem SDK, o build segue igual ao da Fase 5. O APK de depuração tem 10 MB e roda do Android 8.0 em
diante. A conferência encontrou 0 chamadas do jogo acima da API 26 e acusou uma chamada da API 33 inserida de
propósito. Não verificado: um aparelho de verdade abaixo da API 34.

## DA-20 — ExtendViewport e layout da partida calculado em Java puro (Fase 6)

**Contexto.** O `FitViewport` deixava faixas pretas em telas que não são 4:3 (mais da metade de um celular em pé),
e a `TelaPartida` usava posições fixas para 800x600. A issue #11 pede que nada fique cortado nem sobreposto em
retrato e paisagem.
**Decisão.**
- A `TelaBase` usa `ExtendViewport(800, 600)` com a câmera sempre em (400, 300): as telas desenhadas para 800x600
  continuam inteiras e centralizadas, e o mundo cresce no lado que sobra.
- `visivel()` informa a área real, e o fundo a cobre toda.
- Só a partida reorganiza o conteúdo. Ela usa o `LayoutDaPartida`, um record em Java puro que recebe a área
  visível e o modo toque e devolve o mapa, o painel e os botões:
  - paisagem: mapa centralizado na vertical à esquerda, painel e controles na borda direita;
  - retrato: mapa em cima, painel no meio e controles embaixo.
- Cada botão tem pelo menos 72 unidades (cerca de 48 dp em paisagem), e o Abortar fica longe do Embarcar.

**Consequências.** O layout é testado em vários tamanhos sem abrir janela: nada cortado, nada sobreposto, alvo
mínimo e, em 800x600 sem toque, a mesma posição das Fases 4 e 5. Os prints do desktop saem iguais aos da Fase 5,
salvo as animações. Em 4:3 com toque, o mapa cai para 282 de lado.

## DA-21 — Toque pelo caminho do teclado, Voltar como Esc e caixas de texto do sistema (Fase 6)

**Contexto.** As telas e os controladores eram testados pelo teclado. No celular não há teclado físico, e o botão
Voltar fecha a `Activity` por padrão.
**Decisão.**
- Cada tela declara `alvosDeToque()` como pares `(Retangulo, ação)`. Quase toda ação "aperta" a tecla equivalente
  (`keyDown`, `keyTyped`, `keyUp`), e a `TelaBase` converte o toque com `viewport.unproject`.
- O `Keys.BACK` é capturado e tratado como `Esc`, menos na partida: lá, abortar só pelo botão Abortar.
- No Android, nome, senha e endereço vêm de `Gdx.input.getTextInput` (com o teclado de senha). Depois do OK, o
  teclado virtual é escondido.
- `-Dmissaomarte.toque=true` mostra os controles no desktop.

**Consequências.** Não há um segundo caminho de entrada para testar: os controladores são os mesmos. No emulador e
no celular, o jogo inteiro foi jogado só com toques. Um toque por ação, sem repetição ao segurar o botão
(a partida é por turnos).

## DA-22 — Endereço do servidor configurável no jogo e HTTP liberado na rede local (Fase 6)

**Contexto.** No celular não existe `-Dmissaomarte.servidor`, e `localhost` é o próprio aparelho. Em aula, o
servidor roda num computador da rede local, sem certificado. Desde o Android 9, `http://` é bloqueado por padrão.
**Decisão.**
- `ConfiguracaoDoServidor` decide o endereço, nesta ordem:
  1. o `-D` da linha de comando (só no desktop, sem poder trocar na tela);
  2. o gravado pelo jogador na opção 4 da entrada (nas `Preferences` da libGDX);
  3. o padrão da plataforma: `localhost:8080` no desktop e `10.0.2.2:8080` no Android (o computador visto pelo
     emulador).
- Só aceita `http://` ou `https://`, com host, porta de 1 a 65535 e sem usuário, `?` ou `#`.
- O `ServidorHttp` lê o endereço a cada pedido, então a troca vale na hora.
- O manifesto tem `usesCleartextTraffic="true"`.

**Consequências.** O mesmo APK serve para o emulador (sem configurar nada), para um celular na rede local (IP do
computador) e para um servidor publicado (`https://`). O HTTP fica liberado em qualquer rede. Restringir isso
com um `network_security_config` numa versão publicada fica como melhoria.

## DA-23 — O Gradle da raiz também roda em Java 17 (Fase 6)

**Contexto.** O plugin Android, como o do Spring Boot 4 (DA-14), exige que o próprio Gradle rode em Java 17 ou mais
novo. O toolchain só escolhe o JDK que compila.
**Decisão.** `gradle/gradle-daemon-jvm.properties` na raiz, com `toolchainVersion=17`, e
`android.useAndroidX=true` no `gradle.properties` (o backend Android da libGDX usa o `androidx.core`).
**Consequências.** O requisito continua sendo "um JDK 17 instalado em qualquer lugar que o Gradle detecte", mesmo
com outro `java` como padrão. O Gradle avisa que a descoberta do JVM do daemon é um recurso em incubação.

## DA-24 — Servidor pronto para publicar: endereço público no Swagger e guia de publicação (Fase 6)

**Contexto.** A Sprint 4 pede o servidor pronto para publicação, com as variáveis, a porta, a saúde e a
persistência documentadas. Num teste em contêiner, o Swagger mostrou o endereço `http://localhost:8080` fixo no
código, mesmo com os cabeçalhos de proxy: publicado, o "Try it out" mandaria os pedidos para a máquina de quem
abriu a página.
**Decisão.** A variável `MISSAOMARTE_URL_PUBLICA` (padrão `http://localhost:8080`) define o endereço do OpenAPI, com
um teste automático. `servidor/PUBLICACAO.md` documenta:
- as variáveis (o segredo é obrigatório e nunca fica no Git);
- o volume `/dados`, com uma instância só, e a cópia de segurança;
- a saúde (`/actuator/health`, `/liveness` e `/readiness`);
- o HTTPS por proxy reverso;
- como desligar a documentação (`SPRINGDOC_*_ENABLED=false`);
- uma lista de verificação.

**Consequências.** Publicar é configurar a plataforma com esse guia, sem mudar código. Testado localmente com
Docker: a variável, a documentação desligada, a cópia de segurança e os dados mantidos ao recriar o contêiner.
Não testados: um proxy HTTPS de verdade e um domínio público. Publicar é uma ação externa e só acontece a pedido.

## DA-25 — A melhoria da Fase 7 é um modo novo, ao lado do clássico (Fase 7)

**Contexto.** A Fase 7 pede uma melhoria pequena e demonstrável, que não altere comportamentos anteriores sem
teste nem explicação. A mecânica de pilotagem da POC (girar, motor, gravidade, pouso) foi validada pelo usuário em
2026-11-04 e reservada para esta fase (decisão de 2026-11-04, reconfirmada em 2026-11-17).
**Decisão.**
- A pilotagem é um **modo de jogo novo**, escolhido na tela de nova missão ("Modo", tecla Tab ou botão).
- O clássico continua o padrão e não muda.
- O console não ganha a pilotagem: física em tempo real não cabe num terminal por turnos.
- As regras ficam num pacote próprio do `nucleo` (`pilotagem`).
- A análise, com os critérios de aceite e os casos de teste, foi escrita e commitada antes do código
  (`docs/fase-07-analise.md`).

**Consequências.** O modo clássico é provado igual ao da Fase 6: 200/200 partidas pela semente no console, os
roteiros do gabarito e os testes antigos. Só as telas de nova missão e de ranking ganharam a escolha do modo, uma
mudança intencional. O jogo agora tem dois modos para manter.

## DA-26 — Física em passo fixo de 1/120 s e tempo simulado (Fase 7)

**Contexto.** A física com o tempo do quadro muda conforme a taxa de quadros (30 no celular fraco, 144 num monitor
rápido), e testes que dependem do relógio são lentos e instáveis. A issue #14 pede regras sem relógio, janela ou
rede nos testes.
**Decisão.**
- `PartidaDePilotagem.atualizar(segundos, comandos)` acumula o tempo do quadro e executa quantos passos de 1/120 s
  couberem. Um quadro longo (a volta do segundo plano) vale no máximo 0,25 s.
- O tempo da partida é a soma dos passos, e nenhuma regra lê o relógio.
- Os asteroides são funções do tempo (seno), sem sorteio.

**Consequências.** O mesmo voo dá o mesmo resultado a 30 e a 144 quadros por segundo (teste). O piloto automático é
determinístico, e a vitória no Fácil é um teste de unidade (0,1 s) e também um roteiro na janela real. O tempo
mostrado no fim é o tempo de jogo simulado, que pode diferir alguns décimos do relógio de parede.

## DA-27 — A pilotagem reaproveita o domínio e os serviços do clássico (Fase 7)

**Contexto.** O enunciado pede usar os pontos de extensão existentes. Duplicar regras (pontos por passageiro,
dificuldade, ranking) criaria duas fontes de verdade.
**Decisão.** A pilotagem usa as peças do clássico:
- **passageiros:** os mesmos `Astronauta`, `Engenheiro` e `Professor`, com os mesmos nomes; os pontos vêm do
  polimorfismo de `Passageiro` (20/15/10, desde a Fase 1);
- **dificuldade e estado:** a `Dificuldade` (cada uma mapeada para uma `ConfigDeVoo`) e o `Partida.Estado` (em
  andamento, vitória, derrota, abortada);
- **resultado e ranking:** o `ResultadoPartida` (na pilotagem, os "movimentos" são os pousos) e o `ServicoRanking`
  com outro `RepositorioRanking` (`ranking-pilotagem.json`), que é o padrão Repository da Fase 1 pagando dividendo;
- **fim e envio:** o `FimDePartida` e a `TelaFimDePartida`, com o modo e um resumo do voo (o construtor antigo
  continua valendo para o clássico), e o `EnvioDoResultado` e a `TelaHistorico`, sem mudança de fluxo.

**Consequências.** As regras novas são só as do voo. A correção do plano ("10 por passageiro", um número da POC)
virou "os pontos do clássico", que é a regra de verdade da versão-base. Os pontos dos dois modos não são
comparáveis, e por isso cada modo tem o seu ranking.

## DA-28 — Entrada contínua: o controlador acompanha teclas e dedos (Fase 7)

**Contexto.** Até a Fase 6, um toque equivalia a "apertar uma tecla", o que bastava para um jogo por turnos. Na
pilotagem, os comandos valem enquanto estão apertados, e é preciso girar e acelerar ao mesmo tempo (dois polegares).
**Decisão.**
- A `TelaBase` e o `FiltroDeEntrada` passam a repassar o número do dedo, o soltar e o arrastar.
- O `ControladorDaPilotagem` (Humble Object) guarda as teclas apertadas e a posição de cada dedo e, a cada quadro,
  calcula os `Comandos`. Um dedo que escorrega para fora do botão o solta, e o Abortar age na hora.
- O layout dos botões (`LayoutDaPilotagem`) é Java puro e testado: ◄ ► à esquerda, MOTOR maior à direita e Abortar
  no alto.

**Consequências.**
- Dois dedos ao mesmo tempo funcionam, e isso está testado. O `adb` não simula dois dedos, então isso foi visto
  só à mão.
- O teste do controlador, pilotando só pelo teclado, achou um defeito real: girar junto com o motor na decolagem
  fazia a nave "pousar" de novo no mesmo instante. Agora a nave sobe reta até os pés saírem da plataforma
  (`SUBIDA_ANTES_DE_GIRAR`).

## DA-29 — O modo entra na API sem quebrar os clientes antigos (Fase 7)

**Contexto.** Os clientes das Fases 5 e 6 não mandam modo, e o banco de desenvolvimento e o de quem já publicou
têm partidas gravadas. Cada modo precisa do seu ranking global.
**Decisão.**
- `modo` é um campo **opcional** no `POST /api/partidas` (sem ele, `CLASSICO`) e aparece no histórico.
- `GET /api/ranking` aceita `?modo=` (padrão `CLASSICO`).
- A coluna `modo` tem `@ColumnDefault("'CLASSICO'")`: ao ser criada num banco com dados, as partidas antigas viram
  clássicas.
- O servidor tem o seu enum `Modo`, como já tinha a `Dificuldade` (DA-14).
- O cliente pede o ranking pelo modo (`ServidorDoJogo.ranking(Modo, ...)`) e manda o modo no resumo.

**Consequências.**
- Um cliente antigo continua recebendo o mesmo ranking (o clássico).
- A migração foi conferida no banco de desenvolvimento, com partidas das Fases 5 e 6: o ranking ficou igual, e a
  partida antiga aparece como `CLASSICO`.
- Um servidor antigo ignora o campo novo (o Jackson aceita campos desconhecidos), mas devolve o ranking clássico
  também para a pilotagem.

## DA-30 — Um piloto automático como instrumento de validação (Fase 7)

**Contexto.** Provar que a caverna pode ser vencida com as regras de verdade, sem mãos, é difícil: há paredes, uma
passagem estreita com um asteroide que sobe e desce, pousos com limites e combustível finito.
**Decisão.**
- `PilotoAutomatico` (nos testes do `nucleo`): uma rota por pontos de passagem e um controle por realimentação.
  Ele calcula a velocidade desejada e o empuxo necessário, gira para o ângulo certo e modula o motor
  (sigma-delta). Antes da passagem estreita, espera o asteroide começar a descer.
- O mesmo piloto voa a janela real (roteiro `pilotagem` do `PilotoDaJanela`), apertando e soltando as setas a cada
  quadro.

**Consequências.**
- A vitória no Fácil é um teste determinístico.
- No Médio e no Difícil, o piloto faz as viagens do Astronauta e do Engenheiro. A do Professor, a mais longa, passa
  dos 100 de combustível do jeito conservador como ele voa.
- Esse número levou a duas decisões: o tanque do Difícil voltou para 100 (com 80, a caverna seria impossível) e
  o achado sobre o Médio foi levado ao usuário, junto com a partida jogada à mão no celular.


## DA-31 — Modo shadow: o servidor só repassa posições, por WebSocket puro (Fase 7)

**Contexto.** O bloco B da Fase 7 pede ver, em tempo real, as naves de outros pilotos. Cada um continua jogando a
própria partida: as sombras não batem, não pousam e não pegam passageiros. O HTTP da API não serve para isso:
seriam 10 pedidos por segundo por piloto, só para perguntar "mudou alguma coisa?".
**Decisão.**
- Um canal WebSocket no mesmo servidor Spring Boot (`spring-boot-starter-websocket`), em `/ws/sombras`, com um
  `TextWebSocketHandler` (o `CanalDeSombras`). É WebSocket puro, sem STOMP: só há três mensagens (`posicao`,
  `sombra` e `saiu`), e um protocolo de tópicos seria peso sem uso.
- O servidor não simula nada: confere a mensagem e repassa a posição a todos os outros conectados. Quem sai é
  anunciado (`saiu`) para a sombra sumir na hora.
- Não há salas: todos estão na mesma caverna, que é a única do jogo.

**Consequências.**
- O servidor continua sem estado de jogo (só a lista de conexões abertas, na memória). Reiniciá-lo derruba as
  sombras, e os clientes reconectam sozinhos.
- Se um dia houver colisão entre naves ou disputa por passageiros, o servidor precisará simular a partida (ou
  arbitrar), e isso muda bastante a arquitetura. Ficou fora do escopo.

## DA-32 — O canal das sombras usa o mesmo token da API e tem limites (Fase 7)

**Contexto.** Um canal aberto na internet aceitaria qualquer um, com qualquer nome, mandando mensagens de qualquer
tamanho na velocidade que quisesse.
**Decisão.**
- O aperto de mão do WebSocket é um GET comum, então passa pela mesma segurança da API: sem
  `Authorization: Bearer <token>` válido, a resposta é 401 e a conexão não abre. O convidado não participa.
- O nome mostrado vem do token (`nome`), nunca da mensagem: ninguém se passa por outro piloto.
- Limites: até 512 bytes por mensagem (acima disso, a conexão é fechada); posição dentro da caverna e ângulo entre
  -180 e 180 (senão, a mensagem é ignorada); no máximo uma posição a cada 40 ms por piloto (as outras são
  descartadas); até 50 pilotos ao mesmo tempo. O envio a cada piloto é protegido por um
  `ConcurrentWebSocketSessionDecorator` (1 s e 16 KB de fila), para um cliente lento não travar os outros.

**Consequências.**
- Os testes de ponta a ponta (`CanalDeSombrasTeste`, servidor numa porta aleatória e clientes WebSocket de verdade)
  conferem cada regra. Um deles achou um estouro: o "último envio" começava em `Long.MIN_VALUE`, e
  `agora - Long.MIN_VALUE` dá negativo, então a primeira posição de cada piloto era sempre descartada.
- Como na API, o servidor não confere se a posição é possível pela física (um cliente modificado poderia
  "teleportar" a própria sombra). Como as sombras não interferem no jogo dos outros, o risco é só visual.

## DA-33 — No cliente, uma porta para as sombras e a biblioteca Java-WebSocket (Fase 7)

**Contexto.** A libGDX não tem WebSocket, e o Android não tem o `java.net.http` do Java 11. O mesmo código do
`core` precisa funcionar no desktop e no Android.
**Decisão.**
- A porta `ConexaoDeSombras` (abrir com o token, enviar, fechar e um ouvinte), e o adaptador `ConexaoWebSocket` com
  a Java-WebSocket 1.6.0 (MIT, Java puro). O endereço vem do endereço HTTP do servidor (`http` vira `ws`, `https`
  vira `wss`), então a opção 4 da entrada vale para as duas coisas.
- A biblioteca avisa numa thread própria; o adaptador repassa cada aviso à thread do jogo
  (`Gdx.app.postRunnable`), como o cliente HTTP faz. Avisos de uma conexão já fechada são descartados.
- A lógica fica na `SombrasDaPilotagem`, em Java puro e testada com uma conexão falsa. Ela manda a posição 10 vezes
  por segundo. Entre uma mensagem e outra, a sombra desliza até a última posição recebida (suavização
  exponencial; o ângulo gira pelo lado mais curto), em vez de pular. Sombra sem notícia por 3 s some, e, se a
  conexão cair, o jogo tenta de novo a cada 3 s.
- A `TelaPilotagem` só desenha (nave translúcida e azulada com o nome, ponto no minimapa e uma linha no painel) e
  fecha a conexão ao sair da tela.

**Consequências.**
- A sombra fica de 100 a 200 ms atrás da nave de verdade (o intervalo entre as mensagens mais a suavização). Para
  ver outro piloto voar, isso basta; para uma corrida disputada, seria preciso prever o movimento (extrapolação).
- `conferir_api_minima.py` conferiu as classes da biblioteca e do slf4j que ela usa: nada acima da API 26. A
  biblioteca escreve um aviso do slf4j ("No SLF4J providers") no log, sem efeito.

## DA-34 — O tanque do Médio continua 100 (Fase 7)

**Contexto.** O piloto automático, que voa de forma conservadora, precisa de cerca de 120 de combustível na viagem
do Professor (DA-30). A POC dizia que um humano consegue com 100. A decisão ficou com o usuário, depois de jogar à
mão.
**Decisão.** Depois de jogar a pilotagem no celular e no desktop, o usuário decidiu manter o tanque do Médio em 100
(2026-11-18).
**Consequências.** O Médio continua sendo a POC sem mudanças, e o Fácil (130) fica como a opção para quem ainda
está aprendendo a pousar. O piloto automático continua sem vencer o Médio: isso mostra que ele é conservador, não
que o nível seja impossível.

## DA-35 — As sombras mostram a explosão dos outros pilotos (pós-entrega)

**Contexto.** O canal das sombras só levava a posição (DA-31). Quando outro piloto batia, o jogo dele parava de
enviar a posição: a sombra congelava no ponto da batida e, quando ele renascia, deslizava até a base. A explosão
dele ninguém via. O usuário pediu para ver a explosão dos outros participantes (2026-11-25).
**Decisão.**
- **Mensagem nova.** Quem bate manda `{"tipo":"explodiu","x","y"}`. O servidor confere tamanho e caverna, como na
  posição, e repassa `{"tipo":"explosao","piloto","x","y"}` aos outros.
- **Quem assiste.** A sombra sai da tela e a mesma animação de explosão aparece, translúcida, sem som nem tremor.
  Quando o outro renasce, a próxima posição traz a sombra de volta já na base.
- **Limite próprio.** A explosão aceita no máximo uma por segundo e não divide o limite de 40 ms com as posições.
  A validação na janela real mostrou o problema: a batida acontece a qualquer instante, e com o limite único cerca
  de 40% das explosões caíam a menos de 40 ms da última posição e eram descartadas.
**Consequências.** O protocolo continua compatível nos dois sentidos. Um cliente antigo ignora `explosao`, que é
um tipo desconhecido para ele, e um servidor antigo ignora `explodiu`. Evidências em
`docs/evidencias/fase-07/explosao-das-sombras/`.

## DA-36 — Pilotagem como modo padrão e folga de toque nas setas (pós-entrega)

**Contexto.** Jogando no celular, o usuário pediu duas mudanças (2026-11-25). A nova missão deve começar na
Pilotagem. E as setas devem aceitar o polegar que cai um pouco fora do botão: o toque "não pegava".
**Decisão.**
- **Modo padrão.** `FormularioNovaPartida` começa na Pilotagem. Tab ou "Trocar" leva ao Clássico.
- **Folga de toque.** As setas (◄ ► da pilotagem e o direcional do clássico) aceitam toques até 24 unidades fora
  do botão, um terço do alvo mínimo.
  - O toque dentro de qualquer botão vale primeiro. Fora de todos, vale a seta mais próxima dentro da folga.
  - O vão entre ◄ e ► fica com a seta mais perto. O meio do direcional continua sem apertar nada, em todas as telas.
  - Motor, Embarcar, Som e Abortar não têm folga, para não serem tocados sem querer.
- **Onde fica a regra.** Está nos layouts (`LayoutDaPartida.botaoEm`, que o `LayoutDaPilotagem` reaproveita). A
  partida clássica passou a perguntar ao layout pelo gancho `TelaBase.acaoNoPonto`, e o controlador da pilotagem
  usa o `botaoEm` também para acender o botão.
**Consequências.** Os roteiros de validação passaram a trocar para o Clássico quando jogam nele (`PilotoDaJanela`
e `pilotar_android.py`). O roteiro `toque-folga` toca cada seta de 12 a 15 unidades fora do botão. Com a folga, ele
vence a partida. Com a folga zerada, a nave não se move. Evidências em `docs/evidencias/fase-06/folga-de-toque/`.

## DA-37 — Pacotes nativos para Windows e macOS com o Java embutido (pós-entrega)

**Contexto.** O jogo de PC era um zip que exigia o Java 17 instalado. No Windows, instalar o Java costuma pedir
administrador. O usuário pediu versões que funcionem sem isso (2026-11-25). No macOS, um app aberto com dois
cliques roda com a pasta de trabalho em "/", onde o jogo não consegue gravar o `ranking.json`.
**Decisão.**
- **Pacotes com `jpackage`, que vem com o JDK 17.** Nenhum plugin de terceiros: a tarefa
  `./gradlew :lwjgl3:pacoteNativo` gera um app-image com um Java enxuto.
  - O Java tem só os módulos que o `jdeps` aponta (`java.base`, `java.desktop`, `java.management`,
    `jdk.unsupported`), mais o `jdk.crypto.ec`. Sem esse último o HTTPS falha, como o teste mostrou.
  - No Windows, é uma pasta com `Missao Marte.exe`, que roda descompactada em qualquer pasta do usuário.
  - No macOS, é o `Missao Marte.app`, com `-XstartOnFirstThread` e assinatura ad hoc.
- **Uma máquina por sistema no CI.** O `jpackage` não gera pacote de outro sistema: Windows, Mac Apple Silicon e
  Mac Intel saem cada um da sua máquina do GitHub. Os zips vão para a página de downloads, ao lado do zip com Java,
  que continua para Linux.
- **Dados na pasta do usuário.** A versão empacotada grava o ranking local em `~/.missao-marte`
  (`-Dmissaomarte.dados=usuario`, `PastaDosDados`). O `./gradlew :lwjgl3:run` e o script do zip continuam com a
  pasta de trabalho, a mesma do console.
**Consequências.**
- **Sem assinatura paga.** Não há certificado da Microsoft nem conta de desenvolvedor da Apple ($99/ano). Na
  primeira vez, o Windows mostra o SmartScreen ("Executar assim mesmo"), e o macOS pede "Abrir Mesmo Assim" em
  Ajustes → Privacidade e Segurança. A página explica os dois.
- **Tamanho.** Cada zip tem cerca de 40 MB (o Java vai junto), e a imagem do servidor cresce na mesma medida.
- **Validação.** O pacote do Mac Apple Silicon foi testado aqui: aberto pelo `open` a partir de "/", ele sobe até a
  tela de entrada e cria `~/.missao-marte`. Os de Windows e Mac Intel são conferidos no CI (o `.exe` e o `.cfg`; a
  assinatura do `.app`), mas ninguém os abriu numa máquina de verdade ainda.

## DA-38 — A sessão fica guardada no aparelho, e o token vale 30 dias (pós-entrega)

**Contexto.** O usuário notou que o celular "deslogava sozinho" (2026-11-25). A investigação achou três causas:
- **O Android mata o jogo em segundo plano.** O `dumpsys activity exit-info` do celular (Galaxy S24 Ultra,
  Android 17) registrou `reason=LOW_MEMORY` com o jogo em segundo plano. Ao voltar, o processo é novo, e a sessão,
  que pela DA-17 ficava só na memória, tinha sumido: o jogo abria na tela de entrada. No emulador, o mesmo com
  `am kill`.
- **Trocar o tema claro/escuro recria a Activity.** O `uiMode` não estava no `configChanges`. O celular troca o
  tema sozinho no pôr do sol, se o tema escuro estiver agendado. Recriar a Activity reinicia o jogo do zero, mesmo
  em primeiro plano e no meio de uma partida. No emulador: `cmd uimode night yes`, depois `pause` e `create:` no
  mesmo processo.
- **O token valia 2 h, e o cliente ignorava o 401.** Depois desse prazo, o menu continuava em "Piloto conectado",
  mas o envio do resultado, o histórico e as sombras falhavam.
**Decisão** (o usuário escolheu entre guardar a sessão com um token longo, um token de renovação ou manter as 2 h):
- **A sessão fica guardada no aparelho.** `SessaoGuardada` grava nome, token, validade e o servidor que emitiu o
  token nas mesmas preferências do endereço do servidor: no Android, as SharedPreferences privadas do app; no
  desktop, `~/.prefs/missao-marte`. A senha nunca é guardada.
- **O jogo recupera a sessão ao abrir.** Depois do carregamento, `irParaInicio` vai direto ao menu se a sessão for
  do mesmo servidor e ainda valer. Sessão de outro servidor ou vencida é apagada na hora.
- **O token vale 30 dias** (`missaomarte.jwt.validade=P30D`).
- **Um 401 desconecta o piloto.** Se o servidor recusa o token no envio do resultado ou no histórico, o jogo apaga a
  sessão e volta ao modo convidado. A tela já mostra "Entre novamente". "Trocar de piloto" também apaga a sessão.
- **O manifesto ganhou `uiMode|locale|layoutDirection|fontScale` no `configChanges`.** Tema, idioma e tamanho da
  fonte do sistema não recriam mais a Activity, porque o jogo não usa nenhum deles.
**Consequências.**
- **Não dá para revogar um token antes do prazo.** O JWT continua sem estado: "Trocar de piloto" só apaga a cópia do
  aparelho, e um token copiado de lá vale até vencer. Trocar `MISSAOMARTE_JWT_SEGREDO` derruba todos os tokens de uma
  vez. Um token de renovação guardado no servidor resolveria, mas foi deixado de fora pelo custo (tabela, endpoint e
  testes nos dois lados).
- **No desktop, o token fica em texto puro no `~/.prefs`.** Quem lê os arquivos da conta do usuário no computador
  consegue usá-lo.
- **O que ainda se perde.** Se o Android matar o jogo durante uma partida, a partida acaba (o piloto continua
  conectado). As sombras não tratam o 401 do WebSocket à parte: a conexão só não abre.
- **Validação:** roteiros `sessao` e `sessao-vencida` do `pilotar_android.py` no emulador, contra servidores locais
  de 30 dias e de 2 min. O logcat mostra a sessão recuperada depois do `am kill`, nenhum `create:` ao trocar tema e
  fonte, a sessão apagada por "Trocar de piloto" e o 401 levando ao modo convidado. Desta vez as evidências ficam
  fora do repositório, por escolha do usuário.

## DA-39 — O servidor inclui o `nucleo` (Fase 8, 2026-11-26)

**Contexto.** A Fase 8 traz a Horda: um desafio novo a cada 2 horas, igual para todos, que o servidor gera e manda
pronto ao jogo. Para sortear uma caverna, conferir se ela é válida e, mais tarde, refazer partidas contra trapaça, o
servidor precisa das mesmas regras do jogo. Copiar essas regras para o servidor criaria duas versões que podem
divergir. A DA-14 dizia que o servidor não inclui nenhum módulo do jogo, e a DA-16 que ele não executa o jogo.
**Decisão.**
- O `servidor/settings.gradle` inclui `../nucleo`, e só ele. O `nucleo` é Java puro: domínio, aplicação e
  pilotagem, sem libGDX e sem nada do cliente (`core`, telas e rede ficam de fora).
- O build do servidor configura o `nucleo` ali mesmo (toolchain 17 e UTF-8), porque hoje ele herda essas
  configurações do `build.gradle` da raiz. A saída vai para `servidor/build/nucleo`, e o build do jogo continua dono
  de `nucleo/build`.
- Os testes do `nucleo` não rodam no build do servidor: o build da raiz já os roda, com o JUnit dele.
- O contexto do Docker passa a ser a raiz do repositório (`-f servidor/Dockerfile .`). Um
  `servidor/Dockerfile.dockerignore` deixa entrar só o servidor e o `nucleo`, num contexto de cerca de 520 kB. O
  compose, o CI e o deploy usam esse contexto.
- O contrato com o jogo continua sendo a API.

**Consequências.**
- Uma mudança no `nucleo` dispara o deploy do servidor (o `deploy.yml` já observava `nucleo/**`).
- O canal das sombras passa a ler o tamanho da caverna do próprio jogo, em vez de uma cópia dos números.
- O enunciado da Fase 5 pedia um servidor "que não dependa do código do cliente". O `nucleo` não é o cliente: são
  as regras, usadas também pelo console, sem libGDX. Mesmo assim, a dependência fica registrada aqui, para quem
  avaliar.
- `docker build` dentro de `servidor/` deixa de funcionar: o comando agora roda na raiz. O
  `cd servidor && docker compose up --build` continua igual.

## DA-40 — Colunas de enum como texto (Fase 8, 2026-11-26)

> Substituída em parte pela DA-41 (2026-11-26): o esquema vem das migrações do Flyway, e saíram o script
> `migracoes/enums-para-texto.sql`, o dialeto `H2SemCheckDeEnum` e o `MigracaoDoEsquemaTeste`. Continua valendo a
> regra: coluna de enum é `varchar(20)`, sem lista fechada de valores, e quem valida o valor é o enum do Java.

**Contexto.**
- O Hibernate 7.4 cria as colunas `@Enumerated(EnumType.STRING)` como `ENUM` nativo do H2. Num banco gerado pela
  versão da 7.x, isso dá `"MODO" ENUM('CLASSICO', 'PILOTAGEM')`.
- Esse tipo recusa qualquer valor novo, e o `ddl-auto=update` não muda o tipo de uma coluna que já existe.
- Por isso, a primeira partida da Horda (`modo = HORDA`) falharia no banco de produção, e os testes não pegariam,
  porque recriam o banco do zero (`create-drop`).

**Decisão.**
- As colunas de enum passam a texto: `@JdbcTypeCode(SqlTypes.VARCHAR)` com `length = 20`. Toda coluna de enum nova
  segue a mesma regra.
- O dialeto `H2SemCheckDeEnum` (`spring.jpa.database-platform`) tira a lista fechada de valores. Sem ele, mesmo com a
  coluna em texto, o Hibernate cria toda tabela nova com `check (modo in ('CLASSICO','PILOTAGEM'))`. Isso vale para o
  volume novo de quem sobe o compose pela primeira vez e para cada tabela que as próximas etapas criarem em produção.
  O `ddl-auto=update` nunca muda essa lista. Achado na revisão da etapa 1, com um banco criado do zero.
- O script `migracoes/enums-para-texto.sql` converte o banco que já existe.
  - Ele roda em toda subida, antes do Hibernate (`spring.sql.init`).
  - Usa `ALTER TABLE IF EXISTS … ALTER COLUMN IF EXISTS … SET DATA TYPE CHARACTER VARYING(20)`, então pode rodar de
    novo sem efeito e não faz nada num banco novo.
  - Preserva os valores, o default e o `NOT NULL`.

**Consequências.**
- O teste `MigracaoDoEsquemaTeste` sobe o servidor sobre o esquema exato da 7.x
  (`src/test/resources/esquema-fase-7.sql`, extraído de um banco real) e confere que:
  - os tipos mudaram e as partidas antigas continuam iguais;
  - o ranking da API não mudou;
  - `HORDA` já cabe na coluna;
  - o script pode rodar de novo.
- O banco aceita qualquer texto nessas colunas, tanto o convertido quanto o criado do zero (o teste `EsquemaNovoTeste`
  confere que não há nenhum `CHECK` e que `HORDA` cabe). Quem valida o valor é o enum do Java, na entrada da API.
- A primeira subida em produção converte as colunas. Por isso o backup do volume `/dados` vem antes do merge.

## DA-41 — PostgreSQL, com o esquema no Flyway (Fase 8, 2026-11-26)

**Contexto.**
- O `ddl-auto=update` com H2 falhou duas vezes na etapa 1: as colunas `ENUM` nativas e a lista fechada
  `check (... in ...)` nas tabelas novas (DA-40). Nas duas vezes, os testes passavam, porque recriavam o banco do zero
  (`create-drop`).
- O H2 em arquivo só abre em um processo. Por isso, o deploy no Dokploy precisa ser stop-first, com uns 20 s fora do
  ar a cada versão.
- As próximas etapas criam várias tabelas (moedas, bilhetes, hordas, eventos).
- Decisão do usuário em 2026-11-26. O banco de produção é a instância geral de PostgreSQL 18 do Dokploy
  (`postgres-br`, no projeto `databases`), que outros projetos também vão usar.

**Decisão.**
- **Conexão.** O servidor lê `MISSAOMARTE_DB_URL`, `MISSAOMARTE_DB_USUARIO` e `MISSAOMARTE_DB_SENHA`. Na
  instância geral, o jogo tem banco e usuário próprios, os dois chamados `missaomarte`. O usuário é dono só desse
  banco e nunca é o `admin`.
- **Desenvolvimento só com o JDK.** Sem `MISSAOMARTE_DB_URL`, o servidor usa um H2 em arquivo em modo PostgreSQL
  (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`), também com o Flyway. O compose de
  desenvolvimento sobe um PostgreSQL 18 ao lado.
- **Esquema.** O esquema fica nas migrações do Flyway (`db/migration/V1__esquema_inicial.sql`: o esquema da 7.x, com
  as colunas de enum em `varchar(20)` sem check), e o Hibernate só confere (`ddl-auto=validate`). O SQL roda igual no
  PostgreSQL e no H2 em modo PostgreSQL. Saem o `spring.sql.init`, o script de conversão, o dialeto
  `H2SemCheckDeEnum` e o `spring.jpa.database-platform`: o dialeto é detectado pela conexão.
- **Virada.** O `ImportadorDoH2` é ligado por `MISSAOMARTE_IMPORTAR_H2` (`/dados/missaomarte` em produção).
  - Só roda se o PostgreSQL estiver vazio. Copia jogadores e partidas com os mesmos ids. Sem a coluna `modo`, a
    partida é clássica.
  - Depois, reinicia as colunas de identidade no maior id + 1.
  - Abre o H2 só para leitura (`ACCESS_MODE_DATA=r`), numa transação só. Se falha, a subida falha.
  - Roda na estratégia de migração do Flyway (`ConfiguracaoDoBanco`), da qual o JPA depende. Por isso termina antes
    de o Tomcat atender o primeiro pedido. Um `ApplicationRunner` rodaria depois.
- **Testes.** Os testes da API continuam rápidos, em H2 em memória em modo PostgreSQL e com o Flyway. Os do
  PostgreSQL de verdade usam Testcontainers (`postgres:18`, `@Testcontainers(disabledWithoutDocker = true)`).

**Consequências.**
- **Mudança de esquema é migração nova.** Toda tabela ou coluna nova é um arquivo `V<n>__descricao.sql` em
  `servidor/src/main/resources/db/migration`, que roda nos dois bancos. Uma migração já aplicada nunca é editada: o
  Flyway confere o checksum e recusa subir. Se a entidade não bate com as migrações, o `validate` derruba a subida,
  também nos testes.
- **O deploy volta a ser start-first** depois da virada: o PostgreSQL aceita as duas versões conectadas ao mesmo
  tempo. Com `MISSAOMARTE_IMPORTAR_H2` ainda definida, a versão nova vê o banco com dados e nem abre o H2.
- **O H2 antigo não abre no modo PostgreSQL.** Um banco antigo de desenvolvimento (`servidor/dados`) não abre mais
  (`Schema "PUBLIC" not found`): é preciso apagá-lo ou importá-lo. Um deploy sem `MISSAOMARTE_DB_URL` falha do mesmo
  jeito sobre o `/dados` da produção, sem perder dados (`BancoH2AntigoTeste`). Por isso as variáveis entram no
  Dokploy antes do merge.
- **Cópia de segurança com `pg_dump`.** A cópia do arquivo `.mv.db` com o servidor parado deixa de valer.
- **Custos.** Um serviço a mais para operar, compartilhado com outros projetos. Os testes do PostgreSQL precisam de
  Docker; sem ele, são pulados. O runner do GitHub Actions tem Docker.
- **Avisos do Flyway 12.4.0** (o gerenciado pelo Spring Boot 4.1.2). Com o PostgreSQL 18.7, nenhum aviso de versão.
  Com o H2 2.4.240, o aviso "Using H2 2.4.240 which is newer than the version Flyway has been verified with. The latest
  verified version of H2 is 2.3.232", que não afeta nada.
- **Validação.** Cinco classes de teste cobrem o esquema e a virada:
  - `EsquemaNovoTeste` (H2) e `EsquemaNoPostgresTeste` conferem o esquema da V1;
  - `ImportadorDoH2Teste` e `ImportacaoDoH2NoPostgresTeste` conferem a importação. O teste
    `aImportacaoTerminaAntesDeOServidorAtender` falha se ela virar um `ApplicationRunner`;
  - `BancoH2AntigoTeste` confere o banco antigo.
  Houve também um ensaio com a imagem que está em produção (`docs/evidencias/fase-08/etapa-1b-postgres-flyway.txt`):
  ranking, histórico e perfis iguais byte a byte depois da virada.
- **Revisão antes da virada.** Uma configuração errada não sobe "com sucesso" no banco errado. Isso importa porque o
  token só guarda o id do jogador: num banco vazio, o token antigo de alguém entraria na conta nova de outra pessoa.
  Por isso o servidor não sobe em três casos:
  - a URL do banco está vazia (`spring.datasource.embedded-database-connection=none`);
  - na imagem, sem `MISSAOMARTE_DB_URL` (`MISSAOMARTE_PERMITIR_H2=false`, conferido no `main`, antes de qualquer
    conexão);
  - a importação aponta para um arquivo que não existe, com o banco novo vazio.
- **Deploy start-first:** a versão antiga e a nova ficam no ar juntas por alguns segundos. Por isso toda migração
  precisa ser compatível com a versão anterior (expandir antes, contrair depois).

## DA-42 — Moedas por extrato e bilhete de partida (Fase 8, 2026-11-26)

**Contexto.** Vitórias e colocações rendem moedas. A loja precisa de um saldo consistente mesmo com pedidos
simultâneos, e o servidor recebe partidas tanto de jogadores com bilhete quanto de clientes da Fase 7.

**Decisão.**
- O saldo é a soma do extrato. A chave `(jogador_id, origem, referencia)` e a inserção condicional garantem
  um crédito por vitória, horda ou semana. A compra trava a linha do jogador ao conferir o saldo e debitar.
- Cada jogador tem um bilhete ativo por vez, com validade de 2 horas, ligado a modo, dificuldade e, na Horda,
  ao número da horda. O consumo confere o tempo real e gasta o bilhete uma vez.
- Na Horda, só entra uma partida com bilhete válido. Nos modos anteriores, clientes sem bilhete continuam
  registrando partidas sem ganhar moedas.

**Consequências.** A V2 cria extrato, bilhetes e skins. Quem vem do H2 começa com saldo zero. A V3 permite ligar
bilhetes e partidas à horda. O saldo e as premiações continuam consistentes entre instâncias no start-first.

## DA-43 — Skins só cosméticas (Fase 8, 2026-11-26)

**Contexto.** A loja vende variações da nave, e os outros pilotos precisam ver a skin equipada nas sombras.

**Decisão.**
- O servidor guarda o catálogo e os preços, as skins compradas e a skin equipada. O jogo guarda a arte.
- Todo jogador tem a skin `padrao`, que representa a nave de sempre. A V2 a define como padrão também para
  contas que vieram do H2. A skin passa no canal como id, lido do perfil ao abrir a conexão.

**Consequências.** A escolha da skin preserva a geometria de colisão. As folhas ficam fora do enum `Folha`,
e as sombras usam um atlas pequeno no jogo.

## DA-44 — A horda é gerada no servidor e vai pronta como dados (Fase 8, 2026-11-26)

**Contexto.** Todos os pilotos da horda precisam da mesma caverna, dificuldade, modificadores e roteiro,
mesmo durante um deploy.

**Decisão.**
- A âncora é **1º de janeiro de 2026, 00h00 em America/Fortaleza** (`03:00Z`). É a horda 0. Cada horda
  seguinte começa duas horas depois, nas horas pares de Fortaleza.
- A semente é o `long` formado pelos primeiros oito bytes, em ordem big-endian, do HMAC-SHA256 de
  `horda:N` com `MISSAOMARTE_HORDA_SEGREDO`. A descrição sorteada fica em `hordas`, com uma linha por
  número. Uma segunda instância relê a vencedora da inserção concorrente.
- O sorteio usa as cavernas liberadas no catálogo e, quando gravada, a caverna da horda anterior.
  O ensaio local força a caverna indicada, inclusive uma que ainda esteja em avaliação.
- A API envia a descrição como JSON e os segundos relativos até a virada.
- Novas hordas guardam a semente secreta da agenda, derivada por HMAC-SHA256 de `agenda:N`. Sementes públicas de eventos e pontos de VIP por sala usam derivações independentes.

**Consequências.** Uma alteração no catálogo, no segredo ou na versão do servidor só afeta as próximas
gerações. O ensaio ignora a linha publicada e não a altera. O fechamento trava a mesma linha usada no
registro, grava o Top 10 e credita seus prêmios na mesma transação.

## DA-45 — Canal das sombras com salas e eventos (Fase 8, 2026-11-26)

**Contexto.** A Horda pode ter uma caverna diferente do Livre, e um evento ao vivo precisa alcançar pilotos
nas duas modalidades. Esta decisão revisa a DA-31 e mantém a autenticação e os limites da DA-32.

**Decisão.**
- O WebSocket recebe `sala=livre` ou `sala=horda-N`. Sem o parâmetro, usa `livre`. Cada sala valida as
  posições com sua própria caverna e recebe apenas as sombras dela.
- Uma instância com pilotos conectados publica o evento da agenda secreta no horário do aviso. O banco distribui
  o evento às instâncias durante o start-first. Cada uma repassa para suas sessões, com um ponto de pouso de VIP
  por sala.
- O canal limita pedidos de VIP e resultados a um por segundo por piloto, descarta resultados repetidos e ignora pedidos de VIP após o anúncio do vencedor.
- O painel valida a conta no banco pelo `sub` do JWT e usa a lista `MISSAOMARTE_ADMINS`.

**Consequências.** Os clientes antigos continuam na sala Livre e podem ignorar os tipos novos. O agendador
entrega os avisos e inícios nas conexões abertas e retoma os fechamentos que venceram durante uma parada.

## DA-46 — Evento só atrapalha a partida (Fase 8, 2026-11-26)

**Contexto.** Os eventos ao vivo trazem perigos, e nem todos os pilotos estão conectados na hora em que
cada evento começa. O Top 10 da horda compara as melhores vitórias sob o desafio da horda.

**Decisão.**
- Sobreviver rende +3 no placar da semana por jogador e perigo, para uma sessão que enviou uma posição válida
  nos três segundos anteriores ao início. Uma explosão informada durante o perigo invalida a participação
  daquela sessão. O servidor aceita o resultado
  ao fim do evento, com 15 segundos de folga de rede.
- Um `UPDATE` condicional arbitra o VIP para todas as salas e instâncias. A partir de três segundos depois do início,
  o primeiro pedido válido rende +5 no placar da semana e gera `vip-pego` para as salas.
- O placar atribui a horda à semana em que ela foi jogada. A de domingo às 22h integra a semana que acaba na
  virada. Os eventos entram na semana de seu início. Empates no total semanal seguem a ordem dos ids de jogador.

**Consequências.** Pontos de evento ficam no placar semanal. O Top 10 guarda a melhor vitória por jogador, com
empate pela primeira pontuação registrada. Na segunda às 00h30 em Fortaleza, o fechamento semanal considera as
hordas encerradas e premia os três primeiros uma vez.

## DA-47 — Pouso mais tolerante e meteoros mais lentos (Fase 8, 2026-11-26)

**Contexto.** No teste da Fase 8 no celular, o usuário explodiu ao pousar a 15° no Médio, e os meteoros da chuva
(300 a 500 unidades/s, mais rápidos que a nave) não davam tempo de reagir.
**Decisão** (do usuário):
- **Inclinação máxima no pouso seguro:** Fácil 25°, Médio 20°, Difícil 15° (eram 18°, 14° e 12°). Os limites de
  queda e de deslize não mudaram.
- **Meteoros:** de 150 a 250 unidades/s. Uma travessia que levaria mais da metade da chuva vira a travessia reta
  entre as bordas mais próximas (na caverna deitada, uma queda vertical). Assim as entradas continuam espalhadas
  pelos 20 s.

**Consequências.** A Pilotagem Livre fica um pouco mais fácil daqui para frente: os recordes anteriores foram feitos
com a regra mais dura (o ranking não foi zerado). O HUD já mostra o ângulo em vermelho a partir do limite novo, porque
lê a mesma configuração.

## DA-48 — Ajustes do jogo por variável de ambiente (Fase 8, 2026-11-26)

**Contexto.** A DA-47 mudou os limites de pouso e a velocidade dos meteoros num APK novo. O usuário pediu para fazer
esse tipo de ajuste sem lançar outro jogo: "é possível deixar isso por variável de ambiente? para ser mais dinâmico".
A física roda no jogo, então uma variável só no servidor não muda nada no celular. O servidor precisa publicar os
números, e o jogo precisa usá-los.
**Decisão** (aprovada pelo usuário):
- **21 variáveis `MISSAOMARTE_AJUSTE_*`, todas opcionais.** Por dificuldade: tanque, queda, deslize, inclinação e
  velocidade dos asteroides. Também a velocidade mínima e a máxima dos meteoros e a duração de cada evento. Sem elas,
  vale o padrão da Fase 8 (`AjustesDeVoo.PADRAO`, no `nucleo`). Cada valor tem uma faixa. Um nome desconhecido, um
  valor fora da faixa ou uma duração que não é inteira impedem o servidor de subir, com o nome da variável no log.
  No deploy start-first, o servidor anterior continua no ar.
- **`GET /api/ajustes`** (público) publica os ajustes. A Pilotagem Livre os pede a cada volta ao menu. Sem resposta,
  fica com os últimos que chegaram ou com o padrão.
- **A horda congela os ajustes.** Cada horda grava os ajustes do servidor no sorteio (`descricao.ajustes`) e fica com
  eles até o fim, mesmo depois de um deploy. Assim todos disputam o mesmo Top 10 com a mesma regra. Uma horda gravada
  antes desta decisão não tem o campo e usa o padrão.
- **Os eventos de uma horda usam a duração dela:** os do roteiro, os automáticos e os do painel. O evento ao vivo
  chega ao jogo com a duração na mensagem (`duracao`), e o jogo usa essa duração, porque é no fim do evento do
  servidor que o resultado passa a valer. Antes, a duração do `nucleo` governava; ela era sempre igual à do
  servidor. A agenda continua sorteando os horários com o limite da duração mais longa, e com o padrão esse limite é
  o de antes. Os horários das hordas já gravadas não mudam.
- **Ficam fixos** a gravidade, o empuxo, o giro, o casco, os pés, o consumo e a pontuação: mudam a sensação do
  controle e o placar.

**Consequências.**
- Mudar um ajuste muda a dificuldade da Pilotagem Livre para quem joga depois. Os recordes anteriores ficam no
  ranking, como na DA-47.
- O jogo publicado antes desta decisão continua com os números fixos e ignora a rota nova. Num evento mais longo que o
  padrão, ele termina o perigo mais cedo e manda o resultado antes do fim. Para não perder os +3 desse jogador, o
  servidor aceita o resultado a partir da duração padrão.
- A ordem de carregamento dos padrões importa (`FaixaDosAjustes`): o jogo carrega a `ConfigDeVoo` antes dos
  ajustes, e um teste carrega cada classe primeiro num carregador novo.
- Os nomes, os padrões e as faixas estão em `servidor/PUBLICACAO.md`, seção 12.

