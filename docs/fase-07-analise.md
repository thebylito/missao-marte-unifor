# Fase 7 — Análise da melhoria: Modo Pilotagem (issue #13)

Escrita **antes** da implementação, como a issue #13 pede. Ela define a melhoria, o impacto na arquitetura, o que
não pode mudar, os critérios de aceite e os casos de teste. O plano foi aprovado em 2026-11-18. A implementação é a
issue #14, e a documentação final e a demonstração são a #15 (Sprint 5).

## 1. A melhoria escolhida

**Modo Pilotagem:** uma nova mecânica para o mesmo objetivo do jogo, que é resgatar os passageiros e levá-los à
base. Em vez de andar de célula em célula, o jogador **pilota** a nave:
- gira a nave e liga o motor, que empurra na direção do bico e gasta combustível;
- a gravidade puxa a nave para baixo;
- é preciso **pousar** devagar e reto sobre as plataformas;
- tudo isso numa caverna marciana com paredes de rocha e asteroides em movimento.

A mecânica e os números vêm da POC validada pelo usuário em 2026-11-04 (`_referencia/poc`), e os sprites já estão
no jogo desde a Fase 4.

Por que cabe numa sprint e é demonstrável:
- reaproveita o `core` inteiro: telas, recursos, sons, servidor e o Android da Fase 6;
- as regras novas ficam num pacote próprio do `nucleo`;
- o comportamento é observável na janela e no celular, e pode ser medido nos testes (posição, combustível, pontos).

O **modo clássico continua sendo o padrão** e não muda. A pilotagem é escolhida na tela de nova missão.

## 2. Regras do modo

| Regra | Valor (Médio, igual à POC) |
|---|---|
| Passo da simulação | fixo, 1/120 s (o resultado não depende da taxa de quadros) |
| Gravidade, empuxo e giro | 55 unidades/s², 145 unidades/s² e 200 graus/s; arrasto 0,06 |
| Combustível | tanque de 100; o motor gasta 3/s; sem combustível, o motor não empurra |
| Pouso seguro | queda de no máximo 75, deriva lateral de no máximo 50 e inclinação de no máximo 14°, **inclusive**; o centro da nave precisa estar sobre a plataforma |
| Colisões fatais | rocha, corpo das plataformas (por baixo ou de lado), asteroides e saída dos limites da caverna |
| Embarque | pousado na plataforma do passageiro, ele embarca em 0,6 s. Cabem 2 passageiros; com a nave cheia, um aviso |
| Entrega | pousar na base entrega todos os passageiros a bordo e reabastece (35/s) |
| Pontos por passageiro | **os do jogo clássico**: Astronauta 20, Engenheiro 15 e Professor 10, lidos do próprio `Passageiro` (polimorfismo da Fase 1) |
| Bônus de combustível (só na vitória) | `⌊30 × combustível no pouso final / tanque⌋`, de 0 a 30 |
| Vidas | 3. Ao explodir, os passageiros a bordo voltam às suas plataformas, e uma nave nova surge na base 2 s depois |
| Fim | **vitória** ao entregar os 3; **derrota** sem vidas; **abortada** pelo botão Abortar ou pelo Esc |
| Tempo da partida | o tempo **simulado** (soma dos passos), não o relógio: os testes não dependem do relógio real |

**Pontuação máxima:** 45 (20 + 15 + 10) mais 30 de bônus, ou seja, 75.

**Correção registrada:** o plano dizia "10 por passageiro (regra da versão-base)". Isso era um erro: o número veio
da POC, mas a versão-base dá 20, 15 e 10 por tipo. Esta análise segue a regra de verdade da versão-base, que era a
intenção do plano.

**Dificuldade** (o mesmo `Dificuldade` do clássico). A gravidade, o empuxo e o giro não mudam, para o controle ter
a mesma sensação.

| | Fácil | Médio | Difícil |
|---|---|---|---|
| Tanque | 130 | 100 | 100 |
| Pouso: queda, lateral e inclinação | 90 / 60 / 18° | 75 / 50 / 14° | 65 / 40 / 12° |
| Velocidade dos asteroides | × 0,75 | × 1 | × 1,3 |

**Correção registrada durante a implementação (tanque do Difícil):** a análise previa um tanque de 80 no Difícil.
Medido com o piloto automático, a viagem mais longa (o Professor, ida e volta) gasta perto de 100 ou mais, mesmo
voando com cuidado. A própria POC registra que os 33 s de motor do tanque de 100 "cobrem a viagem mais longa, ida e
volta". Com 80, o Difícil seria impossível. Por isso ele fica com 100, e a dificuldade vem dos limites de pouso e
da velocidade dos asteroides.

