package br.edu.unifor.missaomarte.dominio;

/** Obstáculo que se move a cada turno, segundo uma estratégia de movimento. */
public class Inimigo implements Obstaculo {

    private Posicao posicao;

    public Inimigo(Posicao posicao) {
        this.posicao = posicao;
    }

    @Override
    public Posicao getPosicao() {
        return posicao;
    }

    public void mover(MovimentoInimigo movimento, Mapa mapa) {
        posicao = movimento.proximaPosicao(posicao, mapa);
    }
}
