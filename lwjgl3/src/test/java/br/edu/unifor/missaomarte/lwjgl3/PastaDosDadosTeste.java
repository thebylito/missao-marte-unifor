package br.edu.unifor.missaomarte.lwjgl3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Onde o jogo desktop grava o ranking local. O pacote nativo (Windows e macOS) é aberto com dois cliques, e a pasta
 * de trabalho pode ser a do sistema (no macOS, a raiz "/"), onde não dá para gravar: lá os dados vão para a pasta do
 * usuário.
 */
class PastaDosDadosTeste {

    private static final String HOME = Paths.get("/home/ana").toString();

    @Test
    void semAPropriedadeContinuaNaPastaDeTrabalhoComoSempre() {
        assertEquals(Paths.get(""), PastaDosDados.escolher(null, HOME));
        assertEquals(Paths.get(""), PastaDosDados.escolher("  ", HOME));
    }

    @Test
    void noPacoteNativoUsaAPastaDoUsuario() {
        assertEquals(Paths.get(HOME, ".missao-marte"), PastaDosDados.escolher("usuario", HOME));
    }

    @Test
    void umCaminhoExplicitoTambemServe() {
        Path outra = Paths.get("/tmp/missao");
        assertEquals(outra, PastaDosDados.escolher(outra.toString(), HOME));
    }
}