**Nível:** a caverna da POC (3840 × 1440):
- a base à esquerda;
- o Astronauta em cima de um bloco de rocha, o Engenheiro numa prateleira alta e o Professor no chão, no fim;
- três asteroides oscilando.

## 3. O que não pode mudar (compatibilidade)

- **Modo clássico:** as mesmas regras, o mesmo console e a mesma janela. Com a mesma semente, as 200 partidas do
  console são idênticas às da Fase 6, e os roteiros do console batem com o gabarito. Os prints do clássico na
  janela saem iguais aos da Fase 6, salvo as animações que dependem do relógio.
- **Testes anteriores:** os 232 continuam passando sem mudança. Se algum precisar mudar, a mudança é justificada no
  commit e no tutorial.
- **Ranking clássico:** o mesmo arquivo `ranking.json`, as mesmas regras do Top 5 e o mesmo ranking global.
- **API:** um cliente da Fase 5 ou 6, que não manda o campo `modo`, continua funcionando, e as partidas dele contam
  como clássicas. As partidas já gravadas no banco passam a ser clássicas.
- **Console:** não ganha a pilotagem, porque física em tempo real não cabe num terminal por turnos. É uma decisão
  registrada, não uma regressão.

## 4. Impacto por camada

| Módulo | Novo | Alterado | Ponto de extensão usado |
|---|---|---|---|
| `nucleo` | pacote `pilotagem`: configuração de voo por dificuldade, nave em voo, caverna, plataformas, asteroides em órbita, comandos, eventos de voo, partida de pilotagem (passo fixo) | nada no clássico | `Passageiro` e subclasses (pontos e nomes), `Dificuldade`, `ResultadoPartida`, `ServicoRanking` + `RepositorioRanking` (outro arquivo, mesmo serviço) |
| `core` | controlador da pilotagem (Humble Object: estado dos controles → comandos), layout dos botões da pilotagem (puro), câmera da pilotagem (puro), tela da pilotagem | `FormularioNovaPartida` (modo), `TelaNovaPartida`, `MissaoMarteGame` (inicia pelo modo), `FimDePartida` e `TextosDaInterface` (estatísticas da pilotagem), `TelaBase` e `FiltroDeEntrada` (repassam soltar e arrastar o dedo), `ControladorDoRanking` e `TelaRanking` (modo), `ServicosDoJogo` (ranking da pilotagem), `Folha` (chama, rocha e plataformas), `PartidaParaEnviar` e o cliente HTTP (campo `modo`) | `TelaBase` (alvos de toque, Template Method), padrão do `LayoutDaPartida`, `Recursos`/`Folha` (Facade), `Sons` (eventos → som), `ServidorDoJogo` (porta) |
| `lwjgl3` e `android` | — | os launchers montam o ranking da pilotagem (`ranking-pilotagem.json` na pasta de trabalho ou na pasta privada do app) | raiz de composição |
| `servidor` | enum `Modo` | `Partida` (coluna `modo`, padrão `CLASSICO`), `RegistroDePartida` (campo opcional), ranking filtrado por `?modo=`, histórico com o modo, contrato `openapi.json` | DTOs como contrato (DA-15); consulta do ranking (DA-16) |

## 5. Riscos e como cada um é tratado

| Risco | Tratamento |
|---|---|
| Física que muda com a taxa de quadros | passo fixo com acumulador; teste rodando a 30 e a 144 quadros por segundo |
| Testes dependentes do relógio | o tempo da partida é o tempo simulado; os testes avançam a simulação |
| Provar a vitória numa caverna com paredes e asteroides | um piloto automático determinístico (por pontos de passagem, com controle por realimentação) que vence a caverna no teste, sem janela, e na janela real, por um novo roteiro do `PilotoDaJanela` |
| Dois dedos ao mesmo tempo (girar e acelerar) | a tela acompanha cada dedo por número (tocar, arrastar, soltar); o cálculo "dedos → comandos" é puro e testado |
| O `adb` não simula dois dedos | no Android, a validação automática segura um botão por vez; a partida completa é jogada à mão no celular |
| Migração do banco (coluna nova com partidas antigas) | coluna `modo` com padrão `CLASSICO`; validada no banco de desenvolvimento, que já tem partidas das Fases 5 e 6 |
| Textura repetida no OpenGL ES 2 | o tile de rocha tem 256 × 256 (potência de 2), exigência do ES 2 para repetir a textura |
| Prazo | o bloco B (shadow) só começa depois da tag `fase-7`, e é o primeiro corte |

