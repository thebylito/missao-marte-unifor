#!/usr/bin/env sh
# Fase 1 — compila, testa e (opcionalmente) executa o jogo só com o JDK (sem Gradle, que chega na Fase 2).
#   ./build.sh          compila e roda os testes
#   ./build.sh run      compila, roda os testes e inicia o jogo
# Usa o JDK de JAVA_HOME, se definido; senão, o java/javac do PATH. Requer JDK 17+.
set -e
cd "$(dirname "$0")"
JBIN="${JAVA_HOME:+$JAVA_HOME/bin/}"

rm -rf build
"${JBIN}javac" --release 17 -encoding UTF-8 -Xlint:all -d build/classes $(find src -name '*.java')
"${JBIN}javac" --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes $(find test -name '*.java')
"${JBIN}java" -cp build/classes:build/test-classes br.edu.unifor.missaomarte.teste.ExecutarTestes build/test-classes

if [ "$1" = "run" ]; then
    "${JBIN}java" -cp build/classes br.edu.unifor.missaomarte.Main
fi
