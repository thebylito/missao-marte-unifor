# Fase 8 — Análise da melhoria: Hordas, eventos ao vivo, moedas e skins

Escrita **antes** da implementação, como na Fase 7. Ela define a melhoria, as regras, o que não pode mudar, o
impacto na arquitetura, os riscos, os critérios de aceite e os casos de teste. O plano foi aprovado em 2026-11-25.

## 1. A melhoria escolhida

O ranking da Pilotagem está prendendo o grupo, mas toda partida é igual:
- só existe uma caverna (`Caverna.marciana()`);
- os passageiros estão sempre no mesmo lugar;
- os asteroides seguem um seno, e no mesmo segundo estão sempre no mesmo ponto.

Dá para decorar a rota. A melhoria torna as partidas dinâmicas com **eventos do servidor**, sem virar multiplayer de
colisão, e dá um motivo a mais para jogar: **moedas** que compram **skins** da nave.

| Peça | O que é |
|---|---|
| **Horda** | um modo novo. A cada 2 horas o servidor publica um desafio (caverna, variações e dificuldade) igual para todos, com um Top 10 próprio |
| **Eventos ao vivo** | perigos e oportunidades que o servidor dispara para todos os pilotos que estão voando naquele instante, no Livre e na Horda |
| **Placar da semana** | soma da colocação em cada horda com os pontos dos eventos |
| **Moedas e Hangar** | moedas por vitória, uma loja de skins e um Hangar para equipar a skin; a skin aparece também na sombra vista pelos outros |
| **Skins** | no mínimo 8 naves novas, modeladas no Blender como a nave atual |

O jogo passa a ter três modos: **Clássico**, **Pilotagem Livre** (a Pilotagem de hoje, com o ranking de hoje) e
**Horda**.

## 2. Regras

### 2.1 Horda

| Regra | Valor |
|---|---|
| Horário | uma horda nova em cada hora par de Fortaleza (00h, 02h, …, 22h; `America/Fortaleza`, sem horário de verão). Cada uma dura 2 h |
| O que o jogo mostra | a horda atual (caverna, dificuldade, modificadores, quanto falta para acabar) e só o **horário** da próxima. A caverna da próxima só aparece na virada |
| Caverna | sorteada entre as **liberadas** do rodízio, sem repetir a anterior (a não ser que só haja uma liberada) |
| Liberação de uma caverna | só entra no rodízio depois que alguém vencer nela no Difícil, num servidor local com `missaomarte.hordas.ensaio=<id>` |
| Passageiros | os 3 de sempre (Astronauta 20, Engenheiro 15, Professor 10), em 3 dos ~8 pontos de pouso candidatos da caverna |
| Dificuldade | sorteada por horda: Fácil 20%, Médio 50%, Difícil 30%. Vale para todos naquela horda |
| Asteroides | os da caverna, com a velocidade (× 0,8 a 1,3) e a fase sorteadas |
| Modificadores | de 0 a 2, fixos durante a horda: vento constante, gravidade mais forte (× 1,1 a 1,2) e tanque de 85% a 100%. O tanque nunca é cortado no Difícil, porque a viagem mais longa já gasta perto de 100 (DA-34) |
| Roteiro | de 1 a 2 perigos em instantes fixos da partida (entre 20 s e 120 s de voo), iguais para todos. Com 2 ou 3 eventos ao vivo em 2 h, só cerca de 8% das partidas de 3 min pegariam um |
| Pontos | os da Pilotagem: resgates (20, 15 e 10) mais o bônus de combustível (até 30); máximo de 75 |
| Top 10 da horda | a melhor vitória de cada jogador naquela horda; no empate, fica na frente quem fez primeiro (a regra de hoje) |
| Virada | uma partida que começou na horda N vale para N, se terminar até 30 min depois do fim de N |
| Conta | a Horda exige login. O convidado vê "entre com uma conta para jogar a horda" |

### 2.2 Eventos ao vivo

Atingem todos os pilotos **com conta** conectados ao canal das sombras naquele instante (Livre e Horda). O convidado
joga sem eventos, como já joga sem sombras (DA-32).

