# Integração da História com a arte da main

Data: 27/11/2026. História em `4f97f38` (encontro + M01), main em `6ffe6aa` (arte/animações).

## Resolução

Sete arquivos de conflito conciliados. Mantidos os hooks da História e as animações de passageiros, portas e ambiente da main. Enum `Folha` e catálogo `SPECS` incluem as duas entregas. No manifesto, registros da main prevalecem nos recursos compartilhados; recursos novos da História foram adicionados.

`render_report.json` preserva o relatório original da arte. `render-m01.json` preserva o relatório anterior da M01, sem somar medições de execuções diferentes. `verification.json` foi regenerado por conferência real dos PNGs no Blender em segundo plano: 77 resultados, sem erros.

## Verificação

- `./gradlew --no-daemon test :android:assembleDebug :lwjgl3:distZip`: build e 570 testes do jogo passaram (`build.log`).
- Blender: `--background --python arte/blender/verify.py`, exit 0 (`sprites.log`).
- Revisão independente de catálogo, manifesto, dimensões e hooks: sem bloqueadores.
- Instrumentação da M01: APK construído com `-I validacao/historia/android/init.gradle`, isolado no pacote `br.edu.unifor.missaomarte.historia`.
- Celular físico encontrado como offline. Validação de execução realizada no AVD Small_Phone_API_36, sem janela; os resultados e capturas ficam em `emulador/`.

A validação cobre voo contínuo, pouso e embarque, retorno e entrega, persistência em novo processo e replay. Capturas são evidência para revisão visual do usuário; a aprovação visual humana não é presumida.

## Resultado da execução integrada

M01 concluída e salva; reabertura carregou conclusão e melhor pontuação; replay iniciado e abortado preservando ambas. Instrumentação: PASS. O voo registrou 3.284 quadros, 65 intervalos acima de 50 ms e máximo de 364 ms no emulador SwiftShader. Esse resultado não certifica fluidez no celular físico, que estava offline.

Servidor integrado: `cd servidor && ./gradlew --no-daemon build`, exit 0; 216 testes passaram sem falhas (`servidor.log`, `resumo-testes.txt`).
