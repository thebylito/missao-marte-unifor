# M01 — Primeiro sinal

Implementação em historia-m01, baseada em origin/main d0a2d71, com o protótipo de encontro como commit anterior na branch preparada para PR. Evidência registrada antes da preparação dos commits e PRs autorizados. Merge, tag e deploy seguem pendentes.

## Implementado

Entrada pelo botão 9. Modo História, mapa do capítulo, briefing curto de Maria, missão de resgate e retorno, resultado próprio, indicação de M02 em preparação e replay. Baliza azul e posto da expedição foram criados no Blender em segundo plano. O sinal da base muda quando Maria embarca e o rádio reconhece a entrega.

Progresso local do perfil Expedição em historia/expedicao.properties, separado de rankings e saldo. Escritas serializadas fora da thread gráfica, arquivo temporário e substituição atômica quando suportada, cópia de recuperação e identificação de versão. Saves futuros em qualquer cópia ficam protegidos. Dois arquivos inválidos permitem arquivar os originais e começar novamente mediante confirmação na interface. Erros transitórios não habilitam esse reinício.

O Android fornece o diretório privado do aplicativo; o desktop fornece a pasta de dados calculada pelo launcher, inclusive nos pacotes nativos.

## Provas locais concluídas

- 546 testes passaram, sem falhas ou erros, incluindo voo completo da M01 sem reposicionar a nave ou perder vida.
- Build do APK e da instrumentação passou; [build.log](build.log).
- Verificação de API mínima passou sem referências acima da API 26; [api26.log](api26.log). A referência herdada StringBuilder.setLength permanece não resolvida pelo analisador.
- Render e verificação das folhas: [blender.log](blender.log).
- Revisão independente identificou caminho de dados desktop, backup futuro, recuperação, feedback do menu e insuficiência do teste de replay. Foram corrigidos com regressões. Os testes também cobrem rádio após vitória e preservação dos arquivos arquivados.

## Android — emulador e aparelho físico validados

A primeira tentativa foi interrompida pela desconexão USB. Não terminou o voo e não produziu PASS. O log está em [android-interrompido.log](android-interrompido.log). As capturas parciais não comprovam a missão completa nem o save após reabrir.

Por solicitação do usuário, a validação foi concluída no AVD Small_Phone_API_36 (Android 16/API 36), sem janela e com renderização SwiftShader. [Ambiente e hashes dos APKs](emulador-01/ambiente.txt).

O roteiro percorreu menu, briefing, voo contínuo, embarque, retorno e resultado salvo, sem alterar posição, combustível, vida, escudo ou progresso. Depois encerrou o processo, reabriu a campanha, iniciou um replay, abortou e verificou a conclusão e a melhor pontuação. Ambos retornaram PASS. O resultado foi 32 pontos, preservados após o replay.

- [Vídeo do voo completo](emulador-01/voo.mp4) e [log](emulador-01/voo.txt).
- [Vídeo da reabertura e replay](emulador-01/reabrir.mp4) e [log](emulador-01/reabrir.txt).
- [Sequência revisada](emulador-01/contato-voo.jpg), [pouso em detalhe](emulador-01/contato-pouso.jpg), [resultado](emulador-01/voo-07.png) e [campanha reaberta](emulador-01/reabrir-02.png).

A revisão por quadros cobriu a decolagem, a passagem pela patrulha, aproximação, embarque e entrega. A execução mediu 2.815 quadros, com 91 intervalos acima de 50 ms e máximo de 298 ms. A renderização por software apresentou engasgos: esses números descrevem o emulador e não certificam fluidez no aparelho físico. O aceite humano de conforto, clareza e diversão continua pendente.

## Rodada no celular físico

O usuário reconectou o Samsung SM-S928B. O mesmo APK do emulador foi reinstalado no pacote separado, sem limpar os dados. [Ambiente e hashes](celular-02/ambiente.txt).

- Voo completo, resgate de Maria e gravação da conclusão: PASS. [Vídeo](celular-02/voo.mp4) · [Log](celular-02/voo.txt).
- Encerramento, reabertura, início e abandono de replay, mantendo a conclusão e a melhor pontuação: PASS. [Vídeo](celular-02/reabrir.mp4) · [Log](celular-02/reabrir.txt).
- Resultado da rodada: 32 pontos. O mapa preservou o melhor resultado de 34 pontos. [Resultado da rodada](celular-02/voo-07.png) · [Mapa após reabertura e replay](celular-02/reabrir-02.png).
- A execução registrou 7.330 quadros, 6 intervalos acima de 50 ms e máximo de 74 ms. A emulação anterior registrara 2.815 quadros, 91 intervalos acima de 50 ms e máximo de 298 ms. São intervalos entre atualizações instrumentadas, não medições isoladas da GPU.
- [Sequência revisada](celular-02/contato-voo.jpg): decolagem, patrulha, pouso, embarque e retorno contínuos. Não houve reposicionamento da nave pelo ensaio.

A validação funcional passou neste aparelho; conforto dos controles e diversão continuam sujeitos ao aceite humano. A execução não altera produção nem equivale à validação da Horda exigida para a tag fase-8.

## Reproduzir

Com Java17 e Android SDK configurados:

    rtk proxy ./gradlew -I validacao/historia/android/init.gradle test :android:assembleDebug :android:assembleDebugAndroidTest
    rtk proxy python3 validacao/android/conferir_api_minima.py android/build/outputs/apk/debug/android-debug.apk --sdk /caminho/sdk --min 26
    rtk proxy adb install -r android/build/outputs/apk/debug/android-debug.apk
    rtk proxy adb install -r android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
    rtk proxy python3 validacao/historia/executar.py --adb /caminho/adb --dispositivo SERIAL --saida docs/evidencias/historia/etapa-02/rodada-final

O init.gradle usa o pacote separado br.edu.unifor.missaomarte.historia, com o nome História · Missão Marte. O APK normal mantém o identificador original.

## Decisões e limites

- Perfil local único por enquanto: simplifica esta entrega; múltiplos jogadores no aparelho compartilham progresso.
- A M01 depende do PR do encontro. O PR usa essa branch como base para mostrar somente sua própria etapa; deve ser retargetado para main após a integração anterior.
- A arte extensa de arte-poco-polar continua separada; estes novos marcos usam a base publicada do encontro.
- O pedido de prosseguir autorizou implementação da M01, sem converter automaticamente o ensaio anterior em aceite humano.
- A direção de produto e a persistência local estão registradas em ai-memory, decisions/campanha-local.md. Progresso diário continua nos registros da sessão.
- O uso de worktree Git manual cumpre a pasta irmã exigida; a ferramenta nativa não oferece seleção do caminho.
- Reclassifiquei rádio contraditório após a entrega e ausência da mudança do sinal como requisitos incompletos da M01 e corrigi ambos. Não restaram ajustes menores aceitos e adiados nesta revisão.
- Diversão para novatos, ergonomia e desempenho em outros aparelhos dependem de avaliação manual. O gate funcional Android passou no emulador e no celular físico; o aceite humano de conforto e diversão continua aberto.
