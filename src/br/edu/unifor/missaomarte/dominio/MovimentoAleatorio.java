package br.edu.unifor.missaomarte.dominio;

import java.util.Random;

/** Comportamento original: sorteia uma das quatro direções; se cair fora do mapa, fica parado. */
public final class MovimentoAleatorio implements MovimentoInimigo {

    /** Ordem do sorteio da versão original: 0 = direita, 1 = esquerda, 2 = baixo, 3 = cima. */
    private static final Direcao[] SORTEIO = {Direcao.DIREITA, Direcao.ESQUERDA, Direcao.BAIXO, Direcao.CIMA};

    private final Random random;

    public MovimentoAleatorio(Random random) {
        this.random = random;
    }

    @Override
    public Posicao proximaPosicao(Posicao atual, Mapa mapa) {
        Posicao destino = SORTEIO[random.nextInt(SORTEIO.length)].aplicar(atual);
        return mapa.contem(destino) ? destino : atual;
    }
}
