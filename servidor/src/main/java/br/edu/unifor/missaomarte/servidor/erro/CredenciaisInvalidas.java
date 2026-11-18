package br.edu.unifor.missaomarte.servidor.erro;

/** Login com nome inexistente ou senha errada (a mesma mensagem para os dois casos): 401. */
public class CredenciaisInvalidas extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CredenciaisInvalidas() {
        super("nome ou senha incorretos");
    }
}