| Evento | Duração | Efeito |
|---|---|---|
| Chuva de meteoros | 20 s | de 6 a 10 meteoros atravessam a caverna em linha reta, com trajetórias geradas por uma semente. Batida fatal, como num asteroide |
| Tempestade de poeira | 25 s | rajadas laterais que mudam de lado (média zero, até ±30 unidades/s²) e menos visibilidade (névoa só na tela) |
| Pane de gravidade | 20 s | gravidade × 1,3; somada a um modificador, nunca passa de 1,5 × a gravidade base |
| Passageiro VIP | 45 s | uma cápsula surge num ponto de pouso livre. O primeiro piloto a pousar nela ganha os pontos do VIP. Ela não soma pontos à partida, não conta como pouso e não some com a nave em cima |

- **Aviso:** 15 s antes, para todos os conectados (ex.: "⚠ CHUVA DE METEOROS EM 12 s").
- **Agenda automática:** de 2 a 3 eventos por horda, em horários tirados da semente da horda. Nunca nos primeiros
  5 min, com pelo menos 20 min entre eles. O evento só dispara se houver alguém conectado.
- **Painel:** um administrador dispara qualquer evento na hora (o aviso de 15 s continua valendo).
- **Justiça:** um evento só traz **perigo** à partida e nunca aumenta a pontuação normal. O que ajuda vale só no
  placar da semana:
  - sobreviver a um perigo, estando em voo ou pousado no começo dele e sem explodir até o fim: **+3**;
  - pegar o VIP primeiro: **+5**.

### 2.3 Placar da semana

- A semana vai de segunda às 00h até a segunda seguinte (Fortaleza).
- Cada horda fechada na semana dá pontos pela colocação no Top 10: 10, 8, 6, 5, 4, 3, 2, 1, 1, 1.
- Somam-se os pontos de evento.
- A semana fecha na segunda às 00h30, depois do fechamento da última horda.

### 2.4 Moedas

| Origem | Moedas |
|---|---|
| Vitória no Clássico | 1 (teto de 20 por dia de Fortaleza: a vitória no Fácil leva de 8 a 10 s) |
| Vitória no Livre | 3 (+1 no Difícil) |
| Vitória na Horda | 5 |
| Fechamento de uma horda | 1º 20, 2º 15, 3º 10, 4º ao 10º 5 |
| Fechamento da semana | 1º 50, 2º 30, 3º 20 |

- Só com conta e só na vitória. Derrota e missão abortada não dão moedas.
- **Sem retroativo:** todos começam do zero quando as moedas entram.
- **Bilhete de partida:** uma vitória só rende moedas se a partida começou com um bilhete do servidor
  (`POST /api/partidas/inicio`). Regras do bilhete:
  - ligado ao modo, à dificuldade e, na Horda, ao número da horda;
  - um bilhete ativo por jogador (pedir outro anula o anterior), válido por 2 h e usado uma vez, qualquer que seja o
    resultado;
  - o tempo real desde o bilhete precisa ser pelo menos o tempo simulado da partida menos 10 s, e pelo menos 5 s
    no Clássico e 45 s na Pilotagem e na Horda;
  - é **obrigatório na Horda**;
  - sem bilhete (clientes 7.x), a partida entra no ranking de sempre, mas não rende moedas.

### 2.5 Loja, Hangar e skins

- **Catálogo** no servidor (`skins.json`): id, nome, preço e raridade. Preços iniciais: comum 60, rara 150 e
  épica 300, ajustáveis sem nova versão do jogo.
- **Skin `padrao`:** a nave de hoje, de graça e equipada por padrão.
- **Compra:**
  - precisa de saldo;
  - comprar duas vezes a mesma skin dá erro 409;
  - o saldo nunca fica negativo, nem com duas compras ao mesmo tempo.
- **Só cosmético:** a colisão e o voo não mudam (casco de raio 30, pés em ±24/−40, bocal em −28).
- **Onde aparece:** a skin equipada vale em todo lugar onde a nave aparece (Clássico, Pilotagem, menu e fim de
  partida) e na sombra que os outros pilotos veem.
- **Hangar** (tecla `7` no menu): o saldo, as skins com preço, comprar e equipar. Mostra só as skins do catálogo
  cuja arte o jogo instalado tem.

## 3. O que não pode mudar (compatibilidade)

- **Clássico e console:** as mesmas regras. As 200 partidas do console e os 11 roteiros continuam batendo com o
  gabarito.
