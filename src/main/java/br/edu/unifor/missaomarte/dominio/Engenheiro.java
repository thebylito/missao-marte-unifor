package br.edu.unifor.missaomarte.dominio;

public class Engenheiro extends Passageiro {

    public Engenheiro(String nome, Posicao posicao) {
        super(nome, posicao);
    }

    @Override
    public TipoPassageiro getTipo() {
        return TipoPassageiro.ENGENHEIRO;
    }

    @Override
    public int getPontuacao() {
        return 15; // Engenheiro vale 15 pontos
    }
}
