package br.edu.unifor.missaomarte.servidor.jogador;

import jakarta.validation.constraints.NotBlank;

/** Pedido de login. */
public record Credenciais(
        @NotBlank(message = "informe o nome") String nome,
        @NotBlank(message = "informe a senha") String senha) {

    @Override
    public String toString() {
        return "Credenciais[nome=" + nome + ", senha=***]";
    }
}