- **Pilotagem sem eventos:** a mesma física, passo a passo. O caminho sem efeitos fica separado por `if`, para os
  testes da Fase 7 continuarem iguais sem mudança.
- **Rankings atuais:** os Top 10 global e os Top 5 locais do Clássico e da Pilotagem seguem como estão, inclusive a
  mistura de dificuldades do Livre (decisão do usuário, 2026-11-25). A Horda é o ranking com dificuldade única.
- **Clientes 7.x** (APK e desktop já instalados):
  - continuam entrando, jogando e registrando partidas;
  - no canal, caem na sala `livre`;
  - ignoram as mensagens de tipo novo (`SombrasDaPilotagem.receber` descarta tipos desconhecidos);
  - não ganham moedas, porque não pedem bilhete.
- **Banco:** as partidas já gravadas continuam com o mesmo modo, sem horda.
- **Menu e nova missão:** as teclas `1` a `6` e o TAB de dois modos não mudam, porque o `PilotoDaJanela` depende
  deles. A Horda abre pela tecla `H` ou pelo toque na faixa da horda, e o Hangar pela `7`.

## 4. Impacto por camada

| Módulo | Novo | Alterado |
|---|---|---|
| `nucleo` | pacote `pilotagem.horda`: `DescricaoDeCaverna`, `DescricaoDaHorda`, `SorteioDaHorda`, `MontadorDaHorda`, `ValidadorDeCaverna`; efeitos temporários (meteoros, tempestade, gravidade, VIP) | `PartidaDePilotagem` (efeitos, com o caminho sem efeitos intacto), `PlataformaDePouso.Tipo` (cápsula do VIP) |
| `core` | tela da Horda, faixa da horda no menu, Hangar, leitura do JSON da horda, folhas das skins (fora do enum `Folha`) | `TelaMenu` e `ControladorDoMenu` (tecla `H` e `7`, saldo), `ControladorDoRanking` e `TelaRanking` (três modos, abas da Horda), `SombrasDaPilotagem` (sala, skin, eventos), `ServidorDoJogo` e `ServidorHttp` (horda, bilhete, loja, retorno do registro), `TelaPilotagem` (efeitos e skin), `FimDePartida` (moedas e colocação), `CatalogoDeAssets` |
| `servidor` | build com o `:nucleo`; hordas, bilhetes, moedas, loja, placar da semana, agendador de eventos e de fechamentos, administração, migrador do esquema | `Modo` (+ `HORDA`), `Partida` (horda), `CanalDeSombras` (salas, skin, eventos), `ConfiguracaoDeSeguranca` (rotas públicas novas e administração), `ServicoDeRanking` (por horda), Dockerfile, compose e workflows (contexto na raiz) |
| `arte` e `assets` | `arte/blender/naves/`, `assets/sprites/naves/` (folhas, prévias, atlas das sombras e catálogo) | `render_all.py`, `specs.py`, `verify.py`, `assets/ORIGEM.md` |
| página | horda atual, contagem até a próxima, Top 10 da horda, placar da semana, `admin.html` | `index.html`, `app.js` |

## 5. Riscos e como cada um é tratado

