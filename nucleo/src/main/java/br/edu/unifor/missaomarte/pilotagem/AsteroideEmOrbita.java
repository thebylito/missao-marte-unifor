package br.edu.unifor.missaomarte.pilotagem;

/**
 * Asteroide que oscila em torno de um ponto. A posição é uma função do tempo (seno), então o movimento é
 * determinístico e fácil de testar, sem sorteio.
 */
public final class AsteroideEmOrbita {

    private final double centroX;
    private final double centroY;
    private final double raio;
    private final double amplitudeX;
    private final double amplitudeY;
    private final double periodo;
    private final double fase;
    private final double giroGrausPorSegundo;
    private double x;
    private double y;
    private double rotacao;

    /**
     * @param periodo segundos de uma ida e volta (na velocidade normal)
     * @param fase    em radianos: asteroides com fases diferentes não andam juntos
     */
    public AsteroideEmOrbita(double centroX, double centroY, double raio, double amplitudeX, double amplitudeY,
                             double periodo, double fase, double giroGrausPorSegundo) {
        this.centroX = centroX;
        this.centroY = centroY;
        this.raio = raio;
        this.amplitudeX = amplitudeX;
        this.amplitudeY = amplitudeY;
        this.periodo = periodo;
        this.fase = fase;
        this.giroGrausPorSegundo = giroGrausPorSegundo;
        posicionar(0);
    }

    /** Onde o asteroide está no instante dado (o tempo já multiplicado pela velocidade da dificuldade). */
    void posicionar(double tempo) {
        double angulo = 2 * Math.PI * tempo / periodo + fase;
        x = centroX + amplitudeX * Math.sin(angulo);
        y = centroY + amplitudeY * Math.sin(angulo);
        double r = (tempo * giroGrausPorSegundo) % 360;
        rotacao = r < 0 ? r + 360 : r;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRaio() { return raio; }
    /** Rotação visual, em graus, de 0 a 360. */
    public double getRotacao() { return rotacao; }
    public double getPeriodo() { return periodo; }
}
