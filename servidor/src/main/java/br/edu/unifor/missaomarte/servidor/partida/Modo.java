package br.edu.unifor.missaomarte.servidor.partida;

/**
 * Modo de jogo (Fase 7): o clássico (grade por turnos) ou a pilotagem. Como a {@link Dificuldade}, é uma cópia dos
 * nomes do jogo, de propósito: o servidor depende só do contrato da API. Cada modo tem o seu ranking.
 */
public enum Modo { CLASSICO, PILOTAGEM }
