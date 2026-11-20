package br.edu.unifor.missaomarte.servidor.partida;

import java.time.Instant;

/** Uma partida do histórico, como a API devolve (sem dados do jogador: o histórico é sempre do próprio). */
public record PartidaResposta(Long id, Resultado resultado, Dificuldade dificuldade, int pontos, int passageiros,
                              int movimentos, long tempoSegundos, Instant registradaEm, Modo modo) {

    public static PartidaResposta de(Partida p) {
        return new PartidaResposta(p.getId(), p.getResultado(), p.getDificuldade(), p.getPontos(), p.getPassageiros(),
                p.getMovimentos(), p.getTempoSegundos(), p.getRegistradaEm(), p.getModo());
    }
}
