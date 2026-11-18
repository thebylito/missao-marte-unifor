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
