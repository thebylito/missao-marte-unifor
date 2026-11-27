# Modo História — campanha de resgate e exploração

## Estado da proposta

Planejamento para aprovação, elaborado em 26/11/2026. O usuário definiu o tom de **resgate e exploração**, pediu variedade de fases, assets e inimigos e escolheu **misturar evasão e combate leve**. Os números, a história, os controles e a organização abaixo são propostas de implementação sujeitas à validação.

A campanha proposta tem **6 capítulos, 18 missões, 6 famílias de inimigos e 1 guardião final**. A primeira entrega jogável terá as três missões do primeiro capítulo. A expansão completa depende do resultado desse primeiro capítulo.

## Objetivo

Dar ao jogador razões concretas para começar outra missão. Cada fase deve apresentar uma descoberta, uma decisão de pilotagem ou um encontro diferente, enquanto o resgate continua sendo o centro da experiência. A melhora de retenção é uma hipótese a medir depois da publicação.

O jogador acompanha uma equipe isolada por uma falha na rede de comunicações de Marte. Resgata pessoas, recupera equipamentos e reativa postos para organizar uma evacuação. A equipe resgatada passa a participar da base e das comunicações seguintes. Como ponto de partida para o roteiro, uma tempestade corrompeu as autorizações da rede de segurança. Máquinas de manutenção passaram a tratar veículos de resgate como intrusos. Cada capítulo recupera parte da comunicação e revela como alcançar a central para restabelecer o acesso.

A nave permanece um veículo de resgate. O combate abre oportunidades de passagem e protege os sobreviventes. Há rotas de evasão e situações em que enfrentar um inimigo consome mais tempo que contorná-lo.

## Fundamento técnico observado

A referência publicada é `origin/main` em `d0a2d71`. A branch local `arte-poco-polar` contém trabalho de arte e animação desta sessão ainda pendente de integração. A campanha dependerá da versão revisada desse trabalho.

- O núcleo já oferece pilotagem, combustível, pouso, passageiros, capacidade limitada, asteroides em órbita, vento e eventos com roteiro.
- Existem três cavernas em JSON, Marciana, Poço Polar e Veio de Ferro. A geometria física usa blocos retangulares e plataformas.
- `DescricaoDeCaverna` descreve o terreno. `MontadorDaHorda` monta uma Horda com exatamente três passageiros e regras específicas daquele modo.
- O cliente distingue os modos na apresentação e nos registros. A API tem enums e bilhetes próprios para Clássico, Pilotagem e Horda.
- Inimigos que patrulham, detectam o motor, disparam ou atacam instalações exigem lógica nova de campanha. O inimigo atual do Clássico pertence ao jogo de grade.
- A arte usa modelos Blender e sprites pré-renderizados com pivôs fixos. A sessão acrescentou ambientes, espera, reação, aceno, caminhada, proteção, comemoração, portas e suspensão.
- `Recursos` enfileira as folhas do catálogo geral. Um catálogo de campanha deverá carregar o capítulo ativo para acomodar a expansão.

A fundação aproveitada inclui física, montagem de cenários, controles, câmera, áudio e pipeline de arte. A campanha terá sua própria definição de missão, progresso e condições de conclusão.

## Abordagem proposta

| Abordagem | Benefício | Custo e limite |
|---|---|---|
| Campanha autoral em capítulos | Permite ensinar, surpreender e controlar a variedade entre missões | Exige autoria e teste de cada fase |
| Missões procedurais com narrativa entre partidas | Amplia combinações usando regras de geração | Exige validação de rotas geradas e tem menor controle sobre o primeiro contato |
| Campanha com ramificações e finais diferentes | Aumenta a possibilidade de revisitar decisões | Multiplica cenários, diálogos, saves e combinações de teste |

A proposta usa **campanha autoral**, com missões rejogáveis e objetivos secundários. Variações procedurais podem ampliar desafios depois que as missões autorais estiverem validadas. O primeiro lançamento da história segue uma sequência de capítulos definida.

## As três primeiras partidas

A novidade precisa aparecer no começo da experiência. O desenho inicial deve atender a estes marcos de playtest.

