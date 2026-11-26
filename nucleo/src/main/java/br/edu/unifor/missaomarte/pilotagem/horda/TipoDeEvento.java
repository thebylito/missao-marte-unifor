package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Os eventos da Fase 8 (tabela 2.2 da análise). Os três primeiros são perigos e podem entrar no roteiro de uma
 * horda; o VIP só ajuda (vale no placar da semana) e nunca entra no roteiro.
 */
public enum TipoDeEvento {
    CHUVA_DE_METEOROS(20, true),
    TEMPESTADE(25, true),
    PANE_DE_GRAVIDADE(20, true),
    PASSAGEIRO_VIP(45, false);

    private final double duracaoSegundos;
    private final boolean perigo;

    TipoDeEvento(double duracaoSegundos, boolean perigo) {
        this.duracaoSegundos = duracaoSegundos;
        this.perigo = perigo;
    }

    public double duracaoSegundos() { return duracaoSegundos; }

    /** O evento só atrapalha a partida (DA-45). */
    public boolean perigo() { return perigo; }
}
