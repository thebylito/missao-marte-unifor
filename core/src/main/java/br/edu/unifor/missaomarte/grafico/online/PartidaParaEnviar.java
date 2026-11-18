package br.edu.unifor.missaomarte.grafico.online;

/**
 * O que o cliente manda ao servidor no fim de uma partida. É o contrato da API ({@code POST /api/partidas}): os
 * nomes de resultado (VITORIA, DERROTA, ABORTADA) e de dificuldade (FACIL, MEDIO, DIFICIL) são os mesmos do jogo.
 */
public record PartidaParaEnviar(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                                long tempoSegundos) {
}
