package br.edu.unifor.missaomarte.servidor.erro;

/** Cadastro com um nome que já existe (sem diferenciar maiúsculas): 409. */
public class NomeEmUso extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NomeEmUso(String nome) {
        super("o nome \"" + nome + "\" já está em uso");
    }
}
