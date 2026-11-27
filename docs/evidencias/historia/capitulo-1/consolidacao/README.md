# Consolidação no APK oficial — 27/11/2026

- Código de referência: commit 492fec0, Capítulo 1 completo.
- APK local compilado com `-Pmissaomarte.servidorPadrao=https://missao-marte.thebylito.dev`.
- Certificado público do APK novo coincide com o do app instalado; atualização `adb install -r` concluída sem desinstalar o pacote oficial.
- Pacote oficial: `br.edu.unifor.missaomarte`.
- O pacote oficial não tinha arquivo de campanha. Foram preservadas cópias locais dos dois arquivos de História da validação, verificadas pelo leitor real `HistoriaArquivo`.
- Principal e backup transferidos para o diretório privado da campanha no pacote oficial; conteúdo confirmado byte a byte. Nenhum arquivo de autenticação ou ranking foi lido ou alterado.
- Interface oficial aberta: capítulo completo com M01=34, M02=20, M03=40; captura anexa.
- Removidos do usuário principal os pacotes `br.edu.unifor.missaomarte.historia`, `br.edu.unifor.missaomarte.historia.test` e `br.edu.unifor.missaomarte.test`.
- Conferência final `pm list packages --user 0 missaomarte`: somente `package:br.edu.unifor.missaomarte`.

Esta é a instalação local validada. A criação do PR pela integração GitHub retornou 403 (`Resource not accessible by integration`); este registro não afirma publicação remota.
