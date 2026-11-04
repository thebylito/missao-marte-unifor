package br.edu.unifor.missaomarte.aplicacao;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;

import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.teste.Teste;

class ComandoTeste {

    @Teste
    void teclasDoConsole() {
        igual(Comando.CIMA, Comando.daTecla('w'));
        igual(Comando.BAIXO, Comando.daTecla('s'));
        igual(Comando.ESQUERDA, Comando.daTecla('a'));
        igual(Comando.DIREITA, Comando.daTecla('d'));
        igual(Comando.EMBARCAR, Comando.daTecla('c'));
        igual(Comando.ABORTAR, Comando.daTecla('q'));
        igual(Comando.INVALIDO, Comando.daTecla('x'));
        igual(Comando.INVALIDO, Comando.daTecla('W'));
    }

    @Teste
    void soMovimentosTemDirecao() {
        igual(Direcao.CIMA, Comando.CIMA.getDirecao().orElseThrow());
        falso(Comando.EMBARCAR.getDirecao().isPresent(), "embarcar não tem direção");
    }
}
