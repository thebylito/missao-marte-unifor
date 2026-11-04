package br.edu.unifor.missaomarte.dominio;

import java.util.Random;

/** Área quadrada que vai de -tamanho a +tamanho nos dois eixos. */
public final class Mapa {

    private final int min;
    private final int max;

    public Mapa(int tamanho) {
        if (tamanho <= 0) {
            throw new IllegalArgumentException("O tamanho do mapa deve ser positivo: " + tamanho);
        }
        this.min = -tamanho;
        this.max = tamanho;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    public boolean contem(Posicao p) {
        return p.x() >= min && p.x() <= max && p.y() >= min && p.y() <= max;
    }

    /** Onde a nave precisa estar para concluir a missão. */
    public Posicao getPlataforma() {
        return Posicao.ORIGEM;
    }

    /** Sorteia uma célula qualquer: primeiro x, depois y. */
    public Posicao sortear(Random random) {
        int x = random.nextInt(max - min + 1) + min;
        int y = random.nextInt(max - min + 1) + min;
        return new Posicao(x, y);
    }
}
