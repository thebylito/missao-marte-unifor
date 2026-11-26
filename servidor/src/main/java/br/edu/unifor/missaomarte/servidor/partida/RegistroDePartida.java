package br.edu.unifor.missaomarte.servidor.partida;

import io.swagger.v3.oas.annotations.media.Schema;
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
        Modo modo,
        @Schema(description = "Bilhete emitido no início da partida. Obrigatório no modo HORDA.") String bilhete,
        @Schema(description = "Número da horda. Obrigatório no modo HORDA.") Long horda,
        @Schema(description = "O jogo segue os ajustes do servidor (DA-48). Na Pilotagem, com ajustes diferentes do "
                + "padrão, o resultado sem ele só vale se o bilhete foi emitido com o padrão; senão, 409 "
                + "JOGO_DESATUALIZADO.") Boolean comAjustes) {

    /**
     * Fase 7: o modo é opcional; sem ele (clientes das Fases 5 e 6), a partida é do modo clássico. Fase 8: o bilhete
     * ({@code POST /api/partidas/inicio}) também é opcional; sem ele (clientes 7.x), ou com um bilhete inválido, a
     * partida entra como sempre, mas não rende moedas.
     */
    public RegistroDePartida {
        if (modo == null) {
            modo = Modo.CLASSICO;
        }
    }
}
