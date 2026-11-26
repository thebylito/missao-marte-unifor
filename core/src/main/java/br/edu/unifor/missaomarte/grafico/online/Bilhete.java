package br.edu.unifor.missaomarte.grafico.online;

import java.time.Instant;

/** Autorização emitida pelo servidor para uma partida. */
public record Bilhete(String bilhete, Instant validoAte) {
}