| Missão | Primeiro minuto | Decisão principal | Recompensa percebida |
|---|---|---|---|
| Primeiro sinal | Decolar, observar um patrulheiro cruzando uma rota lateral e localizar o pedido de socorro | Escolher a passagem e pousar | Resgatar Maria e vê-la chegar à base |
| A torre muda | Encontrar uma sentinela e experimentar o pulso de defesa | Esperar a varredura ou desativar a sentinela | Reativar a antena e abrir a comunicação seguinte |
| Duas rotas, duas chamadas | Ver pedidos de socorro em alturas diferentes e uma rota curta vigiada | Definir ordem de resgate e administrar combustível | Completar a primeira operação e revelar o mapa polar |

Metas iniciais propostas para sessões observadas com novatos são primeiro controle em até 20 s, primeiro objetivo reconhecível em até 40 s e primeiro resgate em até 3 min. Esses tempos serão ajustados após observação. Uma sequência longa de diálogos antes de pilotar deve ser encurtada durante a revisão.

Depois de cada missão, a tela de resultado informa o que foi concluído, mostra a alteração na base e permite iniciar a próxima. O mapa apresenta a missão seguinte disponível e a anterior rejogável. As instruções surgem perto do momento de uso e podem ser revistas ou puladas.

## Combate leve e evasão

### Regras propostas

A arma de resgate é um **pulso eletromagnético**. A primeira família recebe um pulso e fica desativada por alguns segundos. Famílias posteriores expõem proteção ou um ponto vulnerável legível. Cada encontro deve admitir uma decisão de rota, momento de avanço ou uso do pulso.

Valores de protótipo para teste, ainda sujeitos a balanceamento.

| Elemento | Ponto inicial de ajuste |
|---|---|
| Pulso | Alcance de 300 unidades do mundo, intervalo de 0,8 s entre disparos |
| Bateria | 4 cargas, recuperação de uma carga a cada 3 s fora de alerta, recarga na base |
| Desativação de máquina leve | 4 s, com luzes apagadas e animação de recuperação |
| Aviso de primeiro ataque | Pelo menos 1 s no capítulo inicial |
| Janela de reação posterior | Começar em 0,8 s e ajustar por playtest |
| Escudo da história | 2 cargas contra projéteis, recuperação na base |
| Proteção após impacto | 1 s para evitar vários acertos no mesmo contato |

Esses parâmetros pertencem à campanha. O resultado do pulso, o gasto de bateria e o dano são decididos pelo núcleo. Efeitos visuais acompanham o evento correspondente.

O escudo da história torna o primeiro contato com projéteis recuperável. As colisões com terreno continuam a seguir regras de pouso e impacto definidas para a missão. O tutorial mostra o consumo de uma carga de escudo em um encontro controlado. Combustível, escudo e bateria devem ter indicadores pequenos e distintos.

### Controles propostos

No desktop, `F` dispara e as teclas atuais comandam o voo. No Android, um botão de pulso fica ao alcance do polegar direito, com alvos de toque testados no aparelho. A proposta de acessibilidade oferece disparo assistido opcional contra a máquina que está no cone frontal e dentro do alcance. O primeiro protótipo compara o disparo manual com essa assistência antes de definir o padrão do celular.

A campanha explica que uma máquina está dentro do cone de tiro por uma marca discreta. A ajuda de mira afeta a escolha do alvo, enquanto a orientação e a posição da nave continuam a decidir a oportunidade de disparo.

### Famílias de inimigos

| Família | Aparência e movimento | Percepção e ataque | Aviso e resposta do jogador | Introdução |
|---|---|---|---|---|
| Patrulheiro | Corpo horizontal com duas hélices, rota de ida e volta | Detecta aproximação no corredor e dispara um projétil lento | Luz âmbar antes do tiro; passar atrás, usar cobertura ou desativar | M01 |
| Sentinela de varredura | Torre fixa com cabeça articulada e lente grande | Varre um setor com linha de visão limitada pelo terreno | Feixe de busca azul passa a âmbar; atravessar entre varreduras ou usar pulso | M02 |
| Caçador térmico | Corpo estreito, cauda luminosa e curva de perseguição ampla | Investiga o uso prolongado do motor em uma área | Som crescente e cauda acesa; planar até uma cobertura ou desativar | M05 |
| Escavador descontrolado | Máquina pesada com broca e apoio no terreno | Surge em pontos marcados e bloqueia temporariamente a passagem | Poeira e vibração antes da saída; esperar, buscar outra rota ou atingir a abertura exposta | M08 |
| Sabotador | Drone com braços de ferramenta e luz violeta | Vai até uma antena ou instalação, obrigando a proteger um objetivo | Caminho e alvo sinalizados; interceptar ou concluir a ativação antes dele | M10 |
| Enxame de manutenção | Três unidades pequenas formando um triângulo | Cerca uma área e se reorganiza lentamente | Formação se abre antes da investida; atravessar a abertura ou dispersar com pulso | M14 |
| Guardião da central | Máquina grande, módulos visíveis e movimentos lentos | Alterna varredura, reposicionamento e exposição de um módulo | Abrigar-se, desativar módulos e abrir o corredor da evacuação | M17–M18 |

