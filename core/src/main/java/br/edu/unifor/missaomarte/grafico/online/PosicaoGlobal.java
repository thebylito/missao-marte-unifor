package br.edu.unifor.missaomarte.grafico.online;

/** Uma linha do Top 10 global, como o servidor devolve. */
public record PosicaoGlobal(int posicao, String nome, int pontos, String dificuldade, int passageiros,
                            long tempoSegundos, String registradaEm) {
}
