package br.edu.unifor.missaomarte.dominio;

/** Obstáculo fixo. */
public class Asteroide implements Obstaculo {

    private final Posicao posicao;

    public Asteroide(Posicao posicao) {
        this.posicao = posicao;
    }

    @Override
    public Posicao getPosicao() {
        return posicao;
    }
}
