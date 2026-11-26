package br.edu.unifor.missaomarte.grafico.online;

import java.time.Instant;

/**
 * Jogador autenticado: o nome, o token para os pedidos protegidos e quando o token vence ({@code null} se o servidor
 * não informou). Fica na memória do jogo e, desde a DA-38, também nas preferências do app ({@link SessaoGuardada}),
 * para sobreviver ao Android matar o jogo em segundo plano.
 */
public record Sessao(String nome, String token, Instant expiraEm) {

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
