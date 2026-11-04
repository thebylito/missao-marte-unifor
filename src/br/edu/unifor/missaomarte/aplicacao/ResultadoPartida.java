package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.Dificuldade;

/** Números de uma partida vencida, usados nas estatísticas e no ranking. */
public record ResultadoPartida(
        String piloto,
        Dificuldade dificuldade,
        int pontos,
        int movimentos,
        long tempoSegundos,
        int passageirosResgatados) {
}