## 6. Critérios de aceite

| Id | Critério |
|---|---|
| CA-01 | A tela de nova missão tem "Modo: Clássico / Pilotagem", com Clássico como padrão; com Clássico, tudo segue como na Fase 6 |
| CA-02 | Em voo: a gravidade puxa para baixo; o motor acelera na direção do bico e gasta combustível; sem combustível o motor não empurra; girar muda o ângulo |
| CA-03 | O pouso dentro dos três limites (inclusive) e com o centro sobre a plataforma é seguro; qualquer limite excedido, ou o pouso na beirada, explode a nave; rocha, asteroide ou saída da caverna explodem a nave |
| CA-04 | Pousada na plataforma de um passageiro, ele embarca depois de 0,6 s; com 2 a bordo, aparece o aviso "nave cheia" e ninguém embarca |
| CA-05 | Pousar na base entrega todos os passageiros a bordo, soma os pontos do tipo de cada um (20, 15 e 10) e reabastece |
| CA-06 | Explodir tira uma vida e devolve os passageiros a bordo às suas plataformas; uma nave nova surge na base 2 s depois; sem vidas, derrota |
| CA-07 | Entregar os 3 dá vitória, com pontos = soma + bônus de combustível; o tempo é o simulado |
| CA-08 | Abortar (botão ou Esc) termina como abortada, sem ranking; o Voltar do Android não aborta |
| CA-09 | O mesmo conjunto de comandos gera o mesmo resultado a qualquer taxa de quadros; nenhuma regra usa relógio, janela ou rede |
| CA-10 | A dificuldade muda o tanque, os limites de pouso e a velocidade dos asteroides, como na tabela da seção 2 |
| CA-11 | Controles: no teclado, ←/→ ou A/D giram, ↑, W ou Espaço ligam o motor e Esc aborta; no toque, ◄ ► e MOTOR funcionam enquanto pressionados, inclusive dois ao mesmo tempo, e todos têm o alvo mínimo |
| CA-12 | A câmera segue a nave sem mostrar o lado de fora da caverna; o HUD mostra combustível, pontos, vidas, a bordo, restantes e os indicadores de pouso (verde ou vermelho); funciona em retrato e paisagem |
| CA-13 | O fim de partida mostra as estatísticas da pilotagem (pontos, bônus, pousos, tempo, resgatados); a vitória entra no ranking local da pilotagem (Top 5, outro arquivo), sem mexer no clássico |
| CA-14 | Servidor: `modo` é opcional (ausente = `CLASSICO`); o ranking global é por modo (`?modo=`, padrão `CLASSICO`); o histórico mostra o modo; as partidas antigas contam como clássicas |
| CA-15 | A tela de ranking alterna o modo (Clássico ou Pilotagem) e a aba (Local ou Global) |
| CA-16 | Regressão do clássico: console 200/200 contra a Fase 6, roteiros 11/11, prints iguais e testes anteriores inalterados |
| CA-17 | O APK roda a pilotagem; continua sem chamadas acima do `minSdk` 26 |

## 7. Casos de teste

**Unidade, no `nucleo` (sem janela, relógio nem rede):**

| Id | Caso | Critério |
|---|---|---|
| CT-01 | Sem comando, a nave em voo cai: a velocidade vertical fica negativa e cresce | CA-02 |
| CT-02 | Com o motor ligado e o bico para cima, a nave sobe e o combustível cai 3 por segundo | CA-02 |
| CT-03 | Com o tanque vazio, o motor ligado não muda a aceleração (só a gravidade age) | CA-02 |
| CT-04 | Girar por 0,5 s muda o ângulo em 100° | CA-02 |
| CT-05 | Pousos no limite exato (queda 75, lateral 50, inclinação 14°) são seguros | CA-03 |
| CT-06 | Pousos um pouco acima de cada limite (75,5 / 50,5 / 14,5°) explodem | CA-03 |
| CT-07 | Pouso com o centro fora da plataforma ("na beirada") explode | CA-03 |
| CT-08 | Rocha, asteroide e saída da caverna explodem a nave | CA-03 |
| CT-09 | Pousada na plataforma, o passageiro embarca aos 0,6 s e não antes | CA-04 |
| CT-10 | Com 2 a bordo: aviso de nave cheia uma vez, e o terceiro não embarca | CA-04 |
| CT-11 | Na base, todos desembarcam, os pontos somam 20/15/10 conforme o tipo e o tanque enche até o máximo | CA-05 |
| CT-12 | Explosão com passageiros a bordo: eles voltam às plataformas, perde-se 1 vida, a nave renasce na base aos 2 s | CA-06 |
| CT-13 | A terceira explosão termina em derrota | CA-06 |
| CT-14 | Entregar os 3 dá vitória; os pontos incluem o bônus `⌊30 × combustível / tanque⌋` (0 com o tanque vazio, 30 com ele cheio) | CA-07 |
| CT-15 | Abortar termina como abortada, e o tempo e os comandos seguintes não mudam mais nada | CA-08 |
| CT-16 | Os mesmos comandos a 30 e a 144 quadros por segundo dão o mesmo estado final | CA-09 |
| CT-17 | Fácil, Médio e Difícil têm os valores da tabela, e a velocidade dos asteroides muda | CA-10 |
| CT-18 | **O piloto automático vence a caverna** de forma determinística. Escopo ajustado durante a implementação: ele vence a missão inteira no Fácil e, no Médio e no Difícil, completa as viagens do Astronauta e do Engenheiro. Ele é conservador (para e espera o asteroide em cada passagem estreita) e, do jeito que voa, a viagem do Professor passa dos 100 de combustível. Ver a seção 5 do tutorial da Fase 7 | CA-02 a CA-07 |

