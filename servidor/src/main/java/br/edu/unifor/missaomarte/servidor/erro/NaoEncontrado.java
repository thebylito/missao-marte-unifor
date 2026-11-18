package br.edu.unifor.missaomarte.servidor.erro;

/** Recurso que não existe: 404. */
public class NaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NaoEncontrado(String mensagem) {
        super(mensagem);
    }
}
