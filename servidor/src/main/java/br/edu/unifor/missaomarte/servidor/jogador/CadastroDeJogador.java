package br.edu.unifor.missaomarte.servidor.jogador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido de cadastro. Nome: 3 a 20 caracteres (o mesmo limite do nome do piloto no jogo), só letras, números,
 * espaço, ponto, hífen e sublinhado. Senha: 6 a 72 caracteres (72 é o limite do BCrypt).
 */
public record CadastroDeJogador(
        @NotBlank(message = "informe o nome")
        @Size(min = 3, max = 20, message = "o nome deve ter de 3 a 20 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{N} ._-]*", message = "use só letras, números, espaço, ponto, hífen ou sublinhado")
        String nome,
        @NotBlank(message = "informe a senha")
        @Size(min = 6, max = 72, message = "a senha deve ter de 6 a 72 caracteres")
        String senha) {

    /** Nunca mostra a senha, nem em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "CadastroDeJogador[nome=" + nome + ", senha=***]";
    }
}
