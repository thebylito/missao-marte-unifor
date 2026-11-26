package br.edu.unifor.missaomarte.grafico.online;

import java.time.Instant;

/**
 * Jogador autenticado: o nome, o token para os pedidos protegidos e quando o token vence ({@code null} se o servidor
 * não informou). Fica na memória do jogo e, desde a DA-38, também nas preferências do app ({@link SessaoGuardada}),
 * para sobreviver ao Android matar o jogo em segundo plano.
 */
public record Sessao(String nome, String token, Instant expiraEm, String skin) {

    public static final String SKIN_PADRAO = "padrao";

    public Sessao {
        skin = skin == null || skin.isBlank() ? SKIN_PADRAO : skin;
    }

    public Sessao(String nome, String token, Instant expiraEm) {
        this(nome, token, expiraEm, SKIN_PADRAO);
    }

    public Sessao comSkin(String equipada) {
        return new Sessao(nome, token, expiraEm, equipada);
    }

    /** Sessão sem validade conhecida: vale até o servidor recusar o token. */
    public Sessao(String nome, String token) {
        this(nome, token, null);
    }

    /** O token nunca aparece em texto (log, mensagem de erro, depurador que usa toString). */
    @Override
    public String toString() {
        return "Sessao[nome=" + nome + ", token=***]";
    }
}
