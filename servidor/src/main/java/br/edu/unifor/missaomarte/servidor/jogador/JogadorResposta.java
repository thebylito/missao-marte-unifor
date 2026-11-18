package br.edu.unifor.missaomarte.servidor.jogador;

import java.time.Instant;

/** O que a API mostra de um jogador: nunca a senha nem o hash. */
public record JogadorResposta(Long id, String nome, Instant criadoEm) {

    public static JogadorResposta de(Jogador j) {
        return new JogadorResposta(j.getId(), j.getNome(), j.getCriadoEm());
    }
}
