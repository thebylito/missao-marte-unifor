package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PassageiroTeste {

    @Test
    void cadaTipoValeSuaPontuacao() {
        Posicao p = new Posicao(1, 1);
        assertEquals(10, new Professor("x", p).getPontuacao());
        assertEquals(15, new Engenheiro("x", p).getPontuacao());
        assertEquals(20, new Astronauta("x", p).getPontuacao());
    }

    @Test
    void cadaSubclasseInformaSeuTipo() {
        Posicao p = new Posicao(1, 1);
        assertEquals(TipoPassageiro.PROFESSOR, new Professor("x", p).getTipo());
        assertEquals(TipoPassageiro.ENGENHEIRO, new Engenheiro("x", p).getTipo());
        assertEquals(TipoPassageiro.ASTRONAUTA, new Astronauta("x", p).getTipo());
        assertEquals("Engenheiro", TipoPassageiro.ENGENHEIRO.getNome());
    }
}
