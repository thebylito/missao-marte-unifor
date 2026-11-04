package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.teste.Teste;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class MovimentoAleatorioTeste {

    @Teste
    void inimigoNuncaSaiDoMapaEAndaNoMaximoUmaCelula() {
        Mapa mapa = new Mapa(2);
        MovimentoAleatorio movimento = new MovimentoAleatorio(new Random(3));
        Posicao atual = new Posicao(2, 2);
        for (int i = 0; i < 5000; i++) {
            Posicao proxima = movimento.proximaPosicao(atual, mapa);
            verdadeiro(mapa.contem(proxima), "saiu do mapa: " + proxima);
            int passo = Math.abs(proxima.x() - atual.x()) + Math.abs(proxima.y() - atual.y());
            verdadeiro(passo <= 1, "andou mais de uma célula");
            atual = proxima;
        }
    }

    @Teste
    void usaAsQuatroDirecoes() {
        Mapa mapa = new Mapa(5);
        MovimentoAleatorio movimento = new MovimentoAleatorio(new Random(11));
        Set<Posicao> destinos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            destinos.add(movimento.proximaPosicao(Posicao.ORIGEM, mapa));
        }
        igual(4, destinos.size());
    }

    @Teste
    void mesmaSementeMesmoCaminho() {
        Mapa mapa = new Mapa(3);
        MovimentoAleatorio a = new MovimentoAleatorio(new Random(42));
        MovimentoAleatorio b = new MovimentoAleatorio(new Random(42));
        Posicao pa = Posicao.ORIGEM;
        Posicao pb = Posicao.ORIGEM;
        for (int i = 0; i < 100; i++) {
            pa = a.proximaPosicao(pa, mapa);
            pb = b.proximaPosicao(pb, mapa);
            igual(pa, pb);
        }
    }
}
