package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.lanca;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.teste.Teste;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class MapaTeste {

    @Teste
    void vaiDeMenosTamanhoAMaisTamanho() {
        Mapa mapa = new Mapa(3);
        igual(-3, mapa.getMin());
        igual(3, mapa.getMax());
        verdadeiro(mapa.contem(new Posicao(-3, 3)), "canto");
        falso(mapa.contem(new Posicao(4, 0)), "fora");
        igual(Posicao.ORIGEM, mapa.getPlataforma());
    }

    @Teste
    void tamanhoPrecisaSerPositivo() {
        lanca(IllegalArgumentException.class, () -> new Mapa(0));
    }

    @Teste
    void sorteioCobreTodasAsCelulasSemSairDoMapa() {
        Mapa mapa = new Mapa(1);
        Random random = new Random(7);
        Set<Posicao> vistas = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            Posicao p = mapa.sortear(random);
            verdadeiro(mapa.contem(p), "sorteada fora: " + p);
            vistas.add(p);
        }
        igual(9, vistas.size());
    }
}
