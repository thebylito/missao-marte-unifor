package br.edu.unifor.missaomarte.grafico.apresentacao;

/**
 * Para onde a câmera da pilotagem olha (Fase 7). Java puro: segue a nave um pouco à frente (na direção em que ela
 * anda), com suavidade, e nunca mostra o lado de fora da caverna. Se a tela for maior que a caverna num sentido, a
 * caverna fica centralizada nele.
 */
public final class CameraDaPilotagem {

    /** Quanto à frente da nave a câmera olha, em segundos de movimento. */
    static final double ANTECIPACAO = 0.4;
    /** Quanto da distância até o alvo a câmera anda por segundo (suavidade). */
    static final double RAPIDEZ = 4;

    private final double larguraDaCaverna;
    private final double alturaDaCaverna;
    private double x;
    private double y;
    private boolean posicionada;

    public CameraDaPilotagem(double larguraDaCaverna, double alturaDaCaverna) {
        this.larguraDaCaverna = larguraDaCaverna;
        this.alturaDaCaverna = alturaDaCaverna;
    }

    /**
     * Anda em direção à nave e devolve o centro da câmera.
     *
     * @param larguraVisivel quanto da caverna cabe na tela, na horizontal (unidades da caverna)
     * @param segundos       tempo do quadro (0 para pular direto para o alvo)
     */
    public double[] seguir(double naveX, double naveY, double velocidadeX, double velocidadeY,
                           double larguraVisivel, double alturaVisivel, double segundos) {
        double alvoX = naveX + velocidadeX * ANTECIPACAO;
        double alvoY = naveY + velocidadeY * ANTECIPACAO;
        if (!posicionada || segundos <= 0) {
            x = alvoX;
            y = alvoY;
            posicionada = true;
        } else {
            double k = Math.min(1, segundos * RAPIDEZ);
            x += (alvoX - x) * k;
            y += (alvoY - y) * k;
        }
        x = limitar(x, larguraVisivel, larguraDaCaverna);
        y = limitar(y, alturaVisivel, alturaDaCaverna);
        return new double[] {x, y};
    }

    /** Dentro da caverna; se a tela for maior que ela, no meio. */
    static double limitar(double centro, double visivel, double tamanho) {
        if (visivel >= tamanho) {
            return tamanho / 2;
        }
        return Math.max(visivel / 2, Math.min(tamanho - visivel / 2, centro));
    }
}