Cada família precisa ser reconhecível pela silhueta em escala real de jogo. Cor, animação e áudio reforçam essa leitura. O Caçador perde o alvo e retorna a uma área definida após alguns segundos, evitando perseguição interminável. O Escavador tem pontos de saída fixos na definição da fase. O Sabotador anuncia uma janela de recuperação caso alcance o objetivo. O enxame tem população limitada e pontos de surgimento definidos na missão.

O guardião reutiliza percepção, ataque e desativação das famílias anteriores. Sua luta é curta e permite reiniciar a partir da entrada do encontro. O desafio final combina habilidades já ensinadas. M17 desativa o sistema de defesa principal; M18 atravessa os setores ainda sobrecarregados enquanto a equipe conclui a evacuação. O guardião imobilizado emite descargas anunciadas, servindo como risco ambiental durante a retirada.

## Campanha proposta

Os nomes são de trabalho. Duração é uma hipótese de ritmo para uma tentativa bem-sucedida, a aferir no celular. O total planejado é aproximadamente 80–100 min, com pausas entre missões.

| ID | Missão e ambiente | Objetivo e decisão de jogo | Encontro e novidade | Duração-alvo |
|---|---|---|---|---|
| M01 | Primeiro sinal — Marciana | Resgatar Maria em uma rota aberta; aprender a frear e pousar | Patrulheiro em corredor lateral, primeiro contato com uma máquina | 3 min |
| M02 | A torre muda — Marciana | Levar Rosa à antena e permanecer pousado durante a ativação | Sentinela, primeiro pulso e primeira instalação reativada | 4 min |
| M03 | Duas rotas, duas chamadas — Marciana | Resgatar dois grupos em plataformas diferentes e escolher a ordem | Patrulheiro e sentinela; rota longa protegida e atalho vigiado | 5 min |
| M04 | Sob a crosta — Poço Polar | Localizar um sinal sob o gelo e resgatar a equipe | Visibilidade reduzida e varredura da sentinela atravessando a névoa | 4 min |
| M05 | Motor em silêncio — Poço Polar | Cruzar um trecho em que acelerar atrai uma máquina | Caçador térmico; impulso curto, planeio e cobertura | 5 min |
| M06 | A estação enterrada — Poço Polar | Reativar um repetidor e retirar os sobreviventes em duas viagens | Caçador e sentinela, primeiro objetivo em duas etapas | 6 min |
| M07 | Carga de emergência — Veio de Ferro | Transportar uma bateria da base até um posto | Carga ocupa lugar na nave; abastecimento e ordem da entrega | 4 min |
| M08 | A broca acordou — Veio de Ferro | Alcançar trabalhadores presos e contornar uma máquina escavadora | Escavador, vibração prévia e ponto vulnerável | 5 min |
| M09 | O último vagão — Veio de Ferro | Recuperar equipamento e evacuar um grupo por rotas opostas | Escavador e patrulheiro; alternar transporte e resgate | 6 min |
| M10 | Faróis nas dunas — Mar de Dunas | Reativar dois faróis para localizar a rota de uma caravana | Sabotador e rajadas; proteger a instalação ou avançar | 4 min |
| M11 | A caravana parada — Mar de Dunas | Fazer resgates em dois pontos separados por uma área exposta | Caçador e vento regional; ler abrigo e impulso | 5 min |
| M12 | Janela de passagem — Mar de Dunas | Levar um módulo de rádio por uma rota que muda com a tempestade | Sabotador e sentinela; escolher uma janela de avanço | 6 min |
| M13 | Debaixo do basalto — Tubos de Lava | Explorar uma rota entre jatos térmicos e sinalizar uma saída | Sentinela e risco térmico previamente anunciado | 5 min |
| M14 | Ninho de máquinas — Tubos de Lava | Recuperar uma peça para a central e libertar a rota de saída | Enxame, pulso para dispersar e corredor alternativo | 6 min |
| M15 | Pontes quentes — Tubos de Lava | Fazer resgate em sequência com pausas em áreas seguras | Escavador e enxame; combinar leitura do terreno e do ataque | 6 min |
| M16 | Luzes da colônia — Colônia Isolada | Restaurar energia de um abrigo e começar a evacuação | Sabotador e sentinela em instalações reconhecíveis | 5 min |
| M17 | O guardião da central — Colônia Isolada | Desativar módulos em uma ordem indicada pelo estado da máquina | Guardião, encontro em duas fases e ponto de retomada próprio | 6 min |
| M18 | Todos a bordo — Colônia Isolada | Manter o corredor aberto e retirar o último grupo | Guardião avariado, patrulheiros limitados e evacuação final | 8 min |

