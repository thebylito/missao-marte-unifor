package br.edu.unifor.missaomarte.pilotagem.horda;

/** O que um {@link Modificador} da horda muda, fixo durante as 2 h da horda. */
public enum TipoDeModificador {
    /** Aceleração lateral constante; o sinal é a direção (+ para a direita). */
    VENTO,
    /** Fator que multiplica a gravidade. */
    GRAVIDADE,
    /** Fator que multiplica o tanque (nunca no Difícil). */
    TANQUE
}
