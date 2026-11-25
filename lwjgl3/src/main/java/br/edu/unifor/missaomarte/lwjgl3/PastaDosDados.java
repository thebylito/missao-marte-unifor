package br.edu.unifor.missaomarte.lwjgl3;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Onde o jogo desktop grava o ranking local ({@code ranking.json} e {@code ranking-pilotagem.json}), pela propriedade
 * {@code -Dmissaomarte.dados}:
 * <ul>
 *   <li>sem ela: a pasta de trabalho, como sempre (o {@code ./gradlew :lwjgl3:run} e o script do zip usam a mesma
 *   pasta do jogo de console);</li>
 *   <li>{@code usuario}: {@code ~/.missao-marte}. É o que o pacote nativo (Windows e macOS) passa: aberto com dois
 *   cliques, a pasta de trabalho pode ser uma pasta do sistema (no macOS, a raiz "/"), onde não dá para gravar;</li>
 *   <li>qualquer outro valor: esse caminho.</li>
 * </ul>
 */
final class PastaDosDados {

    static final String PROPRIEDADE = "missaomarte.dados";

    private PastaDosDados() {
    }

    static Path escolher(String valor, String home) {
        if (valor == null || valor.isBlank()) {
            return Paths.get("");
        }
        if (valor.trim().equals("usuario")) {
            return Paths.get(home, ".missao-marte");
        }
        return Paths.get(valor.trim());
    }
}
