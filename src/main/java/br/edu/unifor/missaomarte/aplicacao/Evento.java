package br.edu.unifor.missaomarte.aplicacao;

/**
 * Algo que aconteceu num turno. A partida devolve os eventos em ordem e a interface decide como mostrá-los.
 * Assim a regra não sabe se a saída é um console, uma janela ou um teste.
 */
public record Evento(Tipo tipo, String passageiro, int valor) {

    public enum Tipo {
        ABORTADA,
        COMANDO_INVALIDO,
        SEM_PASSAGEIRO_NA_POSICAO,
        PASSAGEIRO_EMBARCADO,   // passageiro = nome, valor = pontos ganhos
        NAVE_CHEIA,
        COLISAO,                // valor = vidas restantes
        NAVE_DESTRUIDA,
        PONTOS_ESGOTADOS,
        RETORNE_A_PLATAFORMA,
        VITORIA
    }

    public static Evento de(Tipo tipo) {
        return new Evento(tipo, null, 0);
    }
}
