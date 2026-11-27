# CI somente após entrada na main — 27/11/2026

Base: `origin/main` em `6ffe6aa`. Alteração isolada na branch `ci-somente-main`.

## Comportamento

- `ci.yml`: somente `workflow_call`; sem gatilho de PR e sem build duplicado da imagem.
- `deploy.yml`: mantém push na main filtrado pelos caminhos do projeto e acionamento manual; teste inicial exige `refs/heads/main`.
- `needs` preserva testes/pacotes → imagem → deploy. Concorrência por ref impede que uma execução manual fora da main cancele a publicação correta.
- Não foi iniciado workflow remoto, merge de PR nem deploy.

## Verificações locais

- `actionlint .github/workflows/ci.yml .github/workflows/deploy.yml`: exit 0, ver `actionlint.log`.
- `ruby docs/evidencias/ci-somente-main/validar-gatilhos.rb`: conferência estrutural dos eventos, do encadeamento e da concorrência, ver `gatilhos.log`.
- `./gradlew --no-daemon test :android:assembleDebug :lwjgl3:distZip`: build e 535 testes do jogo passaram, ver `build.log` e `resumo-testes.txt`.
- `cd servidor && ./gradlew --no-daemon build`: build e testes do servidor passaram, ver `servidor.log` e `resumo-testes.txt`. A suíte executa integrações locais, incluindo HTTP e banco temporário; não usa produção.
- Revisão independente: encontrou colisão de concorrência entre branches, corrigida; sem bloqueadores restantes.

Java 17, SDK Android local. Pacotes Windows/macOS e execução do workflow no GitHub não foram repetidos nesta máquina. A validação dos gatilhos é estrutural; o primeiro processamento remoto ocorrerá após a integração autorizada na main.
