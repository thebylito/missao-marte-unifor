package br.edu.unifor.missaomarte.servidor.ranking;

import br.edu.unifor.missaomarte.servidor.partida.Dificuldade;
import java.time.Instant;

/** Uma linha do Top 10: a melhor vitória de um jogador. */
public record PosicaoNoRanking(int posicao, String nome, int pontos, Dificuldade dificuldade, int passageiros,
                               long tempoSegundos, Instant registradaEm) {
}
