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
| Dados | banco H2 em arquivo, em `/dados/missaomarte.mv.db` (volume) |
| Saúde | `GET /actuator/health` → `200 {"status":"UP"}`; também `/actuator/health/liveness` e `/actuator/health/readiness` |

## 2. Variáveis de ambiente

| Variável | Obrigatória | Para quê | Padrão |
|---|---|---|---|
| `MISSAOMARTE_JWT_SEGREDO` | **sim** | segredo que assina os tokens (HS256), com 32 bytes ou mais. Gere com `openssl rand -base64 48` e guarde só nos segredos da plataforma | temporário a cada início (os tokens deixam de valer quando o servidor reinicia) |
| `MISSAOMARTE_URL_PUBLICA` | recomendada | endereço público, por exemplo `https://ranking.exemplo.com`. É o que o "Try it out" do Swagger usa | `http://localhost:8080` |
| `MISSAOMARTE_DADOS` | não | caminho do banco, sem a extensão `.mv.db` | `/dados/missaomarte` na imagem |
| `PORT` | não | porta HTTP dentro do contêiner | `8080` |
| `SPRINGDOC_APIDOCS_ENABLED` e `SPRINGDOC_SWAGGERUI_ENABLED` | não | `false` desliga a documentação (`/v3/api-docs` e `/swagger-ui.html` passam a dar 404). Ligada, ela não expõe dados, só o contrato | ligadas |
| `JAVA_TOOL_OPTIONS` | não | opções da JVM, por exemplo `-XX:MaxRAMPercentage=75` para usar mais da memória do contêiner | padrão da JVM (25% da memória do contêiner) |

Regras:
- **Nunca** ponha o segredo no Git, na imagem ou num `docker-compose` versionado. O `.env` desta pasta é só para
  desenvolvimento e está no `.gitignore`.
- **Trocar o segredo** invalida todos os tokens: cada jogador só precisa entrar de novo. As contas e as partidas
  continuam.
- Os tokens valem 2 horas (`missaomarte.jwt.validade=PT2H` em `application.properties`).

## 3. Persistência e cópia de segurança

- Monte um **volume persistente em `/dados`**. Sem ele, cada nova versão do contêiner começa com o banco vazio.
- O H2 em arquivo aceita **uma instância só**: não escale o serviço para duas réplicas usando o mesmo volume.
- Cópia de segurança com o servidor parado (o arquivo pode estar sendo escrito enquanto ele roda):

  ```bash
  docker compose stop servidor
  docker run --rm -v servidor_dados:/dados -v "$PWD":/copia eclipse-temurin:17-jre \
      cp /dados/missaomarte.mv.db /copia/missaomarte-$(date +%F).mv.db
  docker compose start servidor
  ```

  O nome do volume (`servidor_dados` acima) aparece em `docker volume ls`. Ele depende do nome do projeto do
  compose ou da plataforma.
- Para restaurar, copie o arquivo de volta para o volume com o servidor parado.
- **Atualizar uma versão já publicada:** o esquema do banco evolui sozinho (`spring.jpa.hibernate.ddl-auto=update`).
  Na Fase 7, a coluna `modo` é criada com o valor padrão `CLASSICO`, e as partidas já gravadas passam a contar como
  do modo clássico. Isso foi testado num banco com partidas das Fases 5 e 6. Faça uma cópia de segurança antes de
  atualizar, como em qualquer migração.

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

1. A imagem foi construída a partir desta pasta (`docker build -t missao-marte-servidor .`) ou pela plataforma.
2. `MISSAOMARTE_JWT_SEGREDO` está definido nos segredos da plataforma (32 bytes ou mais).
3. Há um volume persistente em `/dados`, e só uma réplica.
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
- O banco H2 em arquivo serve uma instância. Para várias, troque por PostgreSQL (só configuração:
  `spring.datasource.*` e o driver; exercício 5 do tutorial da Fase 5).
- A imagem é grande (cerca de 577 MB, quase tudo do JRE em Ubuntu). Uma base menor fica como melhoria.
- As conexões do modo shadow ficam na memória da instância: com mais de uma réplica, cada piloto só veria os que
  caíram na mesma. Mais um motivo para uma réplica só (ou um repasse entre instâncias, como Redis).

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
| `/downloads/missao-marte-desktop.zip` | jogo desktop (Windows, macOS e Linux, com Java 17) |
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
   - a imagem é construída, mas não é publicada.
2. **`Build & Deploy`** (`.github/workflows/deploy.yml`), em cada merge na `main` que mexe no jogo ou no servidor:
   - roda o mesmo CI;
   - coloca o APK e o zip em `servidor/downloads/` e gera a imagem;
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
  - `MISSAOMARTE_DADOS=/dados/missaomarte`
  - `MISSAOMARTE_JWT_SEGREDO` com o valor de `openssl rand -base64 48`. **Quem preenche é o dono do painel**, e o
    valor nunca vai para o Git.
- Domain `missao-marte.thebylito.dev` → porta **8080**, em HTTP. O HTTPS fica com a Cloudflare, na frente, como nos
  outros serviços.
- Advanced → Volumes: volume `missao-marte-api-dados` montado em `/dados`, e uma réplica só.
- Advanced → Swarm Settings → Update Config: **Order = Stop First**. O padrão do Swarm (start-first) sobe o
  contêiner novo antes de parar o antigo. O H2 só abre o arquivo em um processo, então o novo morre com
  `The file is locked: /dados/missaomarte.mv.db` e a versão antiga continua no ar. Isso aconteceu no primeiro
  deploy automático (2026-11-25). Com stop-first, cada deploy deixa o site fora do ar por uns 20 segundos.

**No GitHub** (Settings → Secrets and variables → Actions):
- `DOKPLOY_SERVER_URL`: URL do painel do Dokploy
- `DOKPLOY_API_KEY`: chave de API do Dokploy (Profile → API/CLI no painel)
- `DOKPLOY_APPLICATION_IDS`: ID do serviço `missao-marte-api` (o último trecho da URL dele no painel)

Limitação: o APK é o de depuração, assinado com a chave que o runner gera a cada build. Uma versão nova não
atualiza a antiga por cima: o jogador desinstala e instala de novo. Para atualizações sem desinstalar, cadastre um
keystore fixo como secret e assine o APK de release com ele.
