package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.List;
import java.util.SplittableRandom;

/** Uma ocorrência de perigo, com duração e sobrevivência independentes das demais. */
final class PerigoAtivo {

    private final EventoAplicado inicio;
    private final long passoFinal;
    private final double fase;
    private final ChuvaDeMeteoros chuva;
    boolean sobreviveu;

    PerigoAtivo(EventoAplicado inicio, boolean naveInteira, Caverna caverna) {
        this.inicio = inicio;
        passoFinal = inicio.passo() + Math.round(tipo().duracaoSegundos() / PartidaDePilotagem.PASSO);
        fase = tipo() == TipoDeEvento.TEMPESTADE
                ? new SplittableRandom(inicio.semente()).nextDouble(2 * Math.PI) : 0;
        chuva = tipo() == TipoDeEvento.CHUVA_DE_METEOROS ? new ChuvaDeMeteoros(caverna, inicio.semente()) : null;
        sobreviveu = naveInteira;
    }

    TipoDeEvento tipo() {
        return inicio.tipo();
    }

    boolean terminou(long passo) {
        return passo >= passoFinal;
    }

    private double idade(long passo) {
        return (passo - inicio.passo()) * PartidaDePilotagem.PASSO;
    }

    double rajada(long passo) {
        return 30 * Math.sin(2 * Math.PI * idade(passo) / 5 + fase);
    }

    double intensidade(long passo) {
        double envelope = Math.sin(Math.PI * idade(passo) / tipo().duracaoSegundos());
        return envelope * envelope;
    }

    void posicionarMeteoros(long passo, List<Meteoro> meteoros) {
        chuva.posicionar(idade(passo), meteoros);
    }
}
