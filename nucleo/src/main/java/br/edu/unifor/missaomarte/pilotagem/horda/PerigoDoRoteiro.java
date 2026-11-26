package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Um perigo marcado para um instante da partida da horda, igual para todos os pilotos.
 *
 * @param tipo     sempre um evento com {@link TipoDeEvento#perigo()}
 * @param instante segundos de voo desde o começo da partida, de 20 a 120
 * @param semente  semente própria do efeito (por exemplo, as trajetórias da chuva de meteoros)
 */
public record PerigoDoRoteiro(TipoDeEvento tipo, double instante, long semente) {}
