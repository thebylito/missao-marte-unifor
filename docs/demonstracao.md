# Roteiro da demonstração final (10 a 15 minutos)

A demonstração mostra o que o enunciado pede: o jogo, a integração com o servidor, a melhoria da equipe (Modo
Pilotagem e modo shadow) e a preservação do comportamento anterior. Cada parte tem um plano B. Se algo falhar ao
vivo, mostramos a evidência gravada e explicamos a limitação, como o enunciado orienta.

## Antes de começar (uns 15 minutos antes)

1. **Servidor:** `cd servidor && docker compose up --build -d`, com o `.env` preenchido (veja o `.env.exemplo`).
   Confira com `curl http://localhost:8080/actuator/health`, que deve responder `{"status":"UP"}`.
2. **Rede:** o Mac e o celular na mesma rede. Anote o IP do Mac com `ipconfig getifaddr en0`.
3. **Celular:** carregado e destravado, com o APK reinstalado (`adb install -r
   android/build/outputs/apk/debug/android-debug.apk`), porque o aparelho pode estar com um build antigo. Na
   entrada, use a opção 4 para apontar para `http://<IP do Mac>:8080`.
4. **Contas de teste:** crie duas contas só para a apresentação, por exemplo `demo-desktop` e `demo-celular`, com
   senhas de teste. Nunca use credenciais reais.
5. **Mac:** ligado na tomada, sem dormir (`caffeinate -d` num terminal), com a fonte do terminal grande e as outras
   janelas fechadas.
6. **Terminais abertos:** um na raiz do projeto, para o console e a janela; um em `servidor/`; e o Swagger aberto no
   navegador (`http://localhost:8080/swagger-ui.html`).
7. **Ensaio:** jogue uma vez a vitória rápida do item 3 abaixo, para ter a sequência na mão.

## Roteiro

| Tempo | Parte | O que mostrar | Plano B |
|---|---|---|---|
| 0:00–1:30 | Abertura | O jogo e a evolução em 8 fases (`git tag`); arquitetura em módulos, com o `nucleo` sem libGDX (slide) | só o slide |
| 1:30–3:00 | Comportamento preservado | `./gradlew :console:run -q --console=plain`: uma partida curta no console, a versão de texto que continua viva; contar que o console de hoje dá 200/200 partidas iguais à Fase 0 pela semente | `docs/evidencias/fase-07/equivalencia-com-semente.txt` e os roteiros 11/11 |
| 3:00–6:00 | Jogo gráfico e servidor | `./gradlew :lwjgl3:run -Dmissaomarte.semente=3`: criar a conta `demo-desktop`, nova missão no Fácil com mapa 3 e vitória rápida (`d d c s s c d w c a a a a a a w w w c d d d s s`); "Resultado registrado no servidor."; ranking global e histórico; no Swagger, `GET /api/ranking` e um `POST /api/partidas` sem token dando 401 | prints em `docs/evidencias/fase-05/janela-online/` |
| 6:00–7:00 | Servidor fora do ar | `docker compose stop`; a entrada avisa "Servidor indisponível"; jogar como convidado, com o ranking local; `docker compose start` | `docs/evidencias/fase-05/janela-offline/` |
| 7:00–11:00 | A melhoria | No celular, com a conta `demo-celular`, Modo Pilotagem pelo toque: decolar com MOTOR, girar, pousar na plataforma, embarcar e levar à base. Ao mesmo tempo, no desktop com `demo-desktop`, Pilotagem também: **cada um vê a nave do outro** (sombra translúcida com o nome, no minimapa e em "Sombras: 1 piloto"). Fechar com o ranking da pilotagem, separado do clássico | sem celular: desktop + `python3 validacao/sombras/piloto_fantasma.py --segundos 120`; sem rede: o emulador; em último caso, `docs/evidencias/fase-07/android-celular-sombras/` |
| 11:00–12:30 | Testes | Rodar `./gradlew build` antes da apresentação e abrir o relatório do `core` (`core/build/reports/tests/test/index.html`, 186 dos 316 testes); contar as técnicas: equivalência por semente e defeitos inseridos de propósito (27/27 e 20/20 na Fase 7) | `docs/evidencias/sprint-5/revisao-final.txt` |
| 12:30–13:30 | Limitações | O servidor não confere a pontuação; a sombra fica 100 a 200 ms atrás; o que não foi verificado (Windows e Linux, Android abaixo da API 34, APK assinado) | slide |
| 13:30–15:00 | Perguntas | respostas abaixo | |

