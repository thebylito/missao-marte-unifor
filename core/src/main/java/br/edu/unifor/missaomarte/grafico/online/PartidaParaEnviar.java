package br.edu.unifor.missaomarte.grafico.online;

/**
 * O que o cliente manda ao servidor no fim de uma partida. É o contrato da API ({@code POST /api/partidas}): os
 * nomes de resultado (VITORIA, DERROTA, ABORTADA) e de dificuldade (FACIL, MEDIO, DIFICIL) são os mesmos do jogo.
 * Fase 7: o modo (CLASSICO ou PILOTAGEM); na pilotagem, os "movimentos" são os pousos.
 */
public record PartidaParaEnviar(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                                long tempoSegundos, String modo) {

    /** Uma partida clássica (o formato das Fases 5 e 6, sem o campo modo). */
    public PartidaParaEnviar(String resultado, String dificuldade, int pontos, int passageiros, int movimentos,
                             long tempoSegundos) {
        this(resultado, dificuldade, pontos, passageiros, movimentos, tempoSegundos, "CLASSICO");
    }
}