| Risco | Tratamento |
|---|---|
| O Hibernate 7.4 cria as colunas de enum como `ENUM` nativo do H2 (`"MODO" ENUM('CLASSICO', 'PILOTAGEM')`, conferido em 2026-11-26 num banco gerado pela versão da 7.x), e o `ddl-auto=update` não muda o tipo de uma coluna: toda partida da Horda falharia em produção | as colunas de enum passam a texto (`@JdbcTypeCode(SqlTypes.VARCHAR)`); um script SQL idempotente, que roda antes do Hibernate, converte as colunas do banco que já existe; um teste sobe o servidor sobre um H2 com o esquema exato da 7.x |
| O servidor passa a compilar o `nucleo`, que hoje herda o toolchain e o JUnit do `build.gradle` da raiz | `include 'nucleo'` no `servidor/settings.gradle`, com o toolchain 17, os repositórios e o UTF-8 configurados no build do servidor; saída em `servidor/build/nucleo`; `:nucleo:test` desligado ali, porque a raiz já roda esses testes |
| O contexto do Docker é `servidor/` e não vê o `../nucleo` | o contexto passa para a raiz, com `-f servidor/Dockerfile` e um `servidor/Dockerfile.dockerignore` |
| O repositório é público: com a semente igual ao número da horda, dava para calcular as próximas | semente = HMAC-SHA256 com um segredo do servidor (`MISSAOMARTE_HORDA_SEGREDO`) |
| Um deploy no meio da horda mudar o sorteio (cavernas liberadas, arquivos novos) | a horda é gravada na primeira geração e relida depois |
| Fechamentos de horda e de semana sem ninguém chamando a API | agendador com `verificar()` (desligado nos testes, que chamam direto com relógio fixo) e recuperação dos fechamentos atrasados na subida |
| Farm de moedas por `curl` (o servidor não confere os pontos, DA-16) | o bilhete de partida, e o bilhete obrigatório na Horda. O replay completo fica fora desta fase |
| Memória de textura no Android (cada folha 1024×1024 com mipmaps ocupa cerca de 5,6 MB) | carregar só a folha da skin equipada, na troca de tela; as sombras usam um atlas pequeno de 64 px com todas as skins |
| Mudança na física quebrar os testes da Fase 7 | os efeitos ficam num caminho separado; os meteoros ficam numa lista própria, conferida depois dos asteroides |
| Relógio do aparelho errado | o servidor manda segundos relativos (`segundosParaAcabar`, `segundosParaProxima`), não horários absolutos |
| Outra sessão mexendo nos mesmos arquivos (DA-38, sessão persistente) | a Fase 8 começa a mexer no código só depois do merge desse trabalho; as decisões começam no DA-39 |

## 6. Critérios de aceite

| Id | Critério |
|---|---|
| CA-01 | O menu mostra a horda atual (caverna, dificuldade, modificadores, "termina em") e o horário da próxima; `H` ou o toque abre a Horda; o convidado vê o aviso de login |
| CA-02 | Na virada de uma hora par, a horda nova começa para todos; a mesma horda é igual para todos os jogadores e não muda com um deploy |
| CA-03 | A partida da Horda usa a caverna, os passageiros, a dificuldade, os modificadores e o roteiro publicados; a vitória entra no Top 10 da horda |
| CA-04 | Uma partida iniciada antes da virada vale para a horda em que começou, se terminar até 30 min depois do fim dela |
| CA-05 | Uma caverna só entra no rodízio quando está liberada, e uma caverna inválida nunca entra |
| CA-06 | Um evento ao vivo chega, com aviso de 15 s, a todos os pilotos conectados, no Livre e na Horda, e tem o efeito da tabela 2.2 |
| CA-07 | Nenhum evento aumenta a pontuação da partida; sobreviver e pegar o VIP somam no placar da semana; só o primeiro piloto pega o VIP |
| CA-08 | A agenda automática dispara de 2 a 3 eventos por horda nas regras de horário; o painel dispara na hora só para administradores (os outros recebem 403) |
| CA-09 | O canal das sombras separa as salas: só se vê a sombra de quem está na mesma caverna; o cliente 7.x cai na sala `livre` |
| CA-10 | O placar da semana soma a colocação em cada horda fechada e os pontos de evento |
| CA-11 | As moedas seguem a tabela 2.4; derrota e abortada não dão moedas; o mesmo crédito nunca é feito duas vezes |
| CA-12 | Sem bilhete válido, a vitória não rende moedas; a Horda sem bilhete é recusada |
| CA-13 | O Hangar mostra o saldo e as skins; comprar desconta o saldo, que nunca fica negativo; equipar troca a nave em todas as telas e na sombra vista pelos outros |
| CA-14 | Há no mínimo 8 skins novas, geradas pelo pipeline do Blender, que passam no `verify.py`, com a mesma geometria de colisão |
| CA-15 | Regressão: o Clássico, o console, a Pilotagem sem eventos e os rankings atuais ficam iguais à Fase 7; os testes anteriores passam sem mudança |
| CA-16 | A página mostra a horda atual, a contagem até a próxima, o Top 10 da horda e o placar da semana |
| CA-17 | Funciona no desktop e no Android com o mesmo código; o APK continua sem chamadas acima da API 26 |

## 7. Casos de teste

**Unidade, no `nucleo`:**

