package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.lanca;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.teste.Teste;

class NaveTeste {

    private final Mapa mapa = new Mapa(1);

    @Teste
    void comecaNaPlataformaComTresVidas() {
        Nave nave = new Nave(5);
        igual(Posicao.ORIGEM, nave.getPosicao());
        igual(3, nave.getVidas());
        igual(0, nave.getPassageiros().size());
    }

    @Teste
    void moveUmaCelulaEmCadaDirecaoComYCrescendoParaBaixo() {
        Nave nave = new Nave(5);
        nave.mover(Direcao.CIMA, mapa);
        igual(new Posicao(0, -1), nave.getPosicao());
        nave.mover(Direcao.DIREITA, mapa);
        igual(new Posicao(1, -1), nave.getPosicao());
        nave.mover(Direcao.BAIXO, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        igual(Posicao.ORIGEM, nave.getPosicao());
    }

    @Teste
    void naBordaDoMapaANaveFicaParada() {
        Nave nave = new Nave(5);
        nave.mover(Direcao.ESQUERDA, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        nave.mover(Direcao.ESQUERDA, mapa);
        igual(new Posicao(-1, 0), nave.getPosicao());
    }

    @Teste
    void embarcaAteACapacidade() {
        Nave nave = new Nave(2);
        verdadeiro(nave.embarcar(new Professor("A", Posicao.ORIGEM)), "1º passageiro");
        verdadeiro(nave.embarcar(new Professor("B", Posicao.ORIGEM)), "2º passageiro");
        falso(nave.embarcar(new Professor("C", Posicao.ORIGEM)), "3º passageiro não cabe");
        igual(2, nave.getPassageiros().size());
    }

    @Teste
    void listaDePassageirosNaoPodeSerAlteradaPorFora() {
        Nave nave = new Nave(2);
        lanca(UnsupportedOperationException.class, () -> nave.getPassageiros().clear());
    }

    @Teste
    void vidasNaoFicamNegativas() {
        Nave nave = new Nave(5);
        nave.perderVida();
        nave.perderVida();
        falso(nave.estaDestruida(), "ainda tem 1 vida");
        nave.perderVida();
        nave.perderVida();
        igual(0, nave.getVidas());
        verdadeiro(nave.estaDestruida(), "sem vidas");
    }
}
