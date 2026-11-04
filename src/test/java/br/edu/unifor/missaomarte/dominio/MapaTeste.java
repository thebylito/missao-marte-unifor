package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class MapaTeste {

    @Test
    void vaiDeMenosTamanhoAMaisTamanho() {
        Mapa mapa = new Mapa(3);
        assertEquals(-3, mapa.getMin());
        assertEquals(3, mapa.getMax());
        assertTrue(mapa.contem(new Posicao(-3, 3)), "canto");
        assertFalse(mapa.contem(new Posicao(4, 0)), "fora");
        assertEquals(Posicao.ORIGEM, mapa.getPlataforma());
    }

    @Test
    void tamanhoPrecisaSerPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new Mapa(0));
    }

    @Test
    void sorteioCobreTodasAsCelulasSemSairDoMapa() {
        Mapa mapa = new Mapa(1);
        Random random = new Random(7);
        Set<Posicao> vistas = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            Posicao p = mapa.sortear(random);
            assertTrue(mapa.contem(p), "sorteada fora: " + p);
            vistas.add(p);
        }
        assertEquals(9, vistas.size());
    }
}
