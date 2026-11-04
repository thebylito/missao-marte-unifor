# Fase 1 — compila, testa e (opcionalmente) executa o jogo só com o JDK (sem Gradle, que chega na Fase 2).
#   .\build.ps1          compila e roda os testes
#   .\build.ps1 -Run     compila, roda os testes e inicia o jogo
# Usa o JDK de JAVA_HOME, se definido; senão, o java/javac do PATH. Requer JDK 17+.
param([switch]$Run)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$jbin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { '' }
$javac = if ($jbin) { Join-Path $jbin 'javac' } else { 'javac' }
$java = if ($jbin) { Join-Path $jbin 'java' } else { 'java' }

if (Test-Path build) { Remove-Item -Recurse -Force build }
$fontes = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
$testes = Get-ChildItem -Recurse test -Filter *.java | ForEach-Object FullName

& $javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes $fontes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes $testes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp "build/classes;build/test-classes" br.edu.unifor.missaomarte.teste.ExecutarTestes build/test-classes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Run) {
    & $java -cp build/classes br.edu.unifor.missaomarte.Main
}
