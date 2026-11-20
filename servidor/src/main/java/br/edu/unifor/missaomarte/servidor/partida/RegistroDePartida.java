package br.edu.unifor.missaomarte.servidor.partida;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido para registrar uma partida concluída. Os limites só barram valores absurdos: o servidor não executa o
 * jogo, então não tem como conferir se a pontuação é legítima (limitação registrada no tutorial da Fase 5).
 */
public record RegistroDePartida(
        @NotNull(message = "informe o resultado (VITORIA, DERROTA ou ABORTADA)") Resultado resultado,
        @NotNull(message = "informe a dificuldade (FACIL, MEDIO ou DIFICIL)") Dificuldade dificuldade,
        @NotNull(message = "informe os pontos") @Min(value = 0, message = "pontos não podem ser negativos")
        @Max(value = 10_000, message = "no máximo 10000 pontos") Integer pontos,
        @NotNull(message = "informe os passageiros") @Min(value = 0, message = "passageiros não podem ser negativos")
        @Max(value = 100, message = "no máximo 100 passageiros") Integer passageiros,
        @NotNull(message = "informe os movimentos") @Min(value = 0, message = "movimentos não podem ser negativos")
        @Max(value = 1_000_000, message = "no máximo 1000000 movimentos") Integer movimentos,
        @NotNull(message = "informe o tempo") @Min(value = 0, message = "o tempo não pode ser negativo")
        @Max(value = 86_400, message = "no máximo 86400 segundos") Long tempoSegundos,
        Modo modo) {

    /** Fase 7: o modo é opcional; sem ele (clientes das Fases 5 e 6), a partida é do modo clássico. */
    public RegistroDePartida {
        if (modo == null) {
            modo = Modo.CLASSICO;
        }
    }
}
