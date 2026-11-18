package br.edu.unifor.missaomarte.grafico.apresentacao;

import com.badlogic.gdx.Input.Keys;

/**
 * Botões de toque da partida (Fase 6). Cada um "aperta" a mesma tecla do teclado: o toque entra pelo mesmo caminho
 * (MapaDeTeclas, CenaDaPartida, Partida), então nenhuma regra nova aparece na interface.
 */
public enum Botao {
    CIMA(Keys.W, "▲"),
    BAIXO(Keys.S, "▼"),
    ESQUERDA(Keys.A, "◀"),
    DIREITA(Keys.D, "▶"),
    EMBARCAR(Keys.C, "Embarcar"),
    ABORTAR(Keys.Q, "Abortar"),
    SOM(Keys.M, "Som");

    private final int tecla;
    private final String rotulo;

    Botao(int tecla, String rotulo) {
        this.tecla = tecla;
        this.rotulo = rotulo;
    }

    /** A tecla que este botão equivale. */
    public int getTecla() { return tecla; }
    public String getRotulo() { return rotulo; }

    public boolean isDirecao() {
        return this == CIMA || this == BAIXO || this == ESQUERDA || this == DIREITA;
    }
}
