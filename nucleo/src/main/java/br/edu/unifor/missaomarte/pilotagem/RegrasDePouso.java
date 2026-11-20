package br.edu.unifor.missaomarte.pilotagem;

import java.util.Optional;

/**
 * Quando um pouso é seguro. Função pura (só números), para os limites serem testados com exatidão. Os três limites
 * são inclusivos: cair a exatamente 75 ainda é seguro; a 75,5 já não é.
 */
public final class RegrasDePouso {

    private RegrasDePouso() {
    }

    /** O que deu errado no pouso, ou vazio se ele foi seguro. */
    public static Optional<String> problema(ConfigDeVoo config, double velocidadeX, double velocidadeY,
                                            double angulo, boolean centroSobreAPlataforma) {
        if (!inclinacaoSegura(config, angulo)) {
            return Optional.of("Pouso torto! (" + Math.round(Math.abs(angulo)) + " graus)");
        }
        if (!quedaSegura(config, velocidadeY)) {
            return Optional.of("Pouso duro! (" + Math.round(-velocidadeY) + " de queda)");
        }
        if (!deslizeSeguro(config, velocidadeX)) {
            return Optional.of("Pouso derrapando! (" + Math.round(Math.abs(velocidadeX)) + " de lado)");
        }
        if (!centroSobreAPlataforma) {
            return Optional.of("Pouso na beirada! A nave tombou.");
        }
        return Optional.empty();
    }

    public static boolean quedaSegura(ConfigDeVoo config, double velocidadeY) {
        return -velocidadeY <= config.quedaMaxima();
    }

    public static boolean deslizeSeguro(ConfigDeVoo config, double velocidadeX) {
        return Math.abs(velocidadeX) <= config.deslizeMaximo();
    }

    public static boolean inclinacaoSegura(ConfigDeVoo config, double angulo) {
        return Math.abs(angulo) <= config.inclinacaoMaxima();
    }
}
