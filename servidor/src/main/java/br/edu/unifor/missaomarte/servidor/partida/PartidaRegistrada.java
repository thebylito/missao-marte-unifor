package br.edu.unifor.missaomarte.servidor.partida;

import java.time.Instant;

/**
 * A resposta do registro de uma partida: os mesmos campos do histórico ({@link PartidaResposta}) e, desde a Fase 8,
 * as moedas que ela rendeu e o saldo depois dela. O histórico não muda: as moedas aparecem só aqui.
 */
public record PartidaRegistrada(Long id, Resultado resultado, Dificuldade dificuldade, int pontos, int passageiros,
                                int movimentos, long tempoSegundos, Instant registradaEm, Modo modo,
                                int moedasGanhas, int saldo) {

    public static PartidaRegistrada de(Partida p, int moedasGanhas, int saldo) {
        return new PartidaRegistrada(p.getId(), p.getResultado(), p.getDificuldade(), p.getPontos(),
                p.getPassageiros(), p.getMovimentos(), p.getTempoSegundos(), p.getRegistradaEm(), p.getModo(),
                moedasGanhas, saldo);
    }
}
