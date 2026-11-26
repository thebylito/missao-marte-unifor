package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Um asteroide da caverna, como dado: os mesmos parâmetros da
 * {@link br.edu.unifor.missaomarte.pilotagem.AsteroideEmOrbita}. Na horda, a {@code fase} daqui é trocada pela
 * fase sorteada.
 */
public record AsteroideDescrito(double centroX, double centroY, double raio, double amplitudeX, double amplitudeY,
                                double periodo, double fase, double giroGrausPorSegundo) {}