| Id | Caso | Critério |
|---|---|---|
| CT-01 | O mesmo número e a mesma semente sorteiam a mesma horda; sementes diferentes variam caverna, pontos, dificuldade e modificadores dentro das faixas | CA-02, CA-03 |
| CT-02 | O sorteio nunca corta o tanque no Difícil e só repete a caverna anterior quando há uma só liberada | CA-03, CA-05 |
| CT-03 | O validador recusa caverna fora dos limites, base ou ponto dentro da rocha, plataforma sem folga para a nave e órbita que passa sobre uma plataforma | CA-05 |
| CT-04 | Sem efeitos, a partida é idêntica à de hoje: os testes da Fase 7 passam sem mudança | CA-15 |
| CT-05 | Chuva de meteoros: um meteoro na rota explode a nave, e as trajetórias são iguais para a mesma semente | CA-06 |
| CT-06 | Tempestade: as rajadas têm média zero na duração; a pane soma no máximo 1,5 × a gravidade base | CA-06, CA-07 |
| CT-07 | VIP: pousar na cápsula marca o VIP, não soma pontos nem pousos, e a cápsula não some com a nave pousada | CA-07 |
| CT-08 | O passo da simulação em que cada evento ao vivo entrou fica registrado | CA-06 |

**Integração, no `servidor` (MockMvc + H2):**

| Id | Caso | Critério |
|---|---|---|
| CT-09 | Relógio das hordas: 19:59:59 é a horda das 18h e 20:00:00 a das 20h; reiniciar o servidor no meio devolve a mesma horda | CA-02 |
| CT-10 | `marciana.json` monta exatamente a geometria do `Caverna.marciana()`; o JSON de exemplo (`docs/api/exemplos/horda.json`) é aceito | CA-03 |
| CT-11 | Partida da Horda aceita dentro da janela de 30 min e recusada depois; pontos acima do máximo são recusados | CA-04 |
| CT-12 | O servidor sobe sobre um banco com o esquema da 7.x e grava uma partida da Horda | CA-15 |
| CT-13 | Bilhete: reuso, tempo curto, modo trocado, bilhete anulado por outro e Horda sem bilhete | CA-12 |
| CT-14 | Moedas: tabela por modo, teto do Clássico, crédito idempotente e duas compras simultâneas sem saldo negativo | CA-11, CA-13 |
| CT-15 | Fechamento da horda e da semana, inclusive a recuperação na subida do servidor | CA-10, CA-11 |
| CT-16 | Salas isolam as sombras; sala de horda vencida é recusada; sem sala, `livre`; a sombra leva a skin do perfil | CA-09, CA-13 |
| CT-17 | Agenda e painel: aviso, início e VIP pelo canal; o primeiro `vip` ganha; administrador 200, os outros 403 | CA-06 a CA-08 |
| CT-18 | Top 10 por horda e placar da semana com a pontuação de colocação e de eventos | CA-03, CA-10 |

**Unidade, no `core`:**

| Id | Caso | Critério |
|---|---|---|
| CT-19 | O cliente monta a partida a partir do JSON de exemplo compartilhado (o mesmo arquivo do CT-10) | CA-03 |
| CT-20 | Menu: faixa da horda, `H` abre a Horda, convidado vê o aviso; as teclas 1 a 6 continuam iguais | CA-01, CA-15 |
| CT-21 | O ranking alterna três modos e as abas da Horda | CA-03, CA-10 |
| CT-22 | Hangar: saldo, compra, erro de saldo, equipar; skin desconhecida vira `padrao` | CA-13 |
| CT-23 | Canal: o cliente manda a sala, trata aviso, início e VIP, e manda o resultado do evento | CA-06, CA-09 |

**Execução real (evidências em `docs/evidencias/fase-08/`):**

| Id | Caso | Critério |
|---|---|---|
| CT-24 | `docker compose up --build` com o contexto na raiz; o servidor sobe com o `nucleo` | CA-15 |
| CT-25 | Jogo desktop contra o servidor local com `ensaio`: uma partida da Horda completa, com o Top 10 e as moedas | CA-01 a CA-04, CA-11 |
| CT-26 | Evento disparado pelo painel atinge o jogo e o `piloto_fantasma.py --sala` ao mesmo tempo | CA-06, CA-08, CA-09 |
| CT-27 | Comprar e equipar uma skin e vê-la na sombra de outro aparelho | CA-13 |
| CT-28 | Android num aparelho real: Horda, evento e Hangar; `conferir_api_minima.py` sem chamadas acima da API 26 | CA-17 |
| CT-29 | Deploy no Dokploy: a página mostra a horda e a contagem | CA-16 |
| CT-30 | Regressão: console 200/200, roteiros 11/11 e os prints do clássico | CA-15 |
| CT-31 | `verify.py` com todas as skins; cada caverna nova vencida por um humano no Difícil antes de ser liberada | CA-05, CA-14 |

