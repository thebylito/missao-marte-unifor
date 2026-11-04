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