### Regras de variedade

- Toda missão introduz ou recombina pelo menos dois elementos entre objetivo, terreno, inimigo, rota e acontecimento narrativo.
- A mesma família entra primeiro sozinha, ganha uma combinação depois e retorna com função diferente em outro ambiente.
- Cada missão tem uma imagem reconhecível, um marco de navegação e um acontecimento autoral. Exemplos são a antena que acende, a estação revelada sob o gelo e a broca abrindo a passagem.
- A composição de ameaças é definida por missão. A combinação de ataques deve preservar pelo menos uma saída utilizável de cada área segura.
- Inimigos desativados oferecem tempo suficiente para passar. Máquinas importantes reaparecem em estados anunciados.
- Objetivos secundários incentivam revisitas, como localizar um registro ou resgatar um grupo por uma rota alternativa. A conclusão principal abre a próxima missão.

## Falha, retomada e objetivos recuperáveis

A missão termina ao completar todos os seus objetivos obrigatórios. Objetivos sequenciais anunciam a próxima etapa, enquanto resgates com ordem livre mostram os destinos disponíveis. Os objetivos secundários ampliam o resultado da missão.

A proposta inicial usa pontos de retomada na entrada de cada missão e na entrada do encontro com o guardião. Ao esgotar as vidas, reinicia-se a partir desse ponto com os recursos e objetos previstos naquele estado. O progresso das missões já concluídas é preservado. Ao reabrir o aplicativo depois de uma interrupção do sistema, o jogador retoma esse ponto salvo.

Passageiros afetados pela destruição da nave voltam aos pontos de resgate da tentativa. Objetos de missão voltam ao ponto de coleta ou ao inventário do ponto de retomada. A montagem deve validar espaço de carga suficiente para completar cada sequência obrigatória.

Os inimigos atacam a nave e instalações. Passageiros aguardando resgate permanecem disponíveis. O Sabotador interrompe a instalação e anuncia a necessidade de reativação, oferecendo uma nova janela de ação. A falha faz o objetivo voltar a um estado recuperável, preservando o avanço das outras etapas concluídas conforme a definição da missão.

O tempo das tempestades cria janelas de passagem. As primeiras missões dão tempo para reconhecer o cenário e entender o objetivo, com ritmo guiado pelos encontros e pelos deslocamentos. Uma janela perdida volta a aparecer ou mantém uma rota de abrigo utilizável. Missões com bloqueio por combate sempre permitem recuperar bateria em local seguro.

## Identidade visual dos seis ambientes

| Ambiente | Luz, cor e geometria | Quatro marcos de cenário | Movimento ambiente e áudio |
|---|---|---|---|
| Marciana | Ferrugem e luz quente; saliências largas, silhuetas abertas | Antena, sonda avariada, painel de energia, depósito de emergência | Poeira baixa, cabos leves e ruído de rádio |
| Poço Polar | Azul frio com pontos âmbar; cristais e paredes estratificadas | Sonda congelada, coluna de gelo, cabo soterrado, torre isolada | Cristais se desprendendo, névoa lenta e estalos |
| Veio de Ferro | Cinza metálico e cobre; faces angulares e corredores industriais | Guindaste, vagonete, broca, cargueiro tombado | Correntes, faíscas pontuais e reverberação metálica |
| Mar de Dunas | Ocre claro e sombras violeta; arcos e dunas visíveis ao fundo | Vela de estação, sensor de vento, carga presa, abrigo de caravana | Areia em rajadas e tecido tensionado |
| Tubos de Lava | Basalto escuro e fendas luminosas; pontes e cavernas profundas | Tubulação, comporta, dissipador, ponte de basalto | Calor ondulante, fumaça localizada e pulsos graves |
| Colônia Isolada | Luzes humanas verdes e brancas em módulos escuros | Módulo médico, central, painel da colônia, portões | Máquinas retomando atividade, portas e rádio restabelecido |

