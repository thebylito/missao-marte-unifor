package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class MovimentoAleatorioTeste {

    @Test
    void inimigoNuncaSaiDoMapaEAndaNoMaximoUmaCelula() {
        Mapa mapa = new Mapa(2);
        MovimentoAleatorio movimento = new MovimentoAleatorio(new Random(3));
        Posicao atual = new Posicao(2, 2);
        for (int i = 0; i < 5000; i++) {
            Posicao proxima = movimento.proximaPosicao(atual, mapa);
            assertTrue(mapa.contem(proxima), "saiu do mapa: " + proxima);
            int passo = Math.abs(proxima.x() - atual.x()) + Math.abs(proxima.y() - atual.y());
            assertTrue(passo <= 1, "andou mais de uma célula");
            atual = proxima;
        }
    }

    @Test
    void usaAsQuatroDirecoes() {
        Mapa mapa = new Mapa(5);
        MovimentoAleatorio movimento = new MovimentoAleatorio(new Random(11));
        Set<Posicao> destinos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            destinos.add(movimento.proximaPosicao(Posicao.ORIGEM, mapa));
        }
        assertEquals(4, destinos.size());
    }

    @Test
    void mesmaSementeMesmoCaminho() {
        Mapa mapa = new Mapa(3);
        MovimentoAleatorio a = new MovimentoAleatorio(new Random(42));
        MovimentoAleatorio b = new MovimentoAleatorio(new Random(42));
        Posicao pa = Posicao.ORIGEM;
        Posicao pb = Posicao.ORIGEM;
        for (int i = 0; i < 100; i++) {
            pa = a.proximaPosicao(pa, mapa);
            pb = b.proximaPosicao(pb, mapa);
            assertEquals(pa, pb);
        }
    }
}
