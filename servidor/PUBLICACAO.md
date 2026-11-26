# Publicação do servidor de ranking

Como colocar o servidor (`servidor/`) num computador ou serviço acessível pela internet, para que o jogo no celular
e no desktop use o ranking global de qualquer lugar. A imagem é a mesma do desenvolvimento (`Dockerfile` desta
pasta). Só mudam as variáveis de ambiente e o que fica na frente dela (HTTPS).

Este guia **não** publica nada sozinho. Ele descreve o que a plataforma de hospedagem (Dokploy, Coolify, Render,
um servidor com Docker etc.) precisa receber. O que foi testado está marcado no fim. A publicação real, no Dokploy
com GitHub Actions, está na [seção 10](#10-publicação-no-dokploy-com-github-actions).

## 1. O que é publicado

| Item | Valor |
|---|---|
| Imagem | construída a partir de `servidor/Dockerfile` (dois estágios: JDK 17 compila, JRE 17 roda), cerca de 577 MB |
| Processo | `java -jar /app/app.jar`, usuário `missao` (sem privilégios) |
| Porta do contêiner | `8080` (mude com `PORT`, se a plataforma exigir outra) |
| Protocolo | HTTP simples. O HTTPS fica no proxy da frente (seção 5) |
| Dados | PostgreSQL (`MISSAOMARTE_DB_*`), com o esquema nas migrações do Flyway (Fase 8, DA-41). Sem `MISSAOMARTE_DB_URL`, um H2 em `/dados/missaomarte.mv.db`, só para testes locais |
| Saúde | `GET /actuator/health` → `200 {"status":"UP"}`; também `/actuator/health/liveness` e `/actuator/health/readiness` |

## 2. Variáveis de ambiente

| Variável | Obrigatória | Para quê | Padrão |
|---|---|---|---|
| `MISSAOMARTE_JWT_SEGREDO` | **sim** | segredo que assina os tokens (HS256), com 32 bytes ou mais. Gere com `openssl rand -base64 48` e guarde só nos segredos da plataforma | temporário a cada início (os tokens deixam de valer quando o servidor reinicia) |
| `MISSAOMARTE_HORDA_SEGREDO` | recomendada | segredo HMAC de 32 bytes ou mais para o sorteio das próximas hordas e agendas. Gere um valor independente do JWT com `openssl rand -base64 48` e use o mesmo nas réplicas | temporário a cada subida, com aviso no log. Hordas já publicadas são relidas do banco |
| `MISSAOMARTE_ADMINS` | para usar o painel | nomes dos jogadores autorizados, separados por vírgula, sem distinguir maiúsculas. Cadastre as contas antes de incluir os nomes na variável. Enquanto um nome estiver livre, qualquer pessoa pode cadastrá-lo | vazio: nenhum administrador |
| `MISSAOMARTE_URL_PUBLICA` | recomendada | endereço público, por exemplo `https://ranking.exemplo.com`. É o que o "Try it out" do Swagger usa | `http://localhost:8080` |
| `MISSAOMARTE_DB_URL` | **sim** | o PostgreSQL, `jdbc:postgresql://<host-interno-do-postgres>:5432/missaomarte` (seção 10) | vazia: H2 em `MISSAOMARTE_DADOS` |
| `MISSAOMARTE_DB_USUARIO` | **sim** | o usuário do banco do jogo (`missaomarte`), nunca o administrador da instância | `sa` (o do H2) |
| `MISSAOMARTE_DB_SENHA` | **sim** | a senha desse usuário. Guarde só nos segredos da plataforma | vazia |
| `MISSAOMARTE_IMPORTAR_H2` | só na virada | o banco H2 da Fase 7, sem a extensão `.mv.db` (`/dados/missaomarte`). É copiado para o PostgreSQL na subida, se este estiver vazio (seção 10) | vazia: não importa nada |
| `MISSAOMARTE_DADOS` | não | caminho do H2, sem a extensão `.mv.db`. Só vale sem `MISSAOMARTE_DB_URL` | `/dados/missaomarte` na imagem |
| `MISSAOMARTE_PERMITIR_H2` | não | `false` na imagem: sem `MISSAOMARTE_DB_URL`, o servidor recusa subir, em vez de abrir ou criar um H2 (DA-41). Fora da imagem, o H2 continua permitido | `false` na imagem |
| `MISSAOMARTE_AJUSTE_*` | não | ajustes do jogo: pouso, tanque, asteroides, meteoros e duração dos eventos (seção 12, DA-48) | os da Fase 8 |
| `PORT` | não | porta HTTP dentro do contêiner | `8080` |
| `SPRINGDOC_APIDOCS_ENABLED` e `SPRINGDOC_SWAGGERUI_ENABLED` | não | `false` desliga a documentação (`/v3/api-docs` e `/swagger-ui.html` passam a dar 404). Ligada, ela não expõe dados, só o contrato | ligadas |
| `JAVA_TOOL_OPTIONS` | não | opções da JVM, por exemplo `-XX:MaxRAMPercentage=75` para usar mais da memória do contêiner | padrão da JVM (25% da memória do contêiner) |

Regras:
- **Nunca** ponha o segredo nem a senha do banco no Git, na imagem ou num `docker-compose` versionado. O `.env`
  desta pasta é só para desenvolvimento e está no `.gitignore`.
- **Trocar o segredo do JWT** invalida todos os tokens: cada jogador só precisa entrar de novo (o jogo recebe 401, apaga a
  sessão guardada e volta ao modo convidado). As contas e as partidas continuam.
- Os tokens valem 30 dias (`missaomarte.jwt.validade=P30D` em `application.properties`), porque o jogo guarda a
  sessão no aparelho (DA-38).

## 3. Persistência e cópia de segurança

Desde a Fase 8 (DA-41), os dados ficam num **PostgreSQL** (a versão 18 na publicação). O contêiner do servidor não
guarda nada: pode ser recriado à vontade.

- **O esquema** vem das migrações do Flyway (`src/main/resources/db/migration`, `V1__esquema_inicial.sql`,
  `V2__...`). Na subida, o servidor aplica as que faltam (o histórico fica na tabela `flyway_schema_history`), e o
  Hibernate só confere que as entidades batem com as tabelas (`ddl-auto=validate`). Se não batem, o servidor não
  sobe.
- **O banco do jogo** tem dono próprio: o usuário `missaomarte`, dono do banco `missaomarte` e de nada mais (seção
  10). O Flyway cria as tabelas no schema `public` desse banco, como dono.
- **Cópia de segurança** com o `pg_dump`, com o servidor no ar:

  ```bash
  # no computador que roda o contêiner do PostgreSQL; o arquivo fica nesse computador
  docker exec <contêiner-do-postgres> pg_dump -U missaomarte -d missaomarte -Fc > missaomarte-$(date +%F).dump
  ```

  Para restaurar, com o servidor parado:
  `docker exec -i <contêiner-do-postgres> pg_restore -U missaomarte -d missaomarte --clean --if-exists < arquivo.dump`.
  No compose de desenvolvimento: `docker compose exec -T postgres pg_dump -U missaomarte -d missaomarte -Fc > ...`.
- **Atualizar uma versão já publicada:** as migrações novas rodam sozinhas na subida. Faça uma cópia antes de
  publicar uma versão com migração nova. Na Fase 8 (etapa 2a), uma migração cria as tabelas `moedas`, `bilhetes` e
  `skins_do_jogador` e a coluna `skin` dos jogadores, com o padrão `padrao`. Quem já tinha conta fica com a nave de
  sempre e saldo zero, porque as vitórias antigas não rendem moedas.
- **Sem `MISSAOMARTE_DB_URL`** (testes locais), o servidor usa um H2 em `/dados/missaomarte.mv.db`. Nesse caso:
  - monte um volume em `/dados`;
  - use uma instância só, porque o H2 trava o arquivo;
  - para a cópia, pare o servidor e copie o `.mv.db`.

  Um H2 criado antes da Fase 8 não abre nesse modo (`Schema "PUBLIC" not found`): apague-o ou importe-o
  (`MISSAOMARTE_IMPORTAR_H2`, seção 10).

## 4. Verificação de saúde

A imagem já tem um `HEALTHCHECK`: a cada 15 s, o próprio bash pede `/actuator/health` e procura `"UP"`. A
plataforma pode usar o mesmo caminho:

| Uso | Caminho | Resposta esperada |
|---|---|---|
| saúde geral (o banco incluído) | `/actuator/health` | `200` com `"status":"UP"` |
| o processo está vivo | `/actuator/health/liveness` | `200` |
| pronto para receber pedidos | `/actuator/health/readiness` | `200` |

O servidor leva cerca de 10 a 15 s para subir: use um período inicial de pelo menos 40 s, como o `HEALTHCHECK`.

## 5. HTTPS (proxy reverso)

O servidor fala HTTP. Na internet, ponha na frente dele um proxy que cuide do certificado e do HTTPS e repasse os
pedidos para a porta 8080 do contêiner. Exemplos: o Traefik que o Dokploy e o Coolify já trazem, o Caddy ou o
Nginx.

- Domínio, por exemplo `ranking.exemplo.com`, apontando para o proxy; certificado automático (Let's Encrypt).
- Destino: o contêiner, na porta `8080`.
- `MISSAOMARTE_URL_PUBLICA=https://ranking.exemplo.com`, para o Swagger usar o endereço certo.
- O jogo não usa cookies nem é uma página web: não é preciso configurar CORS.
- **WebSocket (modo shadow, Fase 7):** o proxy precisa repassar o `Upgrade: websocket` de `/ws/sombras`. O Traefik
  (Dokploy, Coolify) e o Caddy fazem isso sozinhos; no Nginx, a `location` precisa de `proxy_http_version 1.1`,
  `proxy_set_header Upgrade $http_upgrade` e `proxy_set_header Connection "upgrade"`. Com HTTPS no proxy, o jogo
  usa `wss://` sozinho (vem do `https://` da opção 4). Teste: sem token, o aperto de mão responde `401`.

Por que HTTPS: o login manda a senha, e cada pedido manda o token. Em HTTP simples, qualquer um na mesma rede
(um Wi-Fi público, por exemplo) consegue ler os dois. Na rede de casa ou do laboratório, durante as aulas, HTTP é
aceitável. É por isso que o APK aceita `http://` (`usesCleartextTraffic`, DA-22).

## 6. Apontar o jogo para o servidor publicado

- **Celular:** na tela de entrada, toque em **4. Servidor**, digite `https://ranking.exemplo.com` e confirme. O
  endereço fica guardado no aparelho.
- **Desktop:** a mesma opção 4 (tecla `4`), ou `./gradlew :lwjgl3:run -Dmissaomarte.servidor=https://ranking.exemplo.com`.
  Passado na linha de comando, o endereço tem prioridade e não pode ser trocado na tela.

## 7. Lista de verificação

1. A imagem foi construída na raiz do repositório (`docker build -f servidor/Dockerfile -t missao-marte-servidor .`, porque o servidor compila o `nucleo`, DA-39) ou pela plataforma.
2. `MISSAOMARTE_JWT_SEGREDO` está definido nos segredos da plataforma (32 bytes ou mais).
3. `MISSAOMARTE_DB_URL`, `MISSAOMARTE_DB_USUARIO` e `MISSAOMARTE_DB_SENHA` apontam para o banco do jogo, e o log
   da subida mostra o Flyway em `PostgreSQL`. Uma réplica só (por causa do modo shadow, seção 8).
4. A verificação de saúde usa `/actuator/health`, com período inicial de 40 s ou mais.
5. O proxy com HTTPS aponta para a porta 8080, e `MISSAOMARTE_URL_PUBLICA` tem o endereço `https://` público.
6. `curl https://ranking.exemplo.com/actuator/health` responde `{"status":"UP",...}`, e
   `curl https://ranking.exemplo.com/api/ranking` responde uma lista (vazia no começo).
7. No jogo: opção 4 com o endereço, criar uma conta, vencer uma partida e conferir o ranking global.
8. Recrie o contêiner (nova versão) e confira que a conta e o ranking continuam lá.

## 8. Limitações conhecidas

- O servidor guarda a pontuação que o cliente envia e **não confere se ela é legítima** (Fase 5). Um cliente
  modificado poderia enviar qualquer valor dentro dos limites da validação.
- Não há limite de tentativas de login (rate limiting). Atrás de um proxy, ele pode ser configurado no próprio
  proxy.
- O banco é o PostgreSQL desde a Fase 8 (DA-41), então ele não impede mais duas réplicas. Quem impede é o modo
  shadow (abaixo).
- A imagem é grande (cerca de 577 MB, quase tudo do JRE em Ubuntu). Uma base menor fica como melhoria.
- As conexões do modo shadow ficam na memória da instância: com mais de uma réplica, cada piloto só veria os que
  caíram na mesma. É por isso que o serviço tem uma réplica só (para mais, seria preciso um repasse entre instâncias,
  como Redis).

## 9. O que foi testado (Fase 6, 2026-11-18)

Testado localmente com Docker, sem publicar em nenhum serviço:
- construção da imagem;
- contêiner `healthy`, com `/actuator/health`, `/liveness` e `/readiness` em 200;
- dados mantidos depois de recriar o contêiner;
- `MISSAOMARTE_URL_PUBLICA` mudando o endereço no `/v3/api-docs` (com um teste automático:
  `DocumentacaoDaApiTeste`);
- `SPRINGDOC_*_ENABLED=false` desligando a documentação (404), com a API e a saúde em 200;
- o comando de cópia de segurança da seção 3 (servidor parado, arquivo copiado, servidor de volta `UP`);
- um celular de verdade na mesma rede usando o servidor pelo IP do computador.

Não testados: um proxy HTTPS de verdade, um domínio público e a imagem em processadores amd64. Os testes rodaram
num Mac com processador ARM.

## 10. Publicação no Dokploy com GitHub Actions

No ar em **https://missao-marte.thebylito.dev**: um host só para tudo. O mesmo contêiner entrega:

| Caminho | O quê |
|---|---|
| `/` | página do jogo: ranking global ao vivo (os dois modos) e os downloads |
| `/downloads/missao-marte.apk` | APK para Android 8 ou mais novo |
| `/downloads/missao-marte-windows.zip` | Windows, com o Java embutido: descompactar e abrir, sem instalar nem ser administrador |
| `/downloads/missao-marte-macos-apple-silicon.zip`, `/downloads/missao-marte-macos-intel.zip` | macOS, com o Java embutido (`Missao Marte.app`, assinatura ad hoc) |
| `/downloads/missao-marte-desktop.zip` | jogo desktop para quem já tem o Java 17 (Linux e outros sistemas) |
| `/api/**`, `/ws/sombras` | a API e o modo shadow, como antes |
| `/swagger-ui.html`, `/actuator/health` | documentação e saúde |

A página lê `/api/ranking` na mesma origem, por isso não precisa de CORS. Os nomes dos jogadores entram nela só como
texto (`textContent`). O APK e o jogo desktop baixados já vêm apontando para o servidor publicado: o build embute o
endereço com `-Pmissaomarte.servidorPadrao=https://missao-marte.thebylito.dev`. Sem essa propriedade, o
desenvolvimento continua com `localhost` e `10.0.2.2`. O jogador ainda pode trocar o endereço na opção 4.

### Como a entrega acontece

Quem faz o build é o GitHub Actions. O Dokploy só implanta a imagem pronta, no mesmo fluxo do `portfolio-coletivo`:

1. **`CI`** (`.github/workflows/ci.yml`), em cada PR:
   - testes do servidor (`servidor/`);
   - testes do jogo, o APK e o zip desktop com o endereço publicado (artefato `downloads`);
   - os pacotes nativos com o Java embutido (`jpackage`, `./gradlew :lwjgl3:pacoteNativo`), um por máquina: Windows
     (`windows-latest`), Mac Apple Silicon (`macos-15`) e Mac Intel (`macos-15-intel`) (artefatos `pacote-*`);
   - a imagem é construída, mas não é publicada.
2. **`Build & Deploy`** (`.github/workflows/deploy.yml`), em cada merge na `main` que mexe no jogo ou no servidor:
   - roda o mesmo CI;
   - coloca o APK, o zip e os pacotes nativos em `servidor/downloads/` e gera a imagem;
   - publica `ghcr.io/thebylito/missao-marte-unifor-servidor:sha-<commit>` (e `:latest`);
   - chama o Dokploy (`nodilabs/dokploy-deploy-action`) para implantar essa tag.

Enquanto os secrets do Dokploy não existirem, o workflow só publica a imagem e avisa que o deploy foi ignorado. Para
reimplantar sem novo commit: aba **Actions → Build & Deploy → Run workflow**.

### Configuração (uma vez só)

**No Dokploy** (projeto `unifor`, ambiente `production`, serviço `missao-marte-api`, servidor "AWS - Matheus"):
- Provider **Docker**, imagem `ghcr.io/thebylito/missao-marte-unifor-servidor:latest`. O pacote do GHCR precisa
  ser público (ou o registry `ghcr.io` cadastrado com um token de leitura).
- Environment:
  - `MISSAOMARTE_URL_PUBLICA=https://missao-marte.thebylito.dev`
  - `MISSAOMARTE_JWT_SEGREDO` com o valor de `openssl rand -base64 48`. **Quem preenche é o dono do painel**, e o
    valor nunca vai para o Git.
  - Desde a Fase 8: `MISSAOMARTE_DB_URL`, `MISSAOMARTE_DB_USUARIO` e `MISSAOMARTE_DB_SENHA` (a virada, abaixo).
  - Para Hordas e eventos: `MISSAOMARTE_HORDA_SEGREDO` e `MISSAOMARTE_ADMINS`, conforme a seção 2.
- Domain `missao-marte.thebylito.dev` → porta **8080**, em HTTP. O HTTPS fica com a Cloudflare, na frente, como nos
  outros serviços.
- Uma réplica só (modo shadow).
- Até a Fase 7, o banco era um H2 no volume `missao-marte-api-dados`, montado em `/dados`, com
  `MISSAOMARTE_DADOS=/dados/missaomarte`.
  - Advanced → Swarm Settings → Update Config estava em **Order = Stop First**. O padrão do Swarm (start-first) sobe
    o contêiner novo antes de parar o antigo, e o H2 só abre o arquivo em um processo. Então o novo morria com
    `The file is locked: /dados/missaomarte.mv.db`, e a versão antiga continuava no ar. Isso aconteceu no primeiro
    deploy automático (2026-11-25).
  - Com stop-first, cada deploy deixava o site fora do ar por uns 20 segundos. A virada para o PostgreSQL acaba com
    isso (passo 5).

**No GitHub** (Settings → Secrets and variables → Actions):
- `DOKPLOY_SERVER_URL`: URL do painel do Dokploy
- `DOKPLOY_API_KEY`: chave de API do Dokploy (Profile → API/CLI no painel)
- `DOKPLOY_APPLICATION_IDS`: ID do serviço `missao-marte-api` (o último trecho da URL dele no painel)

Limitação: o APK é o de depuração, assinado com a chave que o runner gera a cada build. Uma versão nova não
atualiza a antiga por cima: o jogador desinstala e instala de novo. Para atualizações sem desinstalar, cadastre um
keystore fixo como secret e assine o APK de release com ele.

### Virada para o PostgreSQL (Fase 8, DA-41)

A produção passa do H2 do volume `/dados` para o banco `missaomarte` da instância geral de PostgreSQL 18 do Dokploy:
o serviço `postgres-br`, no projeto `databases`, compartilhado com outros projetos. Os jogadores, as senhas
(o hash) e as partidas vêm junto, com os mesmos ids: ninguém precisa criar a conta de novo. O jogo instalado não muda.
Os tokens continuam valendo, se o `MISSAOMARTE_JWT_SEGREDO` for o mesmo.

**0. Antes de tudo: cópias.** Uma cópia do volume `missao-marte-api-dados` (o arquivo `missaomarte.mv.db`, como na
seção 3, com o servidor parado ou pelo backup de volumes do Dokploy). Guarde também o ranking de agora, para
comparar no passo 4:

```bash
curl -s https://missao-marte.thebylito.dev/api/ranking > ranking-classico-antes.json
curl -s "https://missao-marte.thebylito.dev/api/ranking?modo=PILOTAGEM" > ranking-pilotagem-antes.json
```

**1. O banco do jogo na instância geral (uma vez só).** No painel do Dokploy, abra o serviço `postgres-br` (projeto
`databases`). Na aba do terminal do contêiner, entre como administrador com `psql -U admin` e rode, trocando
`<senha>` por uma senha nova, só do jogo (por exemplo, a saída de `openssl rand -base64 32`):

```sql
CREATE ROLE missaomarte LOGIN PASSWORD '<senha>';
CREATE DATABASE missaomarte OWNER missaomarte;
REVOKE ALL ON DATABASE missaomarte FROM PUBLIC;
```

- O usuário `missaomarte` é dono só do banco `missaomarte`. O Flyway cria as tabelas no schema `public` desse banco,
  como dono, na primeira subida. O servidor nunca usa o `admin`.
- O `REVOKE` impede que os usuários dos outros projetos da instância se conectem ao banco do jogo. Cada banco novo
  da instância deve fazer o mesmo, para que o `missaomarte` também não se conecte ao dos outros.
- A instância fica só na rede interna do Dokploy, sem porta publicada na internet. O servidor do jogo a alcança pelo
  host interno do serviço.

O comando foi conferido num PostgreSQL 18.7 local (2026-11-26):
- o servidor subiu com o usuário `missaomarte`, e as três tabelas ficaram com esse dono;
- o `missaomarte` não conseguiu criar tabela no banco de outro projeto (`permission denied for schema public`);
- depois do `REVOKE` daquele banco, o `missaomarte` não conseguiu nem se conectar a ele.

**2. As variáveis no serviço `missao-marte-api`** (Environment), **antes do merge**. O merge na `main` dispara o
deploy. Sem `MISSAOMARTE_DB_URL`, a versão nova abre o H2 de `/dados` em modo PostgreSQL e não sobe (`Schema "PUBLIC"
not found`). Os dados não se perdem, mas, com o stop-first, o site fica fora do ar.

```
MISSAOMARTE_DB_URL=jdbc:postgresql://databases-postgres-br-an2isi:5432/missaomarte
MISSAOMARTE_DB_USUARIO=missaomarte
MISSAOMARTE_DB_SENHA=<a senha do passo 1>
MISSAOMARTE_IMPORTAR_H2=/dados/missaomarte
```

- O host `databases-postgres-br-an2isi` é o host interno que o painel mostra no serviço `postgres-br`. Confira lá
  antes: ele muda se o serviço for recriado.
- `MISSAOMARTE_JWT_SEGREDO`, `MISSAOMARTE_URL_PUBLICA`, o volume `/dados` e o stop-first ficam como estão. A
  `MISSAOMARTE_DADOS` pode ficar: com `MISSAOMARTE_DB_URL`, ela é ignorada.

**3. O deploy, ainda stop-first.** Faça o merge (ou **Actions → Build & Deploy → Run workflow**). O stop-first para o
contêiner antigo antes de subir o novo, e isso solta a trava do arquivo H2. Na subida, o servidor novo:
- aplica as migrações do Flyway no banco vazio;
- copia o H2 (aberto só para leitura: o arquivo não muda);
- só então começa a atender.

Se o servidor antigo ainda estiver rodando, o novo não sobe, com a mensagem `não deu para abrir /dados/missaomarte.mv.db
só para leitura ... o deploy da virada precisa ser stop-first`, e o PostgreSQL fica vazio. Se a cópia falhar no
meio, ela é desfeita, e o banco também fica vazio. Nos dois casos, corrija e faça o deploy de novo.

**4. Conferir.** No log do serviço, procure:

```
Migrating schema "public" to version "1 - esquema inicial"
... migrações seguintes ...
Importação do H2 /dados/missaomarte.mv.db: <N> jogadores e <M> partidas copiados, com os mesmos ids; próximos ids: jogador <N+1>, partida <M+1>
Tomcat started on port 8080
```

Depois:
- compare o ranking com o do passo 0:
  `curl -s https://missao-marte.thebylito.dev/api/ranking | diff ranking-classico-antes.json -`, e o mesmo com
  `?modo=PILOTAGEM`. Não deve sair nada;
- confira as contagens no terminal do `postgres-br`:
  `psql -U missaomarte -d missaomarte -c "select (select count(*) from jogadores) as jogadores, (select count(*) from partidas) as partidas"`;
- entre no jogo com uma conta antiga e confira o histórico.

**5. Voltar o Swarm para start-first.** No serviço `missao-marte-api`, abra Advanced → Swarm Settings → Update Config e
escolha **Order = Start First**. Com o PostgreSQL, a versão nova e a antiga podem ficar conectadas ao mesmo tempo, e
os próximos deploys deixam de tirar o site do ar. Enquanto `MISSAOMARTE_IMPORTAR_H2` existir, cada subida vê o banco
com dados e escreve no log `o banco novo já tem dados ...; nada importado`, sem abrir o H2.

**6. Logo depois da conferência do passo 4:** remova `MISSAOMARTE_IMPORTAR_H2`. Ela só serve na primeira subida e,
se ficar, é o único caminho para uma reimportação antiga num banco recriado por engano.

**7. Depois de alguns dias** com tudo certo:
- remova `MISSAOMARTE_DADOS`;
- desmonte o volume `/dados` (Advanced → Volumes);
- guarde a cópia do passo 0;
- só então apague o volume `missao-marte-api-dados`.

**Voltar atrás.** Se algo der errado depois da virada:
- reimplante a imagem que estava em produção **antes** da virada (anote a tag `sha-<commit>` no passo 0; não é a da
  7.x), com o stop-first, o volume `/dados` e as variáveis de antes (sem `MISSAOMARTE_DB_*`);
- o H2 está como estava, porque a importação só o leu;
- o que foi gravado no PostgreSQL depois da virada não volta para o H2;
- **antes de repetir a virada, zere o banco do jogo** como `admin` (`DROP DATABASE missaomarte;
  CREATE DATABASE missaomarte OWNER missaomarte; REVOKE ALL ON DATABASE missaomarte FROM PUBLIC;`). Sem isso, a nova
  subida vê o PostgreSQL com dados, pula a importação sem aviso, e o que os jogadores gravaram no H2 durante a volta
  atrás se perde (e os ids podem colidir).

**Voltar à imagem anterior à Horda no mesmo banco.** Essa imagem não lê partidas com modo `HORDA`. Se o banco já
contiver essas partidas, `GET /api/partidas/minhas` retorna 500 para os jogadores cujo histórico inclui uma delas.

**Cópia de segurança do PostgreSQL:** `pg_dump` do banco `missaomarte`, no servidor que roda o `postgres-br`, como
na seção 3 (`docker exec <contêiner-do-postgres-br> pg_dump -U missaomarte -d missaomarte -Fc > missaomarte-$(date +%F).dump`).
Se a instância tiver backups agendados no painel, eles também servem, desde que incluam o banco `missaomarte`.

O ensaio desta virada, com a imagem que está em produção, está em
[`docs/evidencias/fase-08/etapa-1b-postgres-flyway.txt`](../docs/evidencias/fase-08/etapa-1b-postgres-flyway.txt).


## 11. Hordas, placar e painel de eventos

- A Horda troca nas horas pares de Fortaleza. A horda 0 começou em **1º de janeiro de 2026, 00h00**
  (`03:00Z`). O prazo de registro termina 30 minutos depois do fim da horda.
- O agendador recupera hordas e semanas pendentes ao subir. No deploy start-first, as instâncias compartilham o
  fechamento, o extrato, o evento e o vencedor do VIP pelo banco. Cada WebSocket continua no contêiner onde abriu.
- O painel fica em `/pagina/admin.html`. Entre com um jogador listado em `MISSAOMARTE_ADMINS`, escolha o evento
  e clique em **Disparar**. A API responde 202, e o canal avisa os pilotos antes de começar.
- Para testes, `missaomarte.agendador.habilitado=false` desliga o agendamento e a recuperação automática.
  Em um servidor local com banco separado, `missaomarte.hordas.ensaio=<id>` força a caverna sem publicar
  a descrição da horda no banco.

## 12. Ajustes do jogo (DA-48)

Os números abaixo mudam o jogo sem lançar um APK novo. Todas as variáveis são opcionais: sem elas, vale o padrão.
No Dokploy, acrescente as linhas na aba **Environment** do `missao-marte-api` e faça o deploy.

| Variável | O que muda | Padrão (Fácil / Médio / Difícil) | Faixa |
|---|---|---|---|
| `MISSAOMARTE_AJUSTE_FACIL_TANQUE`, `_MEDIO_TANQUE`, `_DIFICIL_TANQUE` | combustível máximo | 130 / 100 / 100 | 50 a 300 |
| `MISSAOMARTE_AJUSTE_FACIL_QUEDA`, `_MEDIO_QUEDA`, `_DIFICIL_QUEDA` | maior velocidade de queda num pouso seguro | 90 / 75 / 65 | 20 a 200 |
| `MISSAOMARTE_AJUSTE_FACIL_DESLIZE`, `_MEDIO_DESLIZE`, `_DIFICIL_DESLIZE` | maior velocidade lateral num pouso seguro | 60 / 50 / 40 | 10 a 150 |
| `MISSAOMARTE_AJUSTE_FACIL_INCLINACAO`, `_MEDIO_INCLINACAO`, `_DIFICIL_INCLINACAO` | maior inclinação num pouso seguro, em graus | 25 / 20 / 15 | 5 a 45 |
| `MISSAOMARTE_AJUSTE_FACIL_ASTEROIDES`, `_MEDIO_ASTEROIDES`, `_DIFICIL_ASTEROIDES` | multiplica a velocidade dos asteroides | 0,75 / 1 / 1,3 | 0,25 a 3 |
| `MISSAOMARTE_AJUSTE_METEOROS_MIN` e `MISSAOMARTE_AJUSTE_METEOROS_MAX` | velocidade dos meteoros da chuva, em unidades/s | 150 e 250 | 50 a 600, mínima menor que a máxima |
| `MISSAOMARTE_AJUSTE_DURACAO_CHUVA`, `_TEMPESTADE`, `_PANE`, `_VIP` | duração de cada evento, em segundos inteiros | 20, 25, 20 e 45 | 10 a 90 |

Os nomes abreviados com `_` seguem o mesmo começo. Por exemplo, `_MEDIO_INCLINACAO` é
`MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO`. O valor aceita vírgula ou ponto (`0,9` ou `0.9`). Um valor vazio conta como
não definido.

Quando cada mudança vale:
- **Pilotagem Livre:** na próxima partida de quem voltar ao menu depois do deploy.
- **Horda:** a partir da próxima horda. A horda em andamento continua com os ajustes gravados nela.
- **Eventos ao vivo:** a duração é a da horda em andamento, inclusive nos eventos disparados pelo painel.
- **Jogo publicado antes da DA-48:** continua com os números da Fase 8 até a pessoa instalar o jogo novo. Numa
  horda com ajustes diferentes do padrão, ele não recebe o bilhete e mostra "Atualize o jogo pelo site". Na
  Pilotagem Livre ele continua jogando, com o padrão, e entra no mesmo ranking.

Se algo estiver errado, o servidor novo não sobe e o log diz o quê, por exemplo:
`Ajuste do jogo inválido em MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO: inclinacaoMaxima precisa estar entre 5 e 45 (veio 46)`.
Isso vale para um nome com erro de digitação (também no começo, como `MISSAOMARTE_AJUSTES_...`), um valor que não é
número, uma duração com casas decimais ou um valor fora da faixa. No deploy start-first, o servidor anterior continua atendendo: corrija a variável e faça o deploy de
novo. Na subida, o log mostra os ajustes em uso (`Ajustes do jogo: {...}`), e `GET /api/ajustes` mostra o resultado.

Num rollback para uma imagem anterior à DA-48, os ajustes deixam de valer, e uma horda sorteada por ela fica com o
padrão até o fim.

Mudar um ajuste muda a dificuldade da Pilotagem Livre para quem joga depois. O ranking não é zerado: os recordes
anteriores foram feitos com a regra de antes.

