package br.edu.unifor.missaomarte.pilotagem;

/**
 * Algo que aconteceu no voo, para a apresentação mostrar (mensagem, som, efeito). Como os eventos do jogo clássico,
 * a partida só os produz; quem decide como mostrá-los é a interface.
 *
 * @param mensagem texto pronto para o jogador
 * @param valor    pontos ganhos (ENTREGOU), vidas restantes (EXPLODIU), pontos finais (VITORIA) ou
 *                 sobrevivência (PERIGO_TERMINOU, 1 ou 0); senão 0
 * @param x        onde aconteceu (a posição da nave)
 * @param y        onde aconteceu
 */
public record EventoDeVoo(Tipo tipo, String mensagem, int valor, double x, double y) {

    public enum Tipo {
        DECOLOU,
        POUSOU,
        EMBARCOU,
        NAVE_CHEIA,
        ENTREGOU,
        COMBUSTIVEL_BAIXO,
        EXPLODIU,
        RENASCEU,
        VITORIA,
        DERROTA,
        ABORTADA,
        PERIGO_INICIOU,
        PERIGO_TERMINOU,
        VIP_SURGIU,
        VIP_PEGO,
        VIP_SUMIU
    }
}
