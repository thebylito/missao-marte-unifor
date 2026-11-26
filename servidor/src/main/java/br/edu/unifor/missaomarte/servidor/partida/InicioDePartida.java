package br.edu.unifor.missaomarte.servidor.partida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Fase 8: o pedido de bilhete no começo de uma partida. O modo é opcional (sem ele, clássico, como no registro). */
public record InicioDePartida(
        Modo modo,
        @NotNull(message = "informe a dificuldade (FACIL, MEDIO ou DIFICIL)") Dificuldade dificuldade,
        @Schema(description = "Número da horda. Obrigatório no modo HORDA.") Long horda) {

    public InicioDePartida {
        if (modo == null) {
            modo = Modo.CLASSICO;
        }
    }
}
