package br.edu.unifor.missaomarte.servidor.partida;

import java.time.Instant;
import java.util.UUID;

/** Fase 8: o bilhete da partida, que o jogo manda de volta no registro ({@code bilhete}), e até quando ele vale. */
public record BilheteResposta(UUID bilhete, Instant validoAte) {
}
