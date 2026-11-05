package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DificuldadeTeste {

    @Test
    void cadaDificuldadeTemSeusNumeros() {
        assertEquals(30, Dificuldade.FACIL.getPontosIniciais());
        assertEquals(20, Dificuldade.MEDIO.getPontosIniciais());
        assertEquals(45, Dificuldade.DIFICIL.getPontosIniciais());
        assertEquals(4, Dificuldade.FACIL.getPassageiros());
        assertEquals(1, Dificuldade.FACIL.getAsteroides());
        assertEquals(1, Dificuldade.FACIL.getInimigos());
        assertEquals(5, Dificuldade.MEDIO.getPassageiros());
        assertEquals(2, Dificuldade.MEDIO.getAsteroides());
        assertEquals(2, Dificuldade.MEDIO.getInimigos());
        assertEquals(5, Dificuldade.DIFICIL.getPassageiros());
        assertEquals(3, Dificuldade.DIFICIL.getAsteroides());
        assertEquals(3, Dificuldade.DIFICIL.getInimigos());
    }

    @Test
    void textoDigitadoViraDificuldadeComMedioComoPadrao() {
        assertEquals(Dificuldade.FACIL, Dificuldade.deString("facil"));
        assertEquals(Dificuldade.FACIL, Dificuldade.deString(" FÁCIL "));
        assertEquals(Dificuldade.DIFICIL, Dificuldade.deString("dificil"));
        assertEquals(Dificuldade.DIFICIL, Dificuldade.deString("Difícil"));
        assertEquals(Dificuldade.MEDIO, Dificuldade.deString("medio"));
        assertEquals(Dificuldade.MEDIO, Dificuldade.deString("qualquer coisa"));
        assertEquals(Dificuldade.MEDIO, Dificuldade.deString(""));
        assertEquals(Dificuldade.MEDIO, Dificuldade.deString(null));
    }

    @Test
    void nomeExibidoTemAcento() {
        assertEquals("Fácil", Dificuldade.FACIL.toString());
        assertEquals("Médio", Dificuldade.MEDIO.toString());
        assertEquals("Difícil", Dificuldade.DIFICIL.toString());
    }
}
