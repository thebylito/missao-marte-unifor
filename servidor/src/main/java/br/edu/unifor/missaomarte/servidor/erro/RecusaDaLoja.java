package br.edu.unifor.missaomarte.servidor.erro;

import java.util.Map;

/**
 * Fase 8: a loja recusou o pedido (409). O {@link Motivo} vai na resposta (campo {@code motivo}), para o jogo
 * diferenciar os casos sem ler o texto; {@code extras} vão junto (por exemplo o preço e o saldo).
 */
public class RecusaDaLoja extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** Comprar uma skin que o jogador já tem (a {@code padrao} incluída). */
        JA_POSSUI,
        /** Comprar uma skin mais cara que o saldo. */
        SALDO_INSUFICIENTE,
        /** Equipar uma skin que o jogador não tem. */
        NAO_POSSUI
    }

    private final Motivo motivo;
    private final transient Map<String, Object> extras;

    public RecusaDaLoja(Motivo motivo, String mensagem) {
        this(motivo, mensagem, Map.of());
    }

    public RecusaDaLoja(Motivo motivo, String mensagem, Map<String, Object> extras) {
        super(mensagem);
        this.motivo = motivo;
        this.extras = Map.copyOf(extras);
    }

    public Motivo getMotivo() { return motivo; }
    public Map<String, Object> getExtras() { return extras; }
}
