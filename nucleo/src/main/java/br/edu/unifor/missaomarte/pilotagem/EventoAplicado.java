package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;

/** Entrada do histórico da partida. O primeiro passo da simulação é 1. */
public record EventoAplicado(TipoDeEvento tipo, long semente, long passo) {}
