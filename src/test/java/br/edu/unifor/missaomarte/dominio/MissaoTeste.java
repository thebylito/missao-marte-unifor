package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MissaoTeste {

    private final Missao missao = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);

    @Test
    void sabeQuaisCelulasEstaoOcupadas() {
        missao.adicionarPassageiro(new Professor("P", new Posicao(1, 0)));
        missao.adicionarAsteroide(new Asteroide(new Posicao(0, 1)));
        missao.adicionarInimigo(new Inimigo(new Posicao(-1, 0)));
        assertTrue(missao.ocupada(Posicao.ORIGEM), "nave");
        assertTrue(missao.ocupada(new Posicao(1, 0)), "passageiro");
        assertTrue(missao.ocupada(new Posicao(0, 1)), "asteroide");
        assertTrue(missao.ocupada(new Posicao(-1, 0)), "inimigo");
        assertFalse(missao.ocupada(new Posicao(2, 2)), "vazia");
    }

    @Test
    void encontraOPrimeiroPassageiroNaCelulaDaNave() {
        Professor primeiro = new Professor("Primeiro", Posicao.ORIGEM);
        missao.adicionarPassageiro(new Engenheiro("Longe", new Posicao(2, 2)));
        missao.adicionarPassageiro(primeiro);
        missao.adicionarPassageiro(new Astronauta("Segundo", Posicao.ORIGEM));
        assertEquals(primeiro, missao.passageiroNaPosicaoDaNave().orElseThrow());
    }

    @Test
    void embarcarTiraOPassageiroDoMapa() {
        Professor p = new Professor("P", Posicao.ORIGEM);
        missao.adicionarPassageiro(p);
        assertFalse(missao.todosEmbarcados(), "antes");
        assertTrue(missao.embarcar(p), "embarcou");
        assertTrue(missao.todosEmbarcados(), "depois");
        assertEquals(1, missao.getNave().getPassageiros().size());
    }

    @Test
    void naveCheiaNaoTiraOPassageiroDoMapa() {
        Missao cheia = new Missao(new Mapa(2), new Nave(0), MovimentoInimigo.PARADO);
        Professor p = new Professor("P", Posicao.ORIGEM);
        cheia.adicionarPassageiro(p);
        assertFalse(cheia.embarcar(p), "sem lugar");
        assertEquals(1, cheia.getPassageiros().size());
    }

    @Test
    void colisaoComAsteroideOuInimigo() {
        assertFalse(missao.naveColidiu(), "mapa vazio");
        missao.adicionarAsteroide(new Asteroide(Posicao.ORIGEM));
        assertTrue(missao.naveColidiu(), "asteroide");

        Missao comInimigo = new Missao(new Mapa(2), new Nave(5), (atual, mapa) -> Posicao.ORIGEM);
        comInimigo.adicionarInimigo(new Inimigo(new Posicao(1, 1)));
        assertFalse(comInimigo.naveColidiu(), "inimigo longe");
        comInimigo.moverInimigos();
        assertTrue(comInimigo.naveColidiu(), "inimigo veio até a nave");
    }

    @Test
    void plataformaEhAOrigem() {
        assertTrue(missao.naveNaPlataforma(), "começa na plataforma");
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        assertFalse(missao.naveNaPlataforma(), "saiu");
    }
}
