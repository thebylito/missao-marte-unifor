package br.edu.unifor.missaomarte.aplicacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.dominio.Direcao;
import org.junit.jupiter.api.Test;

class ComandoTeste {

    @Test
    void teclasDoConsole() {
        assertEquals(Comando.CIMA, Comando.daTecla('w'));
        assertEquals(Comando.BAIXO, Comando.daTecla('s'));
        assertEquals(Comando.ESQUERDA, Comando.daTecla('a'));
        assertEquals(Comando.DIREITA, Comando.daTecla('d'));
        assertEquals(Comando.EMBARCAR, Comando.daTecla('c'));
        assertEquals(Comando.ABORTAR, Comando.daTecla('q'));
        assertEquals(Comando.INVALIDO, Comando.daTecla('x'));
        assertEquals(Comando.INVALIDO, Comando.daTecla('W'));
    }

    @Test
    void soMovimentosTemDirecao() {
        assertEquals(Direcao.CIMA, Comando.CIMA.getDirecao().orElseThrow());
        assertFalse(Comando.EMBARCAR.getDirecao().isPresent(), "embarcar não tem direção");
    }
}