## Respostas curtas às perguntas da apresentação

**Como as responsabilidades mudaram entre a versão console e a gráfica?**
O `Main` de 570 linhas misturava menu, teclado, desenho, regras, sorteio, ranking e arquivo. Hoje as regras estão
na `Partida` (`nucleo`), que recebe um comando e devolve eventos. O console imprime esses eventos, e a tela
gráfica os anima. As telas libGDX só desenham, e as decisões ficam em controladores testáveis (DA-03, DA-10).

**Onde ficam as regras de negócio? Como os testes mostram que não dependem da interface?**
Ficam no módulo `nucleo`, que não declara a libGDX: importar a libGDX ali dá erro de compilação
(`docs/evidencias/fase-03/dependencia-proibida.txt`). Os 92 testes do `nucleo` rodam sem janela, e o mesmo
domínio roda no console, na janela e no Android.

**Qual a diferença entre o cliente libGDX e o servidor Spring Boot?**
São dois builds independentes. O cliente é o jogo: as regras rodam nele. O servidor guarda contas, partidas e
ranking, e repassa as sombras. O servidor não importa nada do cliente, e o contrato entre os dois é só a API
(`docs/api/openapi.json`) (DA-14).

**Como o cliente fala com a API e o que acontece com o servidor fora do ar?**
As telas usam a porta `ServidorDoJogo`. O adaptador `ServidorHttp` usa o `Gdx.net`, que é assíncrono, e devolve a
resposta na thread do jogo, então a tela nunca trava. Sem servidor, o pedido vira uma `Falha` com mensagem, a
entrada avisa "Servidor indisponível" e o jogador segue como convidado, com o ranking local (DA-17).

**Por que senha com hash? Onde o token é validado?**
Com BCrypt, quem lê o banco não descobre a senha, e o BCrypt é lento de propósito contra força bruta. O JWT é
validado pelo resource server do Spring Security em cada pedido autenticado, e também no aperto de mão do
WebSocket das sombras (`ConfiguracaoDeSeguranca`). O dono da partida vem sempre do token, nunca do corpo (DA-15,
DA-32).

**O que é um DTO e por que usá-lo na API?**
É um objeto só para o contrato (`record`s como `RegistroDePartida` e `PartidaResposta`). A entidade JPA pode
mudar sem quebrar os clientes. A validação fica no pedido, e nunca sai um campo interno, como o hash da senha.

**Como o jogo reaproveita código entre desktop e Android?**
O `core` (telas, controladores e cliente da API) e o `nucleo` são os mesmos. O `lwjgl3` e o `android` são só
launchers, que montam as peças da plataforma (onde fica o ranking, o toque ligado, o servidor padrão). O layout é
calculado em Java puro e testado em vários tamanhos de tela (DA-19, DA-20).

**Qual decisão de arquitetura facilitou a melhoria?**
O domínio separado e injetado. A pilotagem reaproveitou os passageiros (os pontos vêm do polimorfismo de
`Passageiro`), a `Dificuldade`, o `ResultadoPartida` e o `ServicoRanking`, com outro `RepositorioRanking`
(`ranking-pilotagem.json`). Na API, bastou um campo opcional `modo`. No cliente, o shadow entrou como mais uma
porta (`ConexaoDeSombras`) (DA-27, DA-29, DA-33).

**O que fariam diferente com mais tempo?**
Testar em Windows, Linux e Android antigo. Usar ranking por consulta com função de janela. Colocar previsão de
movimento e salas no shadow. Publicar o servidor com HTTPS. Restringir o HTTP sem criptografia no app (veja
`docs/retrospectiva.md`).
