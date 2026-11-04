package br.edu.unifor.missaomarte.dominio;

/**
 * Estratégia (padrão Strategy) que decide para onde um inimigo vai no turno.
 * Separar a decisão do inimigo permite trocar o comportamento e testar com movimento previsível.
 */
@FunctionalInterface
public interface MovimentoInimigo {

    Posicao proximaPosicao(Posicao atual, Mapa mapa);

    /** Inimigo que não sai do lugar (útil em testes). */
    MovimentoInimigo PARADO = (atual, mapa) -> atual;
}
