package br.edu.unifor.missaomarte.grafico.online;

/**
 * Por que um pedido ao servidor não deu certo, já com uma mensagem para mostrar ao jogador. A partida nunca
 * depende disto: no pior caso o jogo segue sem o servidor.
 */
public record Falha(Motivo motivo, String mensagem) {

    public enum Motivo {
        /** Servidor desligado, endereço errado, sem rede ou tempo esgotado. */
        SEM_CONEXAO,
        /** 401: senha errada ou sessão expirada. */
        NAO_AUTORIZADO,
        /** 409: nome já em uso. */
        CONFLITO,
        /** 400: o servidor recusou os dados (com o motivo de cada campo). */
        DADOS_INVALIDOS,
        /** 5xx ou outro código inesperado. */
        ERRO_DO_SERVIDOR,
        /** A resposta chegou, mas não no formato combinado. */
        RESPOSTA_INVALIDA
    }

    public static Falha semConexao() {
        return new Falha(Motivo.SEM_CONEXAO, "Servidor indisponível. Você pode jogar como convidado.");
    }
}
