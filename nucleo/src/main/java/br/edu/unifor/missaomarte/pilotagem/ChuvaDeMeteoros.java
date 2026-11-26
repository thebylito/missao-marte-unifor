package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Trajetórias retilíneas sorteadas uma vez, calculadas a partir da idade da chuva. */
final class ChuvaDeMeteoros {

    private final List<Trajetoria> trajetorias = new ArrayList<>();

    ChuvaDeMeteoros(Caverna caverna, long semente) {
        SplittableRandom sorteio = new SplittableRandom(semente);
        int quantidade = sorteio.nextInt(6, 11);
        double largura = caverna.getLargura();
        double altura = caverna.getAltura();
        double duracaoDaChuva = TipoDeEvento.CHUVA_DE_METEOROS.duracaoSegundos();
        for (int i = 0; i < quantidade; i++) {
            int borda = sorteio.nextInt(3);
            double x, y, destinoX, destinoY;
            if (borda == 0) {
                x = sorteio.nextDouble(largura);
                y = altura;
                destinoX = sorteio.nextDouble(largura);
                destinoY = 0;
            } else {
                x = borda == 1 ? 0 : largura;
                y = sorteio.nextDouble(altura);
                destinoX = largura - x;
                destinoY = sorteio.nextDouble(altura);
            }
            double dx = destinoX - x;
            double dy = destinoY - y;
            double tempoDeTravessia = Math.hypot(dx, dy) / sorteio.nextDouble(300, 500);
            double raio = sorteio.nextDouble(18, 28);
            // Espalha as entradas pela duração da chuva. Antecipa as longas quando ainda dá para cruzar.
            double entrada = (i + sorteio.nextDouble()) * duracaoDaChuva / quantidade;
            double ultimoPasso = duracaoDaChuva - PartidaDePilotagem.PASSO;
            double limiteDaEntrada = tempoDeTravessia < duracaoDaChuva ? duracaoDaChuva - tempoDeTravessia : ultimoPasso;
            entrada = Math.min(entrada, Math.min(limiteDaEntrada, ultimoPasso));
            trajetorias.add(new Trajetoria(entrada, tempoDeTravessia, x, y, dx / tempoDeTravessia,
                    dy / tempoDeTravessia, raio));
        }
    }

    void posicionar(double idade, List<Meteoro> meteoros) {
        for (Trajetoria trajetoria : trajetorias) {
            double tempoEmVoo = idade - trajetoria.entrada();
            if (tempoEmVoo >= 0 && tempoEmVoo <= trajetoria.duracao()) {
                meteoros.add(new Meteoro(trajetoria.x() + trajetoria.vx() * tempoEmVoo,
                        trajetoria.y() + trajetoria.vy() * tempoEmVoo, trajetoria.raio()));
            }
        }
    }

    private record Trajetoria(double entrada, double duracao, double x, double y, double vx, double vy, double raio) {}
}
