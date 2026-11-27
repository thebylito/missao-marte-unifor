# Ambientação das cavernas

Base `d0a2d71`, branch `arte-poco-polar`. JDK 17, Android SDK local, Blender 5.2.3 LTS em segundo plano.

## Conteúdo

- Quatro modelos novos de asteroide com 32 ângulos. Variantes estáveis por posição na lista da caverna. O Clássico alterna a rocha anterior e a craterada.
- Doze sequências dos passageiros. Espera em 16 quadros, reação, sinalização e caminhada em 12 quadros cada. As folhas são de 64 × 80 px por célula, com os pés na borda inferior e margem para os braços. A engenheira se agacha junto às ferramentas. O astronauta examina amostras. O professor consulta registros e ergue uma lanterna.
- A antena, a sonda e o guindaste têm oito quadros de movimento e luz de presença.
- O alcance do aceno tem histerese de 270 a 320 unidades, evitando alternar repetidamente entre poses quando a nave oscila no limite. Cada passageiro tem uma defasagem no ciclo de espera.
- Paredes e fundos do Poço Polar e Veio de Ferro. Antena, sonda e guindaste identificam as três cavernas.
- Partículas sob o jato perto das plataformas, usando a rocha do ambiente.
- Balizas de borda que pulsam durante a espera e acendem juntas por 0,7 s após embarque.
- Sons ambientes discretos e um segundo som de embarque.

## Verificação automática

`./gradlew --offline build :android:assembleDebug` passou com 518 testes JVM e lint Android. O catálogo do Blender passou em 47 entradas. As 14 folhas novas estão presentes no APK, iguais às imagens verificadas. Os 11 roteiros reais de console reproduziram o gabarito da fase 1 antes da ampliação visual.

A verificação do Blender confere também as bordas laterais e superior de todas as sequências de passageiros, além do contato dos pés. O primeiro ensaio detectou recorte do braço. A célula ganhou margem e a reação ganhou flexão do cotovelo antes de gerar novamente as folhas.

## Aparelho

Samsung SM-S928B via USB. Instrumentação posiciona a nave acima da plataforma no mapa real do Poço Polar, aciona o motor pela mesma entrada do controlador e confere pouso e embarque. O teste é local e usa a física do jogo. A execução final passou com 3.601 atualizações e 373 quadros na faixa de emissão, seguida de pouso e embarque confirmados. [Log da execução](android-instrumentacao.txt). A primeira execução corrigiu o ponto inicial que tocava o teto.

A conferência automática verifica o estado do voo e a faixa de emissão. A aparência e o volume dos sons precisam do aceite do usuário.

## Repetição

```sh
./gradlew -I validacao/android/ensaio_ambiente/init.gradle :android:assembleDebug :android:assembleDebugAndroidTest
adb install -r android/build/outputs/apk/debug/android-debug.apk
adb install -r android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
adb shell am instrument -w br.edu.unifor.missaomarte.test/ensaio.Polar
```

O executor e os dados locais estão em `validacao/android/ensaio_ambiente/`. [Captura do aparelho](celular-ambiente.png).

## Validação das sequências de personagens

O APK final contém as doze sequências corrigidas, verificadas contra os arquivos do Blender. Os três resgates no Samsung passaram, incluindo pouso e embarque. O roteiro fotografou a espera, a aproximação, a frenagem e o pouso de cada personagem. Os registros estão em [animacoes-execucao.txt](animacoes-execucao.txt), com as capturas na pasta `animacoes/`.

```sh
python3 validacao/android/ensaio_ambiente/executar.py --dispositivo SERIAL --saida /tmp/animacoes
```

A aparência, o ritmo dos gestos e a leitura no celular aguardam o aceite visual do usuário.

## Portas de embarque

As nove skins receberam folhas de porta com 12 quadros. Na Padrão, a escotilha frontal abre. Na Guará, as duas folhas laterais abrem junto ao cofre direito. A verificação do Blender confere margens e compara o quadro fechado com o de voo. Todas as nove folhas estão no APK.

A porta abre em 0,15 s. A caminhada começa após os primeiros 25% do embarque e segue até a posição da escotilha. A porta fecha em 0,22 s depois da entrada. Uma decolagem durante o embarque fecha a porta. Há cobertura para o ciclo completo, interrupção e pouso sem possibilidade de embarque.

O build passou com 522 testes JVM. O catálogo passou em 56 entradas.

