package br.edu.unifor.missaomarte.servidor.partida;

import jakarta.validation.constraints.NotNull;

/** Fase 8: o pedido de bilhete no começo de uma partida. O modo é opcional (sem ele, clássico, como no registro). */
public record InicioDePartida(
        Modo modo,
        @NotNull(message = "informe a dificuldade (FACIL, MEDIO ou DIFICIL)") Dificuldade dificuldade) {

    public InicioDePartida {
        if (modo == null) {
            modo = Modo.CLASSICO;
        }
    }
}