Mar de Dunas exige uma composição própria para o exterior. Os limites físicos precisam ficar legíveis como terreno ou limite navegável. O piso lógico usa a geometria suportada, enquanto a aparência é validada sobre ela.

## Plano de assets

As quantidades abaixo são metas de produção. **Modelo, combinação de material e quadro de animação são unidades diferentes**. Reaproveitar um modelo entre sequências mantém o orçamento viável.

| Grupo | Meta para a campanha completa | Reaproveitamento e produção |
|---|---|---|
| Fundos | 6 ambientes, cada um com 3 camadas de paralaxe | Exportar as camadas separadamente, com transparência e profundidade coerentes |
| Terreno | 2 superfícies e pelo menos 2 silhuetas de borda por ambiente | Kit modular com rocha, gelo, metal, areia, basalto e estruturas |
| Marcos de cenário | 24 modelos, quatro por ambiente | Antena, sonda e guindaste existentes são candidatos a reaproveitamento; produzir os demais a partir do catálogo acima |
| Asteroides e destroços | 8 modelos base, distribuídos em 24 combinações escolhidas de material e forma | Cada ambiente usa pelo menos duas silhuetas. Os meteoros têm leitura própria de movimento |
| Inimigos comuns | 6 modelos principais, 2 variantes visuais legíveis de cada | A segunda variante preserva o comportamento principal e muda um parâmetro anunciado, como setor de varredura |
| Guardião | 1 modelo modular com 2 fases e estado avariado | Módulos partilham o sistema de dano e desativação |
| Passageiros | 3 arquétipos existentes, com variações de equipamento | Espera, reação, aceno, caminhada, proteção e comemoração reaproveitam o modelo da profissão |
| Personagens no rádio | 5 retratos e expressões básicas | Maria, Rosa, Silva, operador da base e responsável pela colônia |
| Instalações | Antena, repetidor, farol, posto de carga, abrigo e central | Estados apagado, ativando, ativo e danificado |
| Objetos de missão | Bateria, rádio, peça da central e registro de exploração | Ícones e aparência no mundo compartilham a mesma identificação |
| Efeitos | Pulso, impacto em escudo, ataque de máquina, desativação, recuperação, calor, gelo e poeira | Biblioteca compartilhada com variações de intensidade |
| Áudio | Assinatura de cada família, ambiente por capítulo, pulso e estados da base | Avisos curtos distinguíveis durante o motor; volume e repetição revisados no celular |

### Sequências dos inimigos

Cada família precisa de repouso, deslocamento ou varredura, detecção, preparação, ataque, impacto, desativação e recuperação. Preparação e recuperação devem ser distinguíveis pelo corpo e pela luz. A folha de direção e os efeitos podem ser separados quando isso preservar os pivôs e a iluminação. A produção evita multiplicar todos os efeitos por todas as direções sem necessidade.

O protótipo começa com 8–16 direções para corpos móveis e 6–12 quadros para ações articuladas, ajustando depois pelo tamanho real na tela. A sentinela usa posições de articulação próprias. O guardião tem folhas por módulo e fase, carregadas apenas no encontro.

### Pipeline Blender e QA de arte

1. Modelar a silhueta e testar sua leitura no tamanho real do aparelho.
2. Definir pivô, região de contato, ponto de disparo e limites antes da animação.
3. Aprovar um ciclo completo de uma família antes de renderizar suas variantes.
4. Renderizar com luz e câmera coerentes com as naves. Exportar folhas, manifesto e origem dos assets.
5. Conferir dimensões, alfa, margens, alinhamento dos pivôs e transições entre estados.
6. Rever vídeo contínuo com personagem, nave, HUD, terreno e efeitos juntos.
7. Conferir desempenho e legibilidade antes de expandir o lote seguinte.

Cada inimigo precisa de pelo menos um encontro filmado com aproximação, aviso, ataque, reação do jogador e recuperação. O objetivo é reconhecer a ameaça antes de sofrer o efeito.

