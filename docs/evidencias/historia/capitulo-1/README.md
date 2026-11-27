# Capítulo 1 — Sinais perdidos

Base: `2ad2250`. Trabalho isolado em `historia-capitulo1`, sem commit/push/produção durante implementação.

## Conteúdo
- M01: resgate de Maria, preservado.
- M02: resgate de Rosa, ativação contínua de 3 s na antena e retorno à base. Retorno antecipado preserva Rosa a bordo; sair do posto interrompe o progresso parcial.
- M03: dois sobreviventes, plataformas em alturas diferentes, escolha de ordem, atalho vigiado e passagem alta; a base reabastece entre viagens.
- Patrulheiro e sentinela com aviso, linha de visão, pulso e recuperação; mira identifica a máquina correta; antena ciano no minimapa.
- Mapa com três missões, desbloqueio em sequência, briefing/rádio próprios, botão da próxima missão após salvar, resultado de capítulo concluído.
- Save v2 com resultados por missão; lê v1 sem perder M01. Retomada e replay preservam recordes. Versões futuras continuam bloqueadas para escrita.

## Verificação
- 589 testes do jogo sem falhas (resumo-testes.txt); APK e zip desktop compilados.
- Testes de voo contínuo do núcleo completam M02 e as duas ordens de M03 sem reposicionamento ou perda de vida.
- Casos RED e GREEN para progresso, sentinela, antena, navegação, aviso de disparo e retomada.
- Arte Blender em background, Cycles 64: 79 verificações completas passaram. Novas folhas com pivô/base constante, detalhes em arte/.
- Revisão independente encontrou incoerência feixe/tiro, mira e indicação da antena; corrigidas. Recarga/áudio da sentinela e pedido de resgate após morte também cobertos.
- Servidor: 216 testes e build passaram (`servidor.log`), com banco/HTTP de teste locais.
- O primeiro comando de baseline coincidiu com a adição do teste novo e falhou pelas APIs ainda inexistentes; a suíte completa posterior está verde.

A execução Android, capturas e resultados ficam em emulador-01/. O celular físico inicialmente estava offline; o usuário reconectou para a validação final. Uma execução automática não substitui a avaliação de diversão e conforto pelo usuário.

A rodada Android `emulador-01` foi interrompida por colisão do piloto de teste no teto da M01. O problema foi reproduzido em entrada a 30 FPS; a altitude do piloto foi corrigida sem mudar a física e os testes completos passaram. A rodada seguinte está em `emulador-02/`.

Rodada `emulador-02`: M01, M02 e M03 concluídas; save reaberto com os três resultados; replay M02 em retrato, aborto e retorno ao mapa em paisagem preservaram todos os recordes.

No celular, a primeira coleta percorreu e concluiu M01–M03, confirmou os três saves e o mapa de capítulo completo (ver `celular-01/voo.txt`). O host atrasou a última captura e encontrou a Activity já fora de primeiro plano; interrompeu a coleta antes do modo reabrir. Ajustamos o coletor para nunca capturar outro app, registrar a ausência do frame e aguardar 3 s no encerramento. A conferência de reabertura/replay é executada separadamente em `celular-reabertura-02/`, com os recordes observados (34, 20, 40). Nenhuma regra do jogo foi alterada por essa correção de coleta.

## Resultado final no aparelho físico

M01, M02 e M03 concluídas e salvas: 34, 20 e 40 pontos (o recorde anterior da M01 foi preservado). Reabertura, replay M02 em retrato (1080×2340), aborto e retorno ao mapa em paisagem: PASS, com as três pontuações idênticas.

Intervalos de quadro da primeira rodada física: M01 7.785 amostras, zero acima de 50 ms, máximo 46,40 ms; M02 10.825 amostras, uma acima de 50 ms, máximo 51,66 ms; M03 16.606 amostras, zero acima de 50 ms, máximo 49,74 ms. Esses números descrevem este aparelho/ensaio; não são uma garantia para todos os Androids.
