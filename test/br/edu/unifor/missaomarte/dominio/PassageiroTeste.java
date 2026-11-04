package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.igual;

import br.edu.unifor.missaomarte.teste.Teste;

class PassageiroTeste {

    @Teste
    void cadaTipoValeSuaPontuacao() {
        Posicao p = new Posicao(1, 1);
        igual(10, new Professor("x", p).getPontuacao());
        igual(15, new Engenheiro("x", p).getPontuacao());
        igual(20, new Astronauta("x", p).getPontuacao());
    }

    @Teste
    void cadaSubclasseInformaSeuTipo() {
        Posicao p = new Posicao(1, 1);
        igual(TipoPassageiro.PROFESSOR, new Professor("x", p).getTipo());
        igual(TipoPassageiro.ENGENHEIRO, new Engenheiro("x", p).getTipo());
        igual(TipoPassageiro.ASTRONAUTA, new Astronauta("x", p).getTipo());
        igual("Engenheiro", TipoPassageiro.ENGENHEIRO.getNome());
    }
}
