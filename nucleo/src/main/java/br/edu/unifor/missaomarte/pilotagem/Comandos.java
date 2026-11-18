package br.edu.unifor.missaomarte.pilotagem;

/**
 * O que o jogador quer fazer neste instante. A apresentação traduz o teclado ou os dedos na tela para isto; a
 * partida não sabe de onde veio. Girar para os dois lados ao mesmo tempo se anula.
 */
public record Comandos(boolean girarEsquerda, boolean girarDireita, boolean motor) {

    public static final Comandos NENHUM = new Comandos(false, false, false);
    public static final Comandos MOTOR = new Comandos(false, false, true);
}
