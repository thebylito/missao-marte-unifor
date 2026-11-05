package br.edu.unifor.missaomarte.dominio;

/** Uma linha do ranking. */
public record RegistroRanking(
        String nome,
        int pontos,
        Dificuldade dificuldade,
        int passageirosColetados,
        String dataHora,
        long tempoJogoSegundos) {
}
