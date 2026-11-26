package br.edu.unifor.missaomarte.servidor.evento;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

public final class AgendaDeEventos {

    private static final int PRIMEIRO_SEGUNDO = 5 * 60;
    private static final int INTERVALO_MINIMO = 20 * 60;

    private AgendaDeEventos() {}

    public static List<EventoAoVivo> daHorda(long numero, Instant inicio, long semente) {
        var sorteio = new SplittableRandom(semente);
        var tipos = TipoDeEvento.values();
        int quantidade = sorteio.nextInt(2, 4);
        int duracaoMaxima = Arrays.stream(tipos).mapToInt(tipo -> (int) tipo.duracaoSegundos()).max().orElseThrow();
        int ultimoSegundo = (int) ServicoDeHordas.DURACAO.toSeconds() - duracaoMaxima;
        // Retira os espaçamentos antes de sortear e os devolve em ordem.
        int limite = ultimoSegundo - (quantidade - 1) * INTERVALO_MINIMO;
        int[] segundos = sorteio.ints(PRIMEIRO_SEGUNDO, limite + 1).distinct().limit(quantidade).sorted().toArray();
        var eventos = new ArrayList<EventoAoVivo>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            Instant hora = inicio.plusSeconds(segundos[i] + (long) i * INTERVALO_MINIMO);
            eventos.add(EventoAoVivo.novo(numero, i + 1, tipos[sorteio.nextInt(tipos.length)], sorteio.nextLong(), hora));
        }
        return List.copyOf(eventos);
    }
}
