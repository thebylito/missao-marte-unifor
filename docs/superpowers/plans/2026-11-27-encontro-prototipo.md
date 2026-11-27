# Encontro protótipo — plano de implementação

Executar nesta sessão com as habilidades de execução de planos e desenvolvimento guiado por testes.

Objetivo — arena jogável de resgate com patrulheiro, aviso de ataque, pulso e escudo.
Arquitetura — simulação de combate junto à pilotagem em passos de 1/120 s, controlador específico e extensão pequena da tela de voo.
Tecnologia — Java 17, libGDX, Blender em segundo plano e Android API mínima 26.
Desenho — docs/superpowers/specs/2026-11-26-modo-historia.md, etapa 1.

## Restrições

Usar origin/main em worktree próprio. O pacote visual da branch arte-poco-polar aguarda integração. O protótipo reutiliza a arte publicada e acrescenta seus recursos. Resultados do encontro ficam locais e temporários. Commits e operações remotas dependem do usuário.

## Verificação

Cobrir disparo sem alvo, repetição do comando, oclusão por terreno, escudo e invulnerabilidade, reposição na base, encerramento e equivalência entre taxas de quadros. Conferir entrada pelo menu, toque simultâneo, mudança de tamanho e retorno do resultado.

## Tarefa 1 — Simulação

- [x] Criar EncontroDeResgateTeste e observar falhas antes da implementação.
- [x] Criar EncontroDeResgate no pacote pilotagem.historia, usando PartidaDePilotagem e expondo posição e estado do patrulheiro, projéteis, escudo, bateria e pulso.
- [x] Implementar atualizar(double, Comandos, boolean disparar, boolean assistido) em passos fixos.
- [x] Adicionar PartidaDePilotagem.receberAtaque() para integrar a perda de vida aos eventos existentes.
- [x] Passar a suíte pertinente.

O patrulheiro percorre X=800..1500 em Y=540 a 55 unidades/s, avisa por 1 s e dispara a 180 unidades/s. O pulso usa cone frontal de 20 graus, alcance de 300, intervalo de 0,8 s e desativa por 4 s. Há quatro cargas de bateria e duas de escudo.

## Tarefa 2 — Entrada e apresentação

- [x] Criar ControladorDoEncontro e TelaEncontro, com resultado próprio.
- [x] Acrescentar ponto protegido de avanço no controlador e pontos protegidos de desenho e conclusão na tela de pilotagem.
- [x] Abrir o encontro pelo menu, com tecla 8 e alvo de toque.
- [x] Implementar F/PULSO e G/ASSISTÊNCIA, mais leitura de escudo e bateria.
- [x] Confirmar conclusão, repetição e saída do encontro.

## Tarefa 3 — Arte

- [x] Modelar patrulheiro com rotores e luz de sensor no Blender.
- [x] Gerar folha de movimento e efeitos de pulso/projétil; registrar manifesto e origem.
- [x] Revisar silhueta, direção, alcance do efeito e aviso no tamanho de jogo.

## Tarefa 4 — Validação e revisão

- [x] Executar build, testes e verificação de assets.
- [x] Rodar no Android um encontro com aviso, tiro, defesa e resgate; guardar vídeo e logs.
- [x] Rever a sequência antes de convidar o usuário.
- [x] Pedir revisão independente.
- [ ] Aceite humano do protótipo. A etapa seguinte depende dessa avaliação.

A autorização para prosseguir cobre este protótipo. Este plano detalha o recorte aprovado. Os parâmetros são escolhas iniciais para calibrar no encontro.
