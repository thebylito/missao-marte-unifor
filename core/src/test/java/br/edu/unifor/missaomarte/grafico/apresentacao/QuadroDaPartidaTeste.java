package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.MovimentoInimigo;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O quadro lê a missão e diz o que desenhar em cada célula, com a mesma prioridade do console. */
class QuadroDaPartidaTeste {

    private final Missao missao = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);

    private Conteudo em(int x, int y) {
        return QuadroDaPartida.conteudo(missao, new Posicao(x, y));
    }

    @Test
    void temUmaCelulaPorPosicaoNaOrdemDoConsole() {
        List<QuadroDaPartida.Celula> celulas = QuadroDaPartida.de(missao);
        assertEquals(25, celulas.size());
        assertEquals(new Posicao(-2, -2), celulas.get(0).posicao());   // primeira linha impressa no console
        assertEquals(new Posicao(-1, -2), celulas.get(1).posicao());
        assertEquals(new Posicao(2, 2), celulas.get(24).posicao());
        assertEquals(1, celulas.stream().filter(c -> c.conteudo() == Conteudo.NAVE).count());
    }

    @Test
    void naveComecaSobreAPlataformaEAPlataformaApareceQuandoElaSai() {
        assertEquals(Conteudo.NAVE, em(0, 0));
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        assertEquals(Conteudo.PLATAFORMA, em(0, 0));
        assertEquals(Conteudo.NAVE, em(1, 0));
        assertEquals(Conteudo.VAZIO, em(-1, 0));
    }

    @Test
    void cadaTipoDePassageiroTemOSeuConteudo() {
        missao.adicionarPassageiro(new Professor("Dr. Silva", new Posicao(-2, 0)));
        missao.adicionarPassageiro(new Engenheiro("Eng. Rosa", new Posicao(-1, 0)));
        missao.adicionarPassageiro(new Astronauta("Ast. Maria", new Posicao(2, 0)));
        assertEquals(Conteudo.PROFESSOR, em(-2, 0));
        assertEquals(Conteudo.ENGENHEIRO, em(-1, 0));
        assertEquals(Conteudo.ASTRONAUTA, em(2, 0));
    }

    @Test
    void prioridadeNavePassageiroAsteroideInimigo() {
        missao.adicionarPassageiro(new Professor("embaixo da nave", new Posicao(1, 0)));
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());

        missao.adicionarPassageiro(new Professor("sobre o asteroide", new Posicao(1, 1)));
        missao.adicionarAsteroide(new Asteroide(new Posicao(1, 1)));
        missao.adicionarInimigo(new Inimigo(new Posicao(1, 1)));

        missao.adicionarAsteroide(new Asteroide(new Posicao(-1, 1)));
        missao.adicionarInimigo(new Inimigo(new Posicao(-1, 1)));

        missao.adicionarInimigo(new Inimigo(new Posicao(-2, 2)));

        assertEquals(Conteudo.NAVE, em(1, 0));
        assertEquals(Conteudo.PROFESSOR, em(1, 1));
        assertEquals(Conteudo.ASTEROIDE, em(-1, 1));
        assertEquals(Conteudo.INIMIGO, em(-2, 2));
    }

    @Test
    void osSimbolosSaoOsDoConsole() {
        assertEquals("@PETXL.#", "" + Conteudo.NAVE.getSimbolo() + Conteudo.PROFESSOR.getSimbolo()
                + Conteudo.ENGENHEIRO.getSimbolo() + Conteudo.ASTRONAUTA.getSimbolo()
                + Conteudo.INIMIGO.getSimbolo() + Conteudo.PLATAFORMA.getSimbolo()
                + Conteudo.VAZIO.getSimbolo() + Conteudo.ASTEROIDE.getSimbolo());
    }
}
