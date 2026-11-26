package br.edu.unifor.missaomarte.servidor.evento;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.time.Duration;
import java.time.Instant;

public record EventoAoVivo(String id, TipoDeEvento tipo, long semente, Instant inicio, Instant fim,
                           String vencedor, String sementeVip) {

    public static final int AVISO_SEGUNDOS = 15;
    public static final int FOLGA_RESULTADO_SEGUNDOS = 15;
    public static final int ESPERA_VIP_SEGUNDOS = 3;

    public static EventoAoVivo novo(long horda, long indice, TipoDeEvento tipo, long semente, Instant inicio) {
        return novo(horda, indice, tipo, semente, inicio, null);
    }

    public static EventoAoVivo novo(long horda, long indice, TipoDeEvento tipo, long semente, Instant inicio,
                                    String sementeVip) {
        return new EventoAoVivo("e-" + horda + "-" + indice, tipo, semente, inicio,
                inicio.plusSeconds((long) tipo.duracaoSegundos()), null, sementeVip);
    }

    public Instant aviso() {
        return inicio.minusSeconds(AVISO_SEGUNDOS);
    }

    public boolean podePegarVip(Instant agora) {
        return tipo == TipoDeEvento.PASSAGEIRO_VIP && !agora.isBefore(inicio.plusSeconds(ESPERA_VIP_SEGUNDOS))
                && agora.isBefore(fim);
    }

    public boolean podeReceberResultado(Instant agora) {
        return tipo.perigo() && !agora.isBefore(fim) && agora.isBefore(fim.plusSeconds(FOLGA_RESULTADO_SEGUNDOS));
    }

    public long duracao() {
        return Duration.between(inicio, fim).getSeconds();
    }
}