**Unidade, no `core`:**

| Id | Caso | Critério |
|---|---|---|
| CT-19 | O formulário alterna o modo, com Clássico como padrão | CA-01 |
| CT-20 | O controlador traduz teclas pressionadas e soltas em comandos; dois botões ao mesmo tempo; soltar um dedo libera só o botão dele; arrastar o dedo para fora libera o botão | CA-11 |
| CT-21 | Layout dos botões: em vários tamanhos de tela, nada cortado nem sobreposto e o alvo mínimo respeitado | CA-11, CA-12 |
| CT-22 | A câmera fica dentro da caverna (inclusive quando a tela é maior que a caverna) | CA-12 |
| CT-23 | O fim de partida da pilotagem gera as linhas de estatística e grava no ranking da pilotagem, não no clássico | CA-13 |
| CT-24 | O resumo enviado ao servidor leva `modo=PILOTAGEM`, e o clássico leva `CLASSICO` | CA-14 |
| CT-25 | O controlador do ranking alterna o modo e a aba, e consulta o servidor com o modo certo | CA-15 |

**Integração, no `servidor` (MockMvc + H2):**

| Id | Caso | Critério |
|---|---|---|
| CT-26 | Registrar sem `modo` grava `CLASSICO`; com `PILOTAGEM`, grava pilotagem; um modo inválido dá 400 | CA-14 |
| CT-27 | Os rankings são separados: uma vitória na pilotagem não aparece no clássico, e vice-versa; sem `?modo`, vale o clássico | CA-14 |
| CT-28 | O histórico traz o modo de cada partida | CA-14 |

**Execução real (evidências em `docs/evidencias/fase-07/`):**

| Id | Caso | Critério |
|---|---|---|
| CT-29 | Janela: roteiro `pilotagem` do `PilotoDaJanela` (o piloto automático voando de verdade), com prints de voo, pouso, embarque, entrega, vitória e ranking | CA-01, CA-11 a CA-13 |
| CT-30 | Janela: regressão do clássico (prints contra a Fase 6), console 200/200 e roteiros 11/11 | CA-16 |
| CT-31 | Emulador e celular: abrir a pilotagem, segurar MOTOR para decolar, pousar de volta na base, girar a tela e voltar do segundo plano no meio do voo | CA-11, CA-12, CA-17 |
| CT-32 | Celular: uma partida completa jogada à mão, com o resultado no ranking global da pilotagem | CA-07, CA-14 |
| CT-33 | Banco de desenvolvimento com partidas das Fases 5 e 6: depois da migração, elas aparecem como clássicas | CA-14 |
| CT-34 | Defeitos inseridos de propósito no domínio novo, no layout e no modo do servidor são pegos pelos testes | todos |
| CT-35 | `conferir_api_minima.py` no APK novo: 0 chamadas acima da API 26 | CA-17 |

## 8. Fora do escopo desta melhoria

- **Modo shadow** (bloco B, aprovado como extensão): ver em tempo real as naves de outros pilotos, via WebSocket
  no Spring Boot. Só começa depois da tag `fase-7` e é o primeiro corte se o prazo apertar.
- Outros níveis, um editor de níveis e som de motor contínuo.
- O servidor não confere se a pontuação da pilotagem é legítima, a mesma limitação do clássico (Fase 5).
