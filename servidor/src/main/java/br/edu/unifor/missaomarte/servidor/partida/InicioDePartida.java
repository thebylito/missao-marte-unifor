package br.edu.unifor.missaomarte.servidor.partida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Fase 8: o pedido de bilhete no começo de uma partida. O modo é opcional (sem ele, clássico, como no registro). */
public record InicioDePartida(
        Modo modo,
        @NotNull(message = "informe a dificuldade (FACIL, MEDIO ou DIFICIL)") Dificuldade dificuldade,
        @Schema(description = "Número da horda. Obrigatório no modo HORDA.") Long horda,
        @Schema(description = "O jogo aplica os ajustes gravados na horda (DA-48). Sem ele, uma horda com ajustes "
                + "diferentes do padrão recusa o bilhete (409 JOGO_DESATUALIZADO).") Boolean comAjustes) {

    public InicioDePartida {
        if (modo == null) {
            modo = Modo.CLASSICO;
        }
    }

    /** O pedido do jogo publicado antes da DA-48, que não conhece os ajustes. */
    public InicioDePartida(Modo modo, Dificuldade dificuldade, Long horda) {
        this(modo, dificuldade, horda, null);
    }
}
