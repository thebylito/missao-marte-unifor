package br.edu.unifor.missaomarte.servidor.moedas;

/**
 * De onde veio (ou para onde foi) um movimento de moedas (Fase 8). Com a referência, identifica o crédito: o mesmo
 * par nunca entra duas vezes no extrato do jogador.
 *
 * <p>Nesta etapa só há caminho para {@link #VITORIA_CLASSICO}, {@link #VITORIA_LIVRE} e {@link #COMPRA}. A Horda,
 * o fechamento da horda e o da semana já estão previstos, mas só serão creditados nas etapas 4 e 6.
 */
public enum Origem {
    /** Vitória no Clássico: 1 moeda, até 20 por dia de Fortaleza. Referência {@code partida:<id>}. */
    VITORIA_CLASSICO,
    /** Vitória na Pilotagem Livre: 3 moedas, 4 no Difícil. Referência {@code partida:<id>}. */
    VITORIA_LIVRE,
    /** Vitória na Horda (etapa 4). */
    VITORIA_HORDA,
    /** Colocação no fechamento de uma horda (etapa 4). */
    HORDA,
    /** Colocação no fechamento da semana (etapa 6). */
    SEMANA,
    /** Compra de uma skin: valor negativo. Referência {@code skin:<id>}. */
    COMPRA
}
