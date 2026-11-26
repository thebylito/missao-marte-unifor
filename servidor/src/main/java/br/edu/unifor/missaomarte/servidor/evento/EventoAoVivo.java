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
        return novo(horda, indice, tipo, semente, inicio, sementeVip, (long) tipo.duracaoSegundos());
    }

    /** Fase 8 (DA-48): com a duração dos ajustes gravados na horda, em segundos inteiros. */
    public static EventoAoVivo novo(long horda, long indice, TipoDeEvento tipo, long semente, Instant inicio,
                                    String sementeVip, long duracaoSegundos) {
        return new EventoAoVivo("e-" + horda + "-" + indice, tipo, semente, inicio,
                inicio.plusSeconds(duracaoSegundos), null, sementeVip);
    }

    public Instant aviso() {
        return inicio.minusSeconds(AVISO_SEGUNDOS);
    }

    public boolean podePegarVip(Instant agora) {
        return tipo == TipoDeEvento.PASSAGEIRO_VIP && !agora.isBefore(inicio.plusSeconds(ESPERA_VIP_SEGUNDOS))
                && agora.isBefore(fim);
    }

    /**
     * O resultado vale do fim até {@link #FOLGA_RESULTADO_SEGUNDOS} depois. Num evento mais longo que o padrão
     * (DA-48), vale já a partir da duração padrão: o jogo publicado antes dos ajustes encerra o perigo nela.
     */
    public boolean podeReceberResultado(Instant agora) {
        return tipo.perigo() && !agora.isBefore(primeiroResultado()) && agora.isBefore(fimDosResultados());
    }

    private Instant primeiroResultado() {
        Instant peloPadrao = fimPeloPadrao();
        return peloPadrao.isBefore(fim) ? peloPadrao : fim;
    }

    /**
     * Até quando o resultado vale: {@link #FOLGA_RESULTADO_SEGUNDOS} depois do fim, ou depois da duração padrão
     * quando o evento foi encurtado (DA-48): o jogo publicado antes dos ajustes só encerra o perigo nela.
     */
    public Instant fimDosResultados() {
        Instant peloPadrao = fimPeloPadrao();
        return (peloPadrao.isAfter(fim) ? peloPadrao : fim).plusSeconds(FOLGA_RESULTADO_SEGUNDOS);
    }

    private Instant fimPeloPadrao() {
        return inicio.plusSeconds((long) tipo.duracaoSegundos());
    }

    public long duracao() {
        return Duration.between(inicio, fim).getSeconds();
    }
}
