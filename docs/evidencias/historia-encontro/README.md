# Encontro do modo História — evidências

Protótipo da etapa 1 do plano aprovado. Branch historia-encontro-prototipo, criada de origin/main em d0a2d71. Validação registrada antes da preparação do PR; commit e push foram autorizados depois. Merge e deploy seguem pendentes. O pacote visual da branch arte-poco-polar é separado e não está incorporado aqui.

## O que foi implementado

Arena curta com um patrulheiro, aviso anterior ao ataque, projéteis, pulso EMP frontal, duas cargas de escudo, quatro cargas de bateria e resgate de Maria. Entrada pelo botão Encontro protótipo ou tecla 8 no menu. F/PULSO dispara; G/Auto alterna assistência. A assistência somente dispara contra alvo válido no cone frontal. Resultados temporários, sem submissão ao servidor e sem gravar o ranking normal.

Patrulheiro, projétil e pulso foram modelados e renderizados no Blender em segundo plano. O catálogo e o manifesto registram os três recursos; nenhum aplicativo de desktop com janela foi aberto.

## Verificação

- 525 testes passaram na suíte completa; zero falhas ou erros. Log: [testes.log](testes.log).
- APK e pacote de instrumentação compilados. [build-android.log](build-android.log).
- Verificador do bytecode encontrou zero referências acima da API 26. Uma referência herdada não resolvida pelo verificador, StringBuilder.setLength, permanece indicada no [log](api-26.log).
- Validação de dimensões, alpha, frames e orientação dos sprites passou. [blender.log](blender.log).
- A revisão independente e o tratamento dos achados estão em [REVISAO.md](REVISAO.md). Os testes de toque breve e pausa falharam antes da correção: [regressoes-red.log](regressoes-red.log).

## Ensaio no aparelho

Samsung SM-S928B conectado por ADB. Aplicativo separado: Encontro · Missão Marte, package br.edu.unifor.missaomarte.historia. O init.gradle muda somente o build de validação; o APK normal conserva o identificador do jogo principal.

A instrumentação passa pela entrada e menu reais. Testa dois dedos em motor e pulso, soltura independente, pausa, mudança entre retrato e paisagem, reabastecimento, impacto absorvido pelo escudo, pulso, embarque, entrega e resultado local. As posições da nave são preparadas entre os cenários. O voo, a queda, o pouso, o combate e o embarque avançam pela simulação real. Isso não substitui o playtest de um jogador atravessando a arena inteira.

A primeira rodada passou no combate e resgate. Na ampliação do ensaio, a rodada 02 exigia recarga antes de a nave terminar o pouso; a rodada 03 supunha que um projétil antigo, disparado para outra posição, atingiria a nave. Foram corrigidas as condições do roteiro, preservando os comportamentos reais do jogo e os registros das falhas.

A gravação pode conter pausas de rotação, capturas e reposicionamentos controlados. Os intervalos medidos na instrumentação não equivalem a um benchmark de GPU nem comprovam desempenho em outros celulares.

## Rodada final

A [rodada 04](rodada-04/instrumentacao.txt) terminou com PASS, inclusive toques simultâneos, soltura independente, pausa, rotação, recarga, combate e entrega. Foram 2.287 quadros medidos, nenhum intervalo acima de 50 ms e máximo de 49 ms. Isso descreve somente esta execução instrumentada neste aparelho.

[Vídeo completo](rodada-04/encontro.mp4) · [Quadros revisados](rodada-04/contato.jpg) · [Resultado](rodada-04/14.png)

## Reproduzir

Na raiz deste worktree, com Java 17 e Android SDK configurados:

    rtk proxy ./gradlew test
    rtk proxy ./gradlew -I validacao/android/encontro/init.gradle :android:assembleDebug :android:assembleDebugAndroidTest
    rtk proxy adb install -r android/build/outputs/apk/debug/android-debug.apk
    rtk proxy adb install -r android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
    rtk proxy python3 validacao/android/encontro/executar.py --adb /caminho/adb --dispositivo SERIAL --saida docs/evidencias/historia-encontro/nova-rodada

Na primeira instalação, a configuração do identificador foi aplicada cedo demais e produziu o pacote principal. O build foi corrigido via finalizeDsl e o APK anterior de arte-poco-polar foi reinstalado, sem limpeza de dados. Os ensaios seguintes usam o pacote separado.

## Aceite pendente

Avaliação manual de clareza do aviso, conforto de mirar e desviar, preferência entre pulso manual e assistido e diversão do encontro. A campanha de 18 missões, seus capítulos e famílias posteriores de máquinas pertencem às próximas etapas do plano.