## Progresso, narrativa e repetição

A base muda após os resgates. Maria orienta rotas, Rosa reativa equipamentos e Silva interpreta os registros encontrados. Essas participações aparecem em mensagens curtas e alterações visuais nos postos.

Salvar ao concluir a missão, abrir um capítulo e sair pela opção de voltar ao mapa. Cada missão tem identificador estável e versão. O primeiro protótipo reinicia a missão atual a partir do ponto de entrada; encontros longos do capítulo final recebem uma retomada definida na missão.

A proposta inicial oferece campanha local disponível sem conta. O progresso é separado por perfil local. Recompensas iniciais são registros, insígnias e aparências da campanha. Uma futura integração com saldo online exige um contrato específico de validação.

Rejogar uma fase permite melhorar o resultado e completar objetivos secundários. O progresso nunca retrocede por uma tentativa malsucedida. Reiniciar toda a campanha exige uma ação explícita do jogador.

## Organização técnica proposta

| Área | Responsabilidade e ponto de integração |
|---|---|
| Núcleo da campanha | Definições de missão, objetivos, etapas, conclusão, inimigos, pulso e escudo |
| Montagem de cenário | Reutilizar `Caverna`, plataformas e descrições de terreno; montar passageiros conforme a missão |
| Sessão de missão | Coordenar objetivos e condições de vitória; concluir uma operação de resgate pode ser uma etapa da missão |
| Apresentação | Mapa de capítulos, rádio, objetivos atuais, resultado e recursos de combate |
| Progresso | Persistência local versionada, gravação atômica e recuperação de cópia válida |
| Recursos | Manifestos de capítulos, carregamento assíncrono e liberação das texturas do capítulo anterior |
| Validação | Roteiros determinísticos do núcleo, entrada real, vídeo e medições no Android |

Pontos candidatos a criação são `nucleo/.../historia/`, `core/.../historia/`, `assets/historia/` e `arte/blender/historia/`. As interfaces exatas serão definidas no plano técnico depois da aprovação desta proposta.

`PartidaDePilotagem` encerra a partida quando todos os passageiros são entregues. Missões com ativação, carga ou encontro final exigem separar conclusão de resgate e conclusão da missão, preservando o contrato usado pelos modos atuais. A montagem da história deve permitir quantidades e tipos definidos no conteúdo, sem depender das restrições do sorteio da Horda.

Os resultados da campanha são gravados no progresso local próprio. O envio de partidas reconhecidas pelo servidor permanece responsabilidade dos fluxos online. O menu e a seleção da campanha precisam de um caminho de entrada próprio, pois os enums atuais também fazem parte de contratos da API.

### Definição de conteúdo

Cada missão descreve identificador, capítulo, terreno, ponto inicial, personagens, sequência de objetivos, encontros, avisos, pontos de retomada, recompensas e referências de arte. As máquinas recebem posição, rota ou região de atuação, percepção, alvo, aviso e parâmetros de ataque.

Validar referências ausentes, identificadores repetidos, regiões inacessíveis, objetivos impossíveis, bateria insuficiente para um bloqueio obrigatório e encontros que fecham simultaneamente todas as saídas. Testar as fases com sementes fixas e comportamento determinístico.

### Progresso confiável

Usar identificador e versão da campanha no save. Serializar escritas de progresso, gravar em arquivo temporário e substituir de forma atômica quando suportado. Manter uma cópia válida. Ler saves antigos e lidar com conteúdo que mudou sem apagar silenciosamente o progresso. Conclusão repetida de missão deve ser idempotente, inclusive para recompensas locais.

### Fluidez

Proposta inicial de orçamento, a validar no primeiro capítulo, é 60 FPS no aparelho de referência, tempo de quadro no percentil 95 até 20 ms e até 96 MiB de texturas ativas da campanha. Medir o custo total do aplicativo antes de fixar limites por aparelho.

Carregar o capítulo ativo e os recursos compartilhados. Uma transição de capítulo deve mostrar progresso de carregamento e liberar recursos anteriores. Conferir aparelho de referência e um Android de capacidade intermediária quando estiver disponível. A análise estática da API mínima complementa o teste de desempenho no aparelho.

## Prioridade e calendário

O prazo acadêmico registrado para o projeto é 03/12/2026. A base já publicada e os materiais da Sprint 5 são o marco de referência dessa entrega. Minha recomendação é tratar a campanha como expansão e fechar as datas por capítulo depois de medir o esforço real do protótipo.

