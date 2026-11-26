# Fase 8, etapa 1 — Servidor com o `nucleo` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O servidor passa a compilar o módulo `nucleo` (build Gradle, Docker, CI e deploy) e as colunas de enum do
banco passam a texto, sem mudar nada do jogo nem da API.

**Architecture:** O build próprio do servidor (`servidor/`) inclui `../nucleo` como subprojeto, configurado ali
mesmo (o `nucleo` hoje herda toolchain e JUnit do `build.gradle` da raiz). O Docker passa a usar a raiz do repo
como contexto, com um `Dockerfile.dockerignore` enxuto. As colunas `ENUM` nativas do H2 viram `CHARACTER VARYING`
por anotação na entidade e por um script SQL idempotente que o Spring roda antes do Hibernate.

**Tech Stack:** Gradle 8.14.3 (wrapper), Java 17 (toolchain), Spring Boot 4.1.2, Hibernate 7.4.13, H2 2.4.240,
JUnit (via Spring Boot no servidor), Docker BuildKit, GitHub Actions.

**Spec:** `docs/fase-08-analise.md` (seções 4, 5 e 10: DA-39 e DA-40) e o plano aprovado
`~/.claude/plans/fluttering-crafting-jellyfish-of-4bcf40d4-326d-4134-b731-8812b3e394f7.md` (seção "Servidor":
Build, Docker e Migração do banco).

## Global Constraints

- Nenhuma mudança de comportamento no jogo, no console ou na API (rotas, JSON e códigos iguais).
- Os testes atuais do servidor (45) e da raiz passam **sem mudança** nos arquivos de teste existentes.
- O servidor inclui **só** o `nucleo` (nem `core`, nem `console`, nem libGDX).
- O build da raiz (`./gradlew build` na raiz) continua igual e continua rodando os testes do `nucleo`.
- O build do servidor **não** roda nem compila os testes do `nucleo` e **não** escreve em `nucleo/build`.
- `cd servidor && docker compose up --build` continua sendo o comando de desenvolvimento.
- Textos, comentários e mensagens de commit em português, no estilo do repo (`feat(servidor): …`, `ci: …`,
  `docs: …`).
- Banco: o esquema da 7.x é exatamente o de `src/test/resources/esquema-fase-7.sql` (tarefa 3), extraído em
  2026-11-26 de um banco gerado pela `main` (`"MODO" ENUM('CLASSICO', 'PILOTAGEM') DEFAULT 'CLASSICO' NOT NULL`).

## Review Focus

- **Banco de produção com linhas reais na primeira subida:** a conversão precisa manter cada valor (`CLASSICO`,
  `VITORIA`, `MEDIO`…), o `DEFAULT 'CLASSICO'` e o `NOT NULL`. Teste na tarefa 3 (linha antiga preservada e
  default conferido).
- **Subir duas vezes sobre o mesmo banco:** o script roda em toda subida e não pode falhar quando as colunas já são
  texto. Teste na tarefa 3 (segunda subida do mesmo arquivo).
- **Banco novo, vazio:** em teste (`create-drop`, memória) e no primeiro deploy de alguém, a tabela ainda não
  existe quando o script roda. Os 45 testes atuais cobrem isso ao passar sem mudança.
- **Contexto do Docker grande demais** (`assets/`, `android/`, `**/build`, `.gradle`): o build fica lento ou leva
  segredo (`.env`) para a imagem. Conferência na tarefa 2 (tamanho do contexto no log e ausência do `.env`).
- **O `bootJar` sem o `nucleo` dentro:** compila, mas quebra ao rodar. Conferência na tarefa 2 (`unzip -l` mostra
  `BOOT-INF/lib/nucleo*.jar`) e o `/actuator/health` da imagem rodando.

---

### Task 1: O build do servidor inclui o `nucleo`

**Files:**
- Modify: `servidor/settings.gradle` (incluir `nucleo` e atualizar o comentário do topo)
- Modify: `servidor/build.gradle` (configurar o `:nucleo` e depender dele)
- Modify: `servidor/src/main/java/br/edu/unifor/missaomarte/servidor/sombras/CanalDeSombras.java:58-60`
- Test: `servidor/src/test/java/br/edu/unifor/missaomarte/servidor/sombras/NucleoNoServidorTeste.java`

**Interfaces:**
- Produces: `implementation project(':nucleo')` no servidor; as classes de `br.edu.unifor.missaomarte.*` do
  `nucleo` ficam disponíveis no código do servidor (as próximas etapas usam `pilotagem.*`).