## 8. Etapas

Cada etapa tem a própria branch e o próprio PR. Antes da próxima, passa pelo portão de sempre: build, testes,
execução real e evidência.

| # | Etapa | Depende de |
|---|---|---|
| 0 | Esta análise | — |
| C | Skins (trilha paralela, feita pelo Codex): primeiro o pipeline e 2 skins, depois as outras | — |
| 1 | Servidor com o `nucleo`: build, Docker, CI, deploy e migrador do esquema. Nada do jogo muda | 0 e o merge da DA-38 |
| 2a | Moedas, bilhete, loja, Hangar e skin nas sombras, com a `padrao` + 2 skins | 1 e C (parte 1) |
| 2b | Todas as skins | C (parte 2) |
| 3 | Cavernas como dados: validador, `marciana.json` e 2 cavernas novas, liberadas depois do teste humano | 1 |
| 4 | Horda no servidor: relógio, sorteio, API, ranking, fechamentos e salas | 3 |
| 5 | Horda no jogo: menu, tela, envio e ranking, no desktop e no Android | 4 |
| 6 | Eventos ao vivo: efeitos, agendador, painel, placar e moedas da semana | 5 |
| 7 | Página, tutorial (`docs/fase-08-tutorial.md`), README e tag `fase-8` | 6 |

## 9. Fora do escopo desta melhoria

- **Replay anti-trapaça:** o servidor refazendo a partida a partir dos comandos. Fica preparado, porque cada evento
  ao vivo já grava o passo da simulação em que entrou. A troca de `Math` por `StrictMath`, necessária para o
  resultado bater entre a JVM e o Android, também fica para depois.
- Colisão entre naves, disputa por passageiros e partidas em grupo.
- Editor de cavernas e caverna gerada por sorteio.
- Separar o ranking do Livre por dificuldade (decisão do usuário: fica como está).
- Moedas retroativas pelas vitórias anteriores (decisão do usuário: todos começam do zero).

## 10. Decisões a registrar

Cada decisão entra em `docs/decisoes-arquiteturais.md` junto com a etapa que a implementa:

- **DA-39 — O servidor inclui o `nucleo`** (etapa 1). Revoga em parte a DA-14 e a DA-16. O servidor passa a
  compilar o `nucleo` (e só ele) para gerar e conferir as hordas, e mais tarde poderá refazer partidas. O contrato
  com o jogo continua sendo a API. O contexto do Docker passa para a raiz.
- **DA-40 — Colunas de enum como texto** (etapa 1). O `ENUM` nativo do H2 não aceita valores novos, e o
  `ddl-auto=update` não muda o tipo. As colunas de enum passam a texto, e um script idempotente converte o banco
  que já existe antes do Hibernate subir.
- **DA-41 — Moedas por extrato e bilhete de partida** (etapa 2a). O extrato tem crédito único por origem e
  referência, a compra trava o jogador, e o bilhete é obrigatório na Horda.
- **DA-42 — Skins só cosméticas** (etapa 2a). O catálogo e os preços ficam no servidor, e a arte fica no jogo. A
  skin `padrao` é a nave de sempre. As folhas ficam fora do enum `Folha`, e as sombras usam um atlas pequeno.
- **DA-43 — A horda é gerada no servidor e vai pronta como dados** (etapas 3 e 4). Semente HMAC com segredo, horda
  gravada na primeira geração, cavernas em JSON no servidor (uma caverna nova não exige nova versão do jogo) e
  segundos relativos na resposta.
- **DA-44 — Canal das sombras com salas e eventos** (etapas 4 e 6). Revisa a DA-31. As salas são `livre` e
  `horda-N`, e o cliente sem sala cai na `livre`. Os eventos vão a todas as salas. A sombra passa a levar a skin do
  perfil.
- **DA-45 — Evento só atrapalha a partida** (etapa 6). O que ajuda vale no placar da semana, e o Top 10 da horda
  guarda a melhor partida.