No Samsung, os três resgates com Padrão, Guará e Padrão V passaram. A instrumentação conferiu abertura completa e fechamento após embarque. [Registro dos testes](portas-execucao.txt). As capturas de cada execução estão na pasta `portas/`.

O código da verificação geométrica da Guará recebeu uma correção após a geração, e o catálogo completo foi verificado novamente com sucesso. A aparência e o ritmo da porta aguardam o aceite visual do usuário.

## Ciclo de voo, base e cenário

- Suspensão modelada no Blender em oito níveis para cada uma das nove skins. O movimento de assentamento é proporcional à velocidade vertical da chegada.
- Rampa em quatro degraus ligada à abertura da porta. Os passageiros sobem e descem por ela.
- Intensidade visual do motor sobe em 0,12 s e o calor residual diminui em 0,28 s.
- Abrigo com luz de recepção e braço articulado de abastecimento. O indicador acompanha o nível do tanque.
- Depois da entrega, os passageiros descem, caminham até o abrigo e comemoram.
- Reações de proteção diante do jato próximo e de meteoros. Poeira, pequenos fragmentos e falhas ocasionais das luzes compõem o cenário.

O build passou com 526 testes JVM, incluindo motor, suspensão, braço e reação a ameaças. A verificação do Blender passou em 72 entradas. A instrumentação foi ampliada com um retorno local à base após cada resgate, partindo de uma posição de aproximação preparada para o ensaio.

## Revisão do pouso e do ritmo dos passageiros

A entrega passou a usar a posição registrada no evento, pois a nave pode pousar e decolar na mesma atualização. Um teste reproduz esse caso.

A reação de proteção persiste por 0,6 s entre pulsos do motor, evitando alternância de pose a cada quadro. A caminhada acompanha o deslocamento e o retorno após interrupção é gradual. Depois do retorno do usuário sobre imobilidade e lentidão, a cadência foi ajustada para 6 quadros/s na espera, 8 na proteção, 12 na sinalização e 20 na reação. A transição entre estados dura 0,12 s.

O build passou com 530 testes. A última revisão no Samsung concluiu resgate, retorno, entrega e abastecimento do professor com a Padrão V. O vídeo foi gravado desde o jato próximo à plataforma. O trecho anterior ao reposicionamento de ensaio foi revisado em sequência, verificando apoio na plataforma e deslocamento durante a entrada.

- [Trecho revisado](revisao-visual/pouso-revisado.mp4).
- [Quadros consecutivos](revisao-visual/sequencia-pouso.png).
- [Execução](revisao-visual-execucao.txt).

O aceite visual final do usuário está pendente.

## Aceno durante a aproximação

A proteção contra o motor passou a conferir a direção e o alcance do jato, usando a posição do bocal e do corpo do passageiro. A aproximação ao lado da plataforma conserva o aceno. A proteção contra meteoros continua ativa.

O build passou com 532 testes. No Samsung, a instrumentação confirmou o aceno em 1.381 quadros da aproximação e concluiu embarque, entrega e abastecimento. A sequência gravada foi revisada antes da comunicação ao usuário.

- [Aceno revisado](aceno-aproximacao/aceno-revisado.mp4).
- [Quadros consecutivos](aceno-aproximacao/sequencia.png).
- [Registro](aceno-execucao.txt).

## Revisão antes do PR

A revisão independente identificou progresso de caminhada preservado após um embarque, que deslocava um passageiro devolvido após explosão. A animação agora é descartada no embarque e quando a plataforma fica vazia. O teste reproduziu o defeito e passou após a correção.

Outro caso permitia salto vertical ao embarcar com a escotilha alinhada ao personagem. A subida na rampa agora depende continuamente do progresso; o teste cobre início no piso, avanço inicial menor que uma unidade e chegada à escotilha. O braço só abastece durante partida em andamento, e o teste de desembarque confere o passageiro e as coordenadas registrados.

535 testes passaram, assim como APK, distribuição desktop, catálogo Blender e análise API26. O novo ensaio no Samsung confirmou 1.386 quadros de aceno, porta, embarque, entrega, suspensão e reabastecimento. O reposicionamento entre plataforma e base pertence ao roteiro de validação. A gravação foi revisada por quadros antes da preparação do commit.

- [Log do ensaio](revisao-pr/professor.txt)
- [Gravação](revisao-pr/professor.mp4)
- [Sequência de quadros](revisao-pr/sequencia.jpg)
- [Build](revisao-pr/build.log) e [API mínima](revisao-pr/api26.log)

Commit e push foram autorizados para preparar o PR. Merge e deploy continuam pendentes de autorização.