O compromisso de produção proposto é progressivo. Primeiro aprovar este desenho, depois medir o encontro protótipo e a missão M01, e então estimar o primeiro capítulo. O volume total de dezoito missões é o alvo do produto; os cinco capítulos seguintes passam por replanejamento após o playtest de M01–M03. Se houver uma data própria para a campanha, o recorte de lançamento deverá ser conciliado com ela antes de agendar a produção.

## Entregas por etapa

Cada etapa é candidata a um PR próprio e usa um worktree irmão criado a partir de `origin/main`. Commits, push, merge, deploy e tags dependem da autorização do usuário. O Blender roda em segundo plano e a revisão visual usa renders e vídeos. Se o sandbox bloquear o Git, interromper a operação e informar o usuário.

| Etapa | Entrega demonstrável | Critério para avançar |
|---|---|---|
| 0 — Consolidar a base | Arte atual revisada e referência de comportamento registrada | Confirmar integração do pacote de animações e sua revisão visual |
| 1 — Protótipo de encontro | Patrulheiro, pulso, escudo e uma arena curta de resgate | Novato reconhece o ataque, consegue desviar e entende o pulso no celular |
| 2 — Missão vertical | M01 completa, com entrada pela campanha, rádio, objetivo e progresso salvo | Fechar e reabrir o app preserva a conclusão; vídeo do ciclo completo aprovado |
| 3 — Primeiro capítulo | M01–M03, sentinela, antena, mapa e resultados | Playtest observa a passagem da primeira para a terceira missão e revisa pontos de abandono |
| 4 — Exploração e perseguição | M04–M06, leitura de sinais, gelo e Caçador | Detecção, perda de alvo e recuperação têm regras legíveis e rota viável |
| 5 — Carga e máquina pesada | M07–M09, carga, posto de entrega e Escavador | Objetivos combinados concluem e salvam corretamente; fase suporta retomada |
| 6 — Exterior e sabotagem | M10–M12, rajadas por região e Sabotador | Limites externos visíveis, recuperação de falha de instalação e uso confortável dos controles |
| 7 — Risco térmico e enxame | M13–M15, jatos de calor e Enxame | Eventos anunciam a janela de risco, população e projéteis ficam limitados |
| 8 — Evacuação final | M16–M18, Guardião e epílogo | Duas fases testadas, retomada do encontro e conclusão da campanha persistida |
| 9 — Revisão da campanha | Ritmo, desempenho, transições, áudio e acessibilidade | Revisão contínua dos 18 níveis e pacote final aprovado |

Cada passagem de etapa inclui build, testes pertinentes, execução real e evidências em `docs/evidencias/historia/etapa-XX/`. A revisão do agente sobre a sequência contínua precede o convite para o usuário avaliar no aparelho. A validação no celular existente e os marcos de entrega acadêmica permanecem dependências a conciliar antes de agendar a expansão.

## Medir antes de expandir

No primeiro capítulo, observar quantas pessoas iniciam M02 após M01 e M03 após M02, tempo até o primeiro resgate, mortes por causa, uso do pulso, compreensão do objetivo e pausas para consultar controles. Em sessões pequenas, apresentar os resultados como observação de playtest, com tamanho da amostra explícito.

Se alguém repetir a mesma morte sem perceber o aviso, revisar o aviso e o encontro. Se terminar uma missão sem saber qual é a próxima, revisar a tela de resultado e o mapa. Se M02 parecer igual à M01, mudar a decisão exigida e o papel da ameaça antes de produzir capítulos adicionais.

Para medir retorno em dias diferentes será necessário definir uma coleta de métricas em uma versão publicada. O plano atual usa execução local e sessões observadas, sem pressupor que essa coleta já exista.

## Pontos para aprovação

- Seis capítulos e dezoito missões como alvo completo, com primeiro capítulo como porta de validação.
- Elenco de seis famílias de máquinas e um guardião, com evasão e pulso de defesa.
- Campanha local inicialmente, com progressão própria.
- Controles do pulso a escolher após comparar a opção manual e a assistida no protótipo.
- História e nomes apresentados como ponto de partida para a escrita final.

Este documento é o desenho de produto e produção de conteúdo. A implementação começa após aprovação e elaboração do plano técnico da primeira etapa.
