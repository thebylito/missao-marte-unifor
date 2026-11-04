package br.edu.unifor.missaomarte.dominio;

public class Professor extends Passageiro {

    public Professor(String nome, Posicao posicao) {
        super(nome, posicao);
    }

    @Override
    public TipoPassageiro getTipo() {
        return TipoPassageiro.PROFESSOR;
    }

    @Override
    public int getPontuacao() {
        return 10; // Professor vale 10 pontos
    }
}
