# Validação da Fase 8

Execute na raiz do projeto, em bash ou zsh. Use contas locais de teste. Esta revisão contém o cliente da Horda. O aceite completo depende de um servidor com Horda, eventos e loja implementados e da arte das skins no cliente.

## Servidor e contas

No checkout do **servidor integrado**, com JDK 17, suba o ensaio da caverna marciana. Em outro terminal, confirme que a resposta contém `numero` e `descricao`.

```sh
(cd servidor && ./gradlew --no-daemon bootRun --args='--missaomarte.hordas.ensaio=marciana')
```

```sh
curl -fsS http://localhost:8080/api/hordas/atual
```

Cadastre o piloto pela opção 2 do jogo, ou use uma conta existente. No terminal dos roteiros, informe o nome dessa conta e sua senha. Para disparar eventos, use uma conta que já seja administradora.

```sh
export MISSAOMARTE_PILOTO=Ana
export MISSAOMARTE_SENHA_PILOTO="$(python3 -c 'import getpass; print(getpass.getpass("Senha do piloto: "))')"
```

## Desktop e Horda (CT-25)

No macOS, use `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`. A função abaixo roda cada roteiro em uma pasta temporária e grava seus PNGs ali. Em um cliente já integrado, a arte vem na distribuição. Nesta branch, o comando `git archive` disponibiliza a arte da branch local `skins-naves` para o Hangar.

```sh
./gradlew --no-daemon :lwjgl3:installDist
L="$PWD/lwjgl3/build/install/missao-marte/lib"
J="$(mktemp -d)"
mkdir -p "$J/classes"
git archive skins-naves assets/sprites/naves | tar -x -C "$J"
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -cp "$L/*" -d "$J/classes" validacao/janela/PilotoDaJanela.java \
  nucleo/src/test/java/br/edu/unifor/missaomarte/pilotagem/PilotoAutomatico.java
A="$PWD/assets"
janela() (
  modo="$1"
  shift
  mkdir -p "$J/$modo"
  cd "$J/$modo" || exit 1
  case "$(uname -s)" in Darwin) set -- -XstartOnFirstThread "$@" ;; esac
  "${JAVA_HOME:+$JAVA_HOME/bin/}java" "$@" -cp "$J/classes:$J/assets:$A:$L/*" PilotoDaJanela "$PWD" 3 "$modo"
)
janela horda-faixa
janela horda
```

Confira a caverna, a dificuldade e a contagem regressiva em `horda-faixa`. O roteiro `horda` entra com a conta, solicita o bilhete por H, espera na base e mantém o motor ligado até a derrota para fotografar voo e fim. Para validar **vitória**, pilote com as setas desde a base, resgate todos os passageiros e confira colocação, +5 moedas e saldo no print final. Consulte o Top 10 pelo número que apareceu na faixa (preserva a consulta mesmo após a virada).

```sh
janela horda -Dmissaomarte.vooManual=true
printf 'Número da Horda jogada: '; read -r N
curl -fsS "http://localhost:8080/api/ranking?modo=HORDA&horda=$N"
```

## Eventos ao vivo (CT-26)

Em um terminal, conecte um segundo piloto. O fantasma cria a conta se necessário. Sem `--sala`, usa o handshake antigo. Também aceita `--sala livre` ou um número explícito, como `--sala horda-42`.

```sh
python3 validacao/sombras/piloto_fantasma.py --nome Fantasma --sala atual --segundos 180 --sobreviver --pegar-vip
```

Abra um voo com conta. Em outro terminal, dispare o evento enquanto ambos estiverem no canal. Compare o aviso e o início no jogo com o horário e o identificador registrados pelo fantasma. Após o perigo, confira os pontos semanais. Repita com VIP e confira o vencedor em `vip-pego`.

```sh
export MISSAOMARTE_SENHA_ADMIN="$(python3 -c 'import getpass; print(getpass.getpass("Senha do admin: "))')"
python3 validacao/eventos/disparar_evento.py --nome Admin --tipo TEMPESTADE
# Depois do fim da tempestade, com os dois pilotos ainda em voo:
python3 validacao/eventos/disparar_evento.py --nome Admin --tipo PASSAGEIRO_VIP
curl -fsS http://localhost:8080/api/ranking/semanal
```

## Hangar e sombra (CT-27)

Use uma conta com saldo para o preço de `guara` em `GET /api/loja`. Confira saldo, posse e skin equipada antes e depois. Repita a compra para conferir que o saldo permanece igual. O roteiro da janela abre o Hangar com 7, navega com a seta e grava dois PNGs em paisagem.

```sh
python3 validacao/loja/comprar_e_equipar.py --nome "$MISSAOMARTE_PILOTO" --skin guara
janela hangar
MISSAOMARTE_SENHA_FANTASMA="$MISSAOMARTE_SENHA_PILOTO" python3 validacao/sombras/piloto_fantasma.py \
  --nome "$MISSAOMARTE_PILOTO" --skin guara --sala livre --segundos 120
```

Em outro aparelho, voe no Livre com uma conta diferente e confira a Guará na sombra. O fantasma mostra no log a skin recebida do outro piloto. A opção `--skin` anota a expectativa local.

## Android real (CT-28)

Com o SDK em `ANDROID_HOME`, um aparelho USB autorizado e o servidor local ativo, use o encaminhamento do ADB. Para conferir a arte das skins no APK, gere-o em um cliente que inclua `assets/sprites/naves`.

```sh
./gradlew --no-daemon :android:assembleDebug
APK=android/build/outputs/apk/debug/android-debug.apk
python3 validacao/android/conferir_api_minima.py "$APK" --sdk "$ANDROID_HOME" --min 26
adb reverse tcp:8080 tcp:8080
adb install -r "$APK"
adb shell monkey -p br.edu.unifor.missaomarte 1
```

Na entrada, opção 4, configure `http://127.0.0.1:8080`. Entre com conta, abra o Hangar em paisagem, confira a skin, toque na faixa da Horda e dispare a tempestade com o comando do CT-26. Registre o aviso e o efeito em voo. O verificador de API deve terminar com sucesso.

Para repetir o teste local dos três scripts, execute `python3 -I validacao/fase-08/teste_local.py -v`.
