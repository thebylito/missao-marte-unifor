package br.edu.unifor.missaomarte.servidor.evento;

import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.seguranca.Sementes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

public final class AgendaDeEventos {

    private static final int PRIMEIRO_SEGUNDO = 5 * 60;
    private static final int INTERVALO_MINIMO = 20 * 60;

    private AgendaDeEventos() {}

    /** Os eventos duram o que os ajustes gravados na horda dizem (DA-48); sem ajustes, o padrão. */
    public static List<EventoAoVivo> daHorda(Horda horda) {
        AjustesDosEventos duracoes = horda.descricao().ajustes().eventos();
        if (horda.sementeAgenda() == null) {
            return sortear(horda.numero(), horda.inicio(), horda.semente(), null, duracoes);
        }
        byte[] chave = Sementes.deHex(horda.sementeAgenda());
        long horarios = Sementes.comoLong(Sementes.derivar(chave, "horarios"));
        return sortear(horda.numero(), horda.inicio(), horarios, chave, duracoes);
    }

    /** Mantém os eventos das hordas publicadas antes da V5. */
    public static List<EventoAoVivo> daHorda(long numero, Instant inicio, long semente) {
        return sortear(numero, inicio, semente, null, AjustesDosEventos.PADRAO);
    }

    private static List<EventoAoVivo> sortear(long numero, Instant inicio, long semente, byte[] chave,
                                              AjustesDosEventos duracoes) {
        var sorteio = new SplittableRandom(semente);
        var tipos = TipoDeEvento.values();
        int quantidade = sorteio.nextInt(2, 4);
        // Com o padrão, o mesmo limite de antes (45 s): as hordas já gravadas mantêm os horários.
        int duracaoMaxima = Arrays.stream(tipos).mapToInt(tipo -> (int) duracoes.duracao(tipo)).max().orElseThrow();
        int ultimoSegundo = (int) ServicoDeHordas.DURACAO.toSeconds() - duracaoMaxima;
        // Retira os espaçamentos antes de sortear e os devolve em ordem.
        int limite = ultimoSegundo - (quantidade - 1) * INTERVALO_MINIMO;
        int[] segundos = sorteio.ints(PRIMEIRO_SEGUNDO, limite + 1).distinct().limit(quantidade).sorted().toArray();
        var eventos = new ArrayList<EventoAoVivo>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            Instant hora = inicio.plusSeconds(segundos[i] + (long) i * INTERVALO_MINIMO);
            int indice = i + 1;
            TipoDeEvento tipo = tipos[sorteio.nextInt(tipos.length)];
            if (chave == null) {
                eventos.add(EventoAoVivo.novo(numero, indice, tipo, sorteio.nextLong(), hora, null,
                        (long) duracoes.duracao(tipo)));
            } else {
                long publica = Sementes.comoLong(Sementes.derivar(chave, "evento:" + indice));
                String vip = tipo == TipoDeEvento.PASSAGEIRO_VIP
                        ? Sementes.emHex(Sementes.derivar(chave, "vip:" + indice)) : null;
                eventos.add(EventoAoVivo.novo(numero, indice, tipo, publica, hora, vip,
                        (long) duracoes.duracao(tipo)));
            }
        }
        return List.copyOf(eventos);
    }
}
