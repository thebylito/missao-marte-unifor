package br.edu.unifor.missaomarte.grafico.online;

/** Uma partida do histórico do jogador, como o servidor devolve. */
public record PartidaDoHistorico(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                                 long tempoSegundos, String registradaEm) {
}