- `CanalDeSombras.LARGURA_DA_CAVERNA` e `ALTURA_DA_CAVERNA` continuam existindo (`static final double`), agora
  lidos de `Caverna.marciana()`.

- [ ] **Step 1: Escrever o teste que falha**

```java
class NucleoNoServidorTeste {
    @Test
    void oCanalDasSombrasUsaOTamanhoDaCavernaDoNucleo() {
        Caverna caverna = Caverna.marciana();
        assertEquals(caverna.getLargura(), CanalDeSombras.LARGURA_DA_CAVERNA);
        assertEquals(caverna.getAltura(), CanalDeSombras.ALTURA_DA_CAVERNA);
        assertEquals(3840, CanalDeSombras.LARGURA_DA_CAVERNA);
        assertEquals(1440, CanalDeSombras.ALTURA_DA_CAVERNA);
    }
}
```
(no pacote `br.edu.unifor.missaomarte.servidor.sombras`, para ler as constantes de pacote; JUnit puro, sem
Spring)

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd servidor && ./gradlew test --tests '*NucleoNoServidorTeste'`
Expected: FAIL de compilação (`package br.edu.unifor.missaomarte.pilotagem does not exist`).

- [ ] **Step 3: Incluir o `nucleo` no build do servidor**

- `servidor/settings.gradle`: `include 'nucleo'` e `project(':nucleo').projectDir = file('../nucleo')`. O
  comentário do topo passa a dizer que o servidor inclui só o `nucleo` (DA-39).
- `servidor/build.gradle`, configurando o `:nucleo` a partir do servidor (use
  `project(':nucleo') { pluginManager.withPlugin('java') { … } }`, porque o `plugins { id 'java-library' }` do
  próprio `nucleo/build.gradle` só é aplicado quando ele é avaliado):
  - toolchain 17, `repositories { mavenCentral() }`, `options.encoding = 'UTF-8'`, `-Xlint:all`;
  - saída em `rootProject.layout.buildDirectory.dir('nucleo')` (ou seja, `servidor/build/nucleo`);
  - `compileTestJava` e `test` desligados (`enabled = false`): o build da raiz roda os 92 testes do `nucleo`, e o
    JUnit da raiz (5.10.2) não é o do Spring Boot.
- `dependencies { implementation project(':nucleo') }`.
- `CanalDeSombras`: `LARGURA_DA_CAVERNA = Caverna.marciana().getLargura()` e o mesmo para a altura, com um
  comentário dizendo que o tamanho vem do jogo (até a etapa 4, quando cada sala terá o seu).

- [ ] **Step 4: Rodar e ver passar**

Run: `cd servidor && ./gradlew --no-daemon build`
Expected: `BUILD SUCCESSFUL`. O relatório mostra os 45 testes antigos + o novo passando. `ls nucleo/build` na raiz
não mostra nada criado por esse comando (confira o horário antes e depois), e `servidor/build/nucleo/libs/` tem o
jar.

Run (na raiz): `./gradlew --no-daemon :nucleo:test`
Expected: `BUILD SUCCESSFUL`, os 92 testes do `nucleo` passando como antes.

- [ ] **Step 5: Commit**

```bash
git add servidor/settings.gradle servidor/build.gradle servidor/src/main/java/br/edu/unifor/missaomarte/servidor/sombras/CanalDeSombras.java servidor/src/test/java/br/edu/unifor/missaomarte/servidor/sombras/NucleoNoServidorTeste.java
git commit -m "feat(servidor): o build do servidor inclui o nucleo (DA-39)"
```

### Task 2: Docker, compose e workflows com o contexto na raiz

**Files:**
- Modify: `servidor/Dockerfile`
- Create: `servidor/Dockerfile.dockerignore`
- Delete: `servidor/.dockerignore` (passa a valer o `Dockerfile.dockerignore`, que fica ao lado do Dockerfile)
- Modify: `servidor/compose.yaml`
- Modify: `.github/workflows/ci.yml:131-134` (job `imagem`)
- Modify: `.github/workflows/deploy.yml` (passo "Build and push")
- Modify: `servidor/PUBLICACAO.md:103` e o cabeçalho do `servidor/Dockerfile` (o comando de build manual)

**Interfaces:**
- Consumes: o build da tarefa 1 (`servidor/settings.gradle` referencia `../nucleo`).
- Produces: `docker build -f servidor/Dockerfile .` (na raiz) gera a mesma imagem de antes, com o `nucleo` dentro
  do jar; `cd servidor && docker compose up --build` continua funcionando.

- [ ] **Step 1: Reescrever o Dockerfile para a raiz como contexto**

- Estágio de compilação com `WORKDIR /fonte/servidor`. A camada de dependências copia `servidor/gradlew`,
  `servidor/settings.gradle`, `servidor/build.gradle`, `servidor/gradle/` e também
  `nucleo/build.gradle → /fonte/nucleo/build.gradle`, antes do `./gradlew dependencies`. Depois copia
  `servidor/src` e `nucleo/src/main`, e roda o `bootJar`.
- O estágio final continua igual, mas copia `servidor/downloads` (o caminho a partir da raiz) para `/app/downloads`.
- O comentário do topo mostra o comando novo: `docker build -f servidor/Dockerfile -t missao-marte-servidor .`
  (na raiz).

- [ ] **Step 2: Criar o `servidor/Dockerfile.dockerignore`**

Ele começa negando tudo (`*`) e libera só o necessário: `servidor/gradlew`, `servidor/gradle/`,
`servidor/settings.gradle`, `servidor/build.gradle`, `servidor/src/`, `servidor/downloads/`, `nucleo/build.gradle`
e `nucleo/src/main/`. Depois volta a excluir `**/build`, `**/.gradle`, `servidor/.env` e `servidor/dados`. Apague
o `servidor/.dockerignore` antigo.

- [ ] **Step 3: Compose e workflows**

- `servidor/compose.yaml`: `build: { context: .., dockerfile: servidor/Dockerfile }`. Os caminhos do
  `develop.watch` são `./src`, `./build.gradle` e `../nucleo/src/main`.
- `ci.yml` (job `imagem`) e `deploy.yml` ("Build and push"): troque o último argumento `servidor` por
  `-f servidor/Dockerfile .`. O resto dos comandos não muda (o `download-artifact` continua em
  `servidor/downloads`).
- `PUBLICACAO.md:103`: o comando novo de build manual.

- [ ] **Step 4: Construir e conferir a imagem**

Run (na raiz): `docker buildx build --progress plain --load -f servidor/Dockerfile -t missao-marte-servidor:etapa1 . 2>&1 | tee /tmp/etapa1-docker.log`
Expected: termina sem erro. A linha `transferring context` mostra poucos MB (não as dezenas de `assets/` e
`android/`).

Run: `docker run --rm --entrypoint sh missao-marte-servidor:etapa1 -c 'ls /app; unzip -l /app/app.jar 2>/dev/null | grep -c nucleo || (cd /tmp && jar tf /app/app.jar | grep nucleo)'`
Expected: `/app` sem `.env`, e o jar lista `BOOT-INF/lib/nucleo…jar`. Se a imagem não tiver `unzip` nem `jar`,
faça a mesma conferência com `unzip -l servidor/build/libs/missao-marte-servidor.jar | grep nucleo` no host.

Run: `docker run -d --rm -p 18081:8080 -e MISSAOMARTE_JWT_SEGREDO=segredo-local-com-32-bytes-ou-mais-aqui --name etapa1 missao-marte-servidor:etapa1 && sleep 25 && curl -s localhost:18081/actuator/health; docker stop etapa1`
Expected: `{"status":"UP",…}`.

Run: `cd servidor && docker compose config | grep -A3 'build:'`
Expected: `context` apontando para a raiz do repo e `dockerfile: servidor/Dockerfile`.

- [ ] **Step 5: Commit**

```bash
git add servidor/Dockerfile servidor/Dockerfile.dockerignore servidor/compose.yaml .github/workflows/ci.yml .github/workflows/deploy.yml servidor/PUBLICACAO.md
git rm servidor/.dockerignore
git commit -m "ci: imagem do servidor construída a partir da raiz, com o nucleo"
```

### Task 3: Colunas de enum como texto, com a conversão do banco existente

**Files:**
- Modify: `servidor/src/main/java/br/edu/unifor/missaomarte/servidor/partida/Partida.java` (campos `resultado`,
  `dificuldade` e `modo`)
- Create: `servidor/src/main/resources/migracoes/enums-para-texto.sql`
- Modify: `servidor/src/main/resources/application.properties` (inicialização SQL)
- Create: `servidor/src/test/resources/esquema-fase-7.sql`
- Test: `servidor/src/test/java/br/edu/unifor/missaomarte/servidor/partida/MigracaoDoEsquemaTeste.java`

**Interfaces:**
- Produces: as colunas `PARTIDAS.RESULTADO`, `PARTIDAS.DIFICULDADE` e `PARTIDAS.MODO` como
  `CHARACTER VARYING(20)` em bancos novos e convertidos. A regra para as próximas etapas: **toda** coluna
  `@Enumerated(EnumType.STRING)` nova também leva `@JdbcTypeCode(SqlTypes.VARCHAR)` e `length = 20`.

- [ ] **Step 1: A cópia do esquema da 7.x para o teste**

`esquema-fase-7.sql` = as instruções `CREATE TABLE`, `ALTER TABLE … ADD CONSTRAINT` e `CREATE INDEX` de
`JOGADORES` e `PARTIDAS` exatamente como o H2 as gerou na `main` (o `SCRIPT NODATA` de 2026-11-26), **sem** o
`CREATE USER`, mais estes dados:
- `INSERT INTO JOGADORES(CRIADO_EM, NOME, SENHA_HASH) VALUES (CURRENT_TIMESTAMP, 'Veterana', 'x');`
- uma partida dela sem a coluna `MODO` (para valer o default): `RESULTADO='VITORIA'`, `DIFICULDADE='MEDIO'`,
  `PONTOS=50`, `PASSAGEIROS=3`, `MOVIMENTOS=12`, `TEMPO_SEGUNDOS=40`, `REGISTRADA_EM=CURRENT_TIMESTAMP`;
- uma partida `MODO='PILOTAGEM'`, `RESULTADO='VITORIA'`, `DIFICULDADE='DIFICIL'`, `PONTOS=60`.

Na criação, `"MODO" ENUM('CLASSICO', 'PILOTAGEM') DEFAULT 'CLASSICO' NOT NULL` (copiado do dump, não reescrito à
mão).

- [ ] **Step 2: Escrever o teste que falha**

`@SpringBootTest` com `@AutoConfigureMockMvc` e um `@DynamicPropertySource` estático, que:
1. cria um arquivo H2 novo numa pasta temporária (`Files.createTempDirectory`);
2. roda o `esquema-fase-7.sql` nele por JDBC puro, antes de o contexto subir;
3. aponta `spring.datasource.url` para esse arquivo, com `spring.jpa.hibernate.ddl-auto=update` e
   `missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais`.

Testes:

```java
@Test void asColunasDeEnumViramTexto()            // INFORMATION_SCHEMA.COLUMNS: DATA_TYPE = 'CHARACTER VARYING' para MODO, RESULTADO e DIFICULDADE
@Test void asPartidasAntigasContinuamIguais()     // as 2 linhas: ('CLASSICO','VITORIA','MEDIO',50) e ('PILOTAGEM','VITORIA','DIFICIL',60)
@Test void oDefaultEONotNullDoModoContinuam()     // COLUMN_DEFAULT de MODO contém 'CLASSICO' e IS_NULLABLE = 'NO'
@Test void umModoNovoJaCabeNaColuna()             // INSERT por JDBC com MODO='HORDA' não lança exceção
@Test void oRankingDaApiContinuaIgual()           // GET /api/ranking → [0].nome = "Veterana", [0].pontos = 50; GET /api/ranking?modo=PILOTAGEM → [0].pontos = 60
@Test void oScriptPodeRodarDeNovo()               // rodar migracoes/enums-para-texto.sql de novo por JDBC no mesmo banco não lança exceção
```
O teste não estende `ApiTeste`: o `ApiTeste` usa banco em memória com `create-drop`.

- [ ] **Step 3: Rodar e ver falhar**

Run: `cd servidor && ./gradlew test --tests '*MigracaoDoEsquemaTeste'`
Expected: `asColunasDeEnumViramTexto` e `umModoNovoJaCabeNaColuna` falham (o tipo continua `ENUM`, e o `HORDA` é
recusado com `Value not permitted` ou parecido). Os outros podem passar.

- [ ] **Step 4: Implementar**

- `Partida`: nos três campos de enum, `@JdbcTypeCode(SqlTypes.VARCHAR)` e `@Column(…, length = 20)`. Mantenha o
  `nullable = false` e o `@ColumnDefault("'CLASSICO'")` do `modo`.
- `migracoes/enums-para-texto.sql`, uma linha por coluna, com comentário em português explicando a DA-40:
  `ALTER TABLE IF EXISTS PARTIDAS ALTER COLUMN IF EXISTS MODO SET DATA TYPE CHARACTER VARYING(20);`, e o mesmo
  para `RESULTADO` e `DIFICULDADE`.
- `application.properties`:
  - `spring.sql.init.mode=always`;
  - `spring.sql.init.schema-locations=classpath:migracoes/enums-para-texto.sql`;
  - um comentário dizendo que o script roda antes do Hibernate (`spring.jpa.defer-datasource-initialization`
    continua `false`).
- Se o `SET DATA TYPE` do H2 perder o default ou o `NOT NULL` (o teste do Step 2 mostra), acrescente ao script
  `ALTER TABLE IF EXISTS PARTIDAS ALTER COLUMN IF EXISTS MODO SET DEFAULT 'CLASSICO';` e o `SET NOT NULL` das três.

- [ ] **Step 5: Rodar e ver passar**

Run: `cd servidor && ./gradlew --no-daemon build`
Expected: `BUILD SUCCESSFUL`. Os 6 testes novos, o da tarefa 1 e os 45 antigos passam, sem mudar nenhum arquivo de
teste existente.

- [ ] **Step 6: Commit**

```bash
git add servidor/src/main/java/br/edu/unifor/missaomarte/servidor/partida/Partida.java servidor/src/main/resources/migracoes/enums-para-texto.sql servidor/src/main/resources/application.properties servidor/src/test/resources/esquema-fase-7.sql servidor/src/test/java/br/edu/unifor/missaomarte/servidor/partida/MigracaoDoEsquemaTeste.java
git commit -m "fix(servidor): colunas de enum como texto, com a conversão do banco existente (DA-40)"
```

### Task 4: Decisões, documentação e evidência da etapa

**Files:**
- Modify: `docs/decisoes-arquiteturais.md` (DA-39 e DA-40 no fim; DA-14 e DA-16 marcadas como substituídas em
  parte)
- Modify: `README.md:141-142` (só se o comentário do `--watch` precisar citar o `nucleo`)
- Create: `docs/evidencias/fase-08/etapa-1-servidor-com-nucleo.txt`

**Interfaces:**
- Consumes: as tarefas 1 a 3 prontas.

- [ ] **Step 1: Registrar as decisões**

DA-39 e DA-40 no formato do arquivo (título com a data, **Contexto**, **Decisão** e **Consequências**), com o
texto da seção 10 da análise. Abaixo do título da DA-14 e da DA-16, uma linha: "Substituída em parte pela DA-39
(2026-11-26): o servidor inclui o `nucleo`, e só ele."

- [ ] **Step 2: Executar de verdade a migração num banco gerado pela 7.x**

1. Num worktree temporário da `main`: `git worktree add /tmp/mm-main origin/main`.
2. Subir o servidor 7.x num banco novo: `MISSAOMARTE_DADOS=/tmp/mm-db/missaomarte PORT=18082 ./gradlew bootRun`.
3. Pela API, criar uma conta e registrar uma vitória do Clássico e uma da Pilotagem. Guardar a saída de
   `GET /api/ranking` e de `GET /api/ranking?modo=PILOTAGEM`, e parar o servidor.
4. Subir a versão desta branch sobre o **mesmo** arquivo, na porta 18083.
5. Repetir os dois `GET`: a resposta precisa ser idêntica.
6. Conferir os tipos com o `org.h2.tools.Shell`:
   `select column_name, data_type from information_schema.columns where table_name='PARTIDAS'`.
7. Parar e subir de novo, para provar a segunda subida.
8. Remover o worktree temporário.

- [ ] **Step 3: Escrever a evidência**

`docs/evidencias/fase-08/etapa-1-servidor-com-nucleo.txt`, no estilo de `docs/evidencias/fase-07/`:
- os comandos e os resultados das tarefas 1 a 3: build do servidor com N testes, `:nucleo:test` da raiz, build e
  saúde da imagem, tamanho do contexto e jar com o `nucleo`;
- os tipos antes e depois;
- os rankings iguais antes e depois da migração;
- a segunda subida.

Nada de segredo nem de token no arquivo.

- [ ] **Step 4: Build completo na raiz**

Run (na raiz): `./gradlew --no-daemon build`
Expected: `BUILD SUCCESSFUL`. O jogo não mudou, e os testes de `nucleo`, `core`, `console` e `lwjgl3` passam como
antes.

- [ ] **Step 5: Commit**

```bash
git add docs/decisoes-arquiteturais.md docs/evidencias/fase-08/etapa-1-servidor-com-nucleo.txt
git commit -m "docs(fase-8): DA-39 e DA-40 e a evidência da etapa 1"
```

### Antes do merge (fora das tarefas)

- Pedir ao usuário um **backup do volume `/dados`** do Dokploy antes de mergear: a primeira subida converte as
  colunas do banco de produção.
- Abrir o PR `fase-8-etapa-1` para a `main`: o CI roda os jobs `servidor`, `jogo` e `imagem`, e o último prova o
  build com o contexto na raiz no GitHub.
