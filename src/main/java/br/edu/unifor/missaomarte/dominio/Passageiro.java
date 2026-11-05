package br.edu.unifor.missaomarte.dominio;

/** Alguém esperando resgate. Cada subclasse define quanto vale embarcá-la. */
public abstract class Passageiro {

    private final String nome;
    private final Posicao posicao;

    protected Passageiro(String nome, Posicao posicao) {
        this.nome = nome;
        this.posicao = posicao;
    }

    public String getNome() { return nome; }
    public Posicao getPosicao() { return posicao; }

    public abstract TipoPassageiro getTipo();

    /** Pontos ganhos ao embarcar este passageiro. */
    public abstract int getPontuacao();
}
