package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NaveTeste {

    private final Mapa mapa = new Mapa(1);

    @Test
    void comecaNaPlataformaComTresVidas() {
        Nave nave = new Nave(5);
        assertEquals(Posicao.ORIGEM, nave.getPosicao());
        assertEquals(3, nave.getVidas());
        assertEquals(0, nave.getPassageiros().size());
    }

    @Test
    void moveUmaCelulaEmCadaDirecaoComYCrescendoParaBaixo() {
        Nave nave = new Nave(5);
        nave.mover(Direcao.CIMA, mapa);
        assertEquals(new Posicao(0, -1), nave.getPosicao());
        nave.mover(Direcao.DIREITA, mapa);
        assertEquals(new Posicao(1, -1), nave.getPosicao());
        nave.mover(Direcao.BAIXO, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        assertEquals(Posicao.ORIGEM, nave.getPosicao());
    }

    @Test
    void naBordaDoMapaANaveFicaParada() {
        Nave nave = new Nave(5);
        nave.mover(Direcao.ESQUERDA, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        assertEquals(new Posicao(-1, 0), nave.getPosicao());
    }

    @Test
    void embarcaAteACapacidade() {
        Nave nave = new Nave(2);
        assertTrue(nave.embarcar(new Professor("A", Posicao.ORIGEM)), "1º passageiro");
        assertTrue(nave.embarcar(new Professor("B", Posicao.ORIGEM)), "2º passageiro");
        assertFalse(nave.embarcar(new Professor("C", Posicao.ORIGEM)), "3º passageiro não cabe");
        assertEquals(2, nave.getPassageiros().size());
    }

    @Test
    void listaDePassageirosNaoPodeSerAlteradaPorFora() {
        Nave nave = new Nave(2);
        assertThrows(UnsupportedOperationException.class, () -> nave.getPassageiros().clear());
    }

    @Test
    void vidasNaoFicamNegativas() {
        Nave nave = new Nave(5);
        nave.perderVida();
        nave.perderVida();
        assertFalse(nave.estaDestruida(), "ainda tem 1 vida");
        nave.perderVida();
        nave.perderVida();
        assertEquals(0, nave.getVidas());
        assertTrue(nave.estaDestruida(), "sem vidas");
    }
}
