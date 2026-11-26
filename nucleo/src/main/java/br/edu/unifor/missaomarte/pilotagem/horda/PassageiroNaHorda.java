package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.dominio.TipoPassageiro;

/**
 * Onde um passageiro espera na horda.
 *
 * @param ponto índice em {@link DescricaoDeCaverna#pontosDePouso()}
 */
public record PassageiroNaHorda(TipoPassageiro tipo, int ponto) {}
