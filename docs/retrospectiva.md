# Retrospectiva da equipe

Projeto de 03/11 a 03/12/2026, em 5 sprints. As Sprints 1 a 4 foram entregues em 4 pull requests (#16 a #19), com 8
tags (`fase-0` a `fase-7`). Esta retrospectiva foi registrada na Sprint 5, junto com a revisão final.

## O que funcionou

- **Uma prova por fase antes de seguir.** Nenhuma tag `fase-N` foi criada sem build, testes, execução real e
  evidência em `docs/evidencias/`. Isso tornou cada PR fácil de revisar e deu a resposta pronta para "isso ainda
  funciona?".
- **Equivalência por semente.** Injetar o `Random` na Fase 1 permitiu comparar 200 partidas inteiras entre duas
  versões a cada fase. A regressão do console ficou automática, e não só no "parece igual".
- **Defeitos inseridos de propósito.** Inserir um defeito por vez, numa cópia descartável, mostrou quais regras os
  testes não enxergavam, e eles foram reforçados antes da entrega: 114 defeitos pegos ao longo das fases.
- **POC antes do trabalho real.** A pilotagem e o visual foram validados numa POC descartável (04/11). A Fase 7
  levou só os valores de física e o pipeline de arte, sem retrabalho de design.
- **Registro de decisões.** As 34 DAs evitaram rediscutir escolhas e já respondem às perguntas da apresentação.
- **Ferramentas próprias de validação** quando o ambiente não ajudava. O macOS não deixou o terminal mandar teclas
  nem capturar a tela, e o `PilotoDaJanela` resolveu isso por dentro do jogo. O `adb` não simula dois dedos, e a
  partida foi jogada à mão.

## O que não funcionou tão bem

- **O histórico não mostra a divisão do trabalho.** Todos os 43 commits saíram de uma única conta, e só as issues
  #1 a #3 tinham responsável até a Sprint 5. O enunciado cobra a contribuição de cada integrante no histórico.
- **O Kanban foi atualizado à mão e em lote.** O GitHub Project não move sozinho os itens fechados para
  "Concluído". Em alguns momentos, vários cards foram atualizados de uma vez, e não conforme o trabalho andava.
- **O calendário das sprints não bate com as datas reais.** O trabalho andou na frente: o PR da Sprint 4 foi
  aceito em 20/11, antes do início oficial da sprint (23/11).
- **O repositório ficou privado durante o desenvolvimento,** embora o enunciado peça um repositório público. A
  mudança ficou para o fim.
- **Uma validação que só um caminho cobria.** Os roteiros pelo `./gradlew :console:run` sempre rodaram numa
  máquina com o Android SDK. Só a revisão final, sem o SDK, mostrou que o aviso "Android SDK não encontrado" aparece
  na saída mesmo com `-q`.
- **Uma prova que dependeu de relato.** A partida da pilotagem jogada à mão no celular (CT-32) ficou registrada
  pelo relato do integrante, porque o resultado não chegou ao ranking global do servidor conferido.
- **Tempo gasto numa alternativa descartada:** uma segunda versão do servidor, em Node.js, chegou a ser planejada
  (17/11) e foi abandonada no dia seguinte (DA-18).

## O que faríamos diferente

- Atribuir cada issue a um responsável desde o primeiro dia, e revisar o PR do outro integrante antes de fechar a
  issue, como o enunciado sugere.
- Mover os cards no momento em que o trabalho muda de estado, ou automatizar isso com os workflows do Project.
- Validar cedo numa segunda máquina, de preferência Windows, e num Android antigo.
- Criar o repositório público desde o início, com a varredura de segredos (gitleaks) na integração contínua.
- Rodar cada validação também no caminho "sem o opcional": sem SDK, sem servidor, sem rede.

## Ações para o fechamento

- [ ] Responsáveis nas issues #4 a #15 e a participação no relatório técnico.
- [ ] Repositório público depois da revisão final.
- [ ] Kanban encerrado: #15 e o PR da Sprint 5 em "Concluído", e os marcos fechados.
