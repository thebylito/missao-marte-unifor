package br.edu.unifor.missaomarte.grafico.online;

/** Uma partida do histórico do jogador, como o servidor devolve. Fase 7: o modo (CLASSICO ou PILOTAGEM). */
public record PartidaDoHistorico(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                                 long tempoSegundos, String registradaEm, String modo) {

    /** Uma partida clássica (o formato das Fases 5 e 6, sem o modo). */
    public PartidaDoHistorico(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                              long tempoSegundos, String registradaEm) {
        this(resultado, dificuldade, pontos, passageiros, movimentos, tempoSegundos, registradaEm, "CLASSICO");
    }
}
