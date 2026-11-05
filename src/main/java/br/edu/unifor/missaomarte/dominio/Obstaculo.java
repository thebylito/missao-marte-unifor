package br.edu.unifor.missaomarte.dominio;

/** Qualquer coisa no mapa que tira uma vida da nave quando ocupa a mesma célula. */
public interface Obstaculo {

    Posicao getPosicao();

    default boolean atingiu(Nave nave) {
        return nave.estaEm(getPosicao());
    }
}
