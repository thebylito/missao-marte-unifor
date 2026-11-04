package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.igual;

import br.edu.unifor.missaomarte.teste.Teste;

class DificuldadeTeste {

    @Teste
    void cadaDificuldadeTemSeusNumeros() {
        igual(30, Dificuldade.FACIL.getPontosIniciais());
        igual(20, Dificuldade.MEDIO.getPontosIniciais());
        igual(45, Dificuldade.DIFICIL.getPontosIniciais());
        igual(4, Dificuldade.FACIL.getPassageiros());
        igual(1, Dificuldade.FACIL.getAsteroides());
        igual(1, Dificuldade.FACIL.getInimigos());
        igual(5, Dificuldade.MEDIO.getPassageiros());
        igual(2, Dificuldade.MEDIO.getAsteroides());
        igual(2, Dificuldade.MEDIO.getInimigos());
        igual(5, Dificuldade.DIFICIL.getPassageiros());
        igual(3, Dificuldade.DIFICIL.getAsteroides());
        igual(3, Dificuldade.DIFICIL.getInimigos());
    }

    @Teste
    void textoDigitadoViraDificuldadeComMedioComoPadrao() {
        igual(Dificuldade.FACIL, Dificuldade.deString("facil"));
        igual(Dificuldade.FACIL, Dificuldade.deString(" FÁCIL "));
        igual(Dificuldade.DIFICIL, Dificuldade.deString("dificil"));
        igual(Dificuldade.DIFICIL, Dificuldade.deString("Difícil"));
        igual(Dificuldade.MEDIO, Dificuldade.deString("medio"));
        igual(Dificuldade.MEDIO, Dificuldade.deString("qualquer coisa"));
        igual(Dificuldade.MEDIO, Dificuldade.deString(""));
        igual(Dificuldade.MEDIO, Dificuldade.deString(null));
    }

    @Teste
    void nomeExibidoTemAcento() {
        igual("Fácil", Dificuldade.FACIL.toString());
        igual("Médio", Dificuldade.MEDIO.toString());
        igual("Difícil", Dificuldade.DIFICIL.toString());
    }
}
