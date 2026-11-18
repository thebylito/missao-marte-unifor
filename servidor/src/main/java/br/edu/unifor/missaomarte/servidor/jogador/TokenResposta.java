package br.edu.unifor.missaomarte.servidor.jogador;

import java.time.Instant;

/** Resposta do login: o token para o cabeçalho {@code Authorization: Bearer <token>} e quando ele expira. */
public record TokenResposta(String token, String tipo, Instant expiraEm, JogadorResposta jogador) {

    @Override
    public String toString() {
        return "TokenResposta[tipo=" + tipo + ", expiraEm=" + expiraEm + ", jogador=" + jogador + ", token=***]";
    }
}
