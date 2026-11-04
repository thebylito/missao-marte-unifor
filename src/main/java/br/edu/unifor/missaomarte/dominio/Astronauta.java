package br.edu.unifor.missaomarte.dominio;

public class Astronauta extends Passageiro {

    public Astronauta(String nome, Posicao posicao) {
        super(nome, posicao);
    }

    @Override
    public TipoPassageiro getTipo() {
        return TipoPassageiro.ASTRONAUTA;
    }

    @Override
    public int getPontuacao() {
        return 20; // Astronauta vale 20 pontos
    }
}
