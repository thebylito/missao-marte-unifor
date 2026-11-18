package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.grafico.apresentacao.AnimacaoDoTurno.Instantaneo;
import br.edu.unifor.missaomarte.grafico.apresentacao.AnimacaoDoTurno.Ponto;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O deslize entre turnos, com o tempo controlado pelo teste. */
class AnimacaoDoTurnoTeste {

    private static final float DELTA = 1e-5f;

    /** Inimigo que anda uma célula para a esquerda a cada turno. */
    private final Missao missao = new Missao(new Mapa(3), new Nave(5), (atual, mapa) -> atual.deslocar(-1, 0));

    private static void pontoEm(float x, float y, Ponto p) {
        assertEquals(x, p.x(), DELTA, "x");
        assertEquals(y, p.y(), DELTA, "y");
    }

    @Test
    void semTurnoNadaSeMove() {
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        assertFalse(a.emAndamento());
        pontoEm(0, 0, a.nave());
    }

    @Test
    void turnoSemMudancaVisivelNaoAnima() {
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        a.iniciar(Instantaneo.de(missao), Instantaneo.de(missao));
        assertFalse(a.emAndamento());
    }

    @Test
    void naveDeslizaDaCelulaAntigaParaANova() {
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        Instantaneo antes = Instantaneo.de(missao);
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        a.iniciar(antes, Instantaneo.de(missao));

        assertTrue(a.emAndamento());
        pontoEm(0, 0, a.nave());                          // começa onde estava
        a.avancar(AnimacaoDoTurno.DURACAO / 2);
        pontoEm(0.5f, 0, a.nave());                       // na metade do tempo, na metade do caminho (curva simétrica)
        a.avancar(AnimacaoDoTurno.DURACAO / 4);
        assertTrue(a.nave().x() > 0.75f, "acelera no meio e freia no fim: passou dos 75%");
        a.avancar(1);
        pontoEm(1, 0, a.nave());                          // termina na célula nova e para
        assertFalse(a.emAndamento());
    }

    @Test
    void inimigosDeslizamJuntoComANave() {
        missao.adicionarInimigo(new Inimigo(new Posicao(2, 2)));
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        Instantaneo antes = Instantaneo.de(missao);
        missao.moverInimigos();
        a.iniciar(antes, Instantaneo.de(missao));
        a.avancar(AnimacaoDoTurno.DURACAO / 2);
        pontoEm(1.5f, 2, a.inimigos().get(0));
        a.avancar(1);
        pontoEm(1, 2, a.inimigos().get(0));
    }

    @Test
    void passageiroQueEmbarcouVaiAteANaveDuranteODeslize() {
        Passageiro p = new Professor("Dr. Silva", Posicao.ORIGEM);
        missao.adicionarPassageiro(p);
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        Instantaneo antes = Instantaneo.de(missao);
        missao.embarcar(p);
        a.iniciar(antes, Instantaneo.de(missao));

        assertTrue(a.passageirosNoMapa().isEmpty(), "já não está no mapa");
        a.avancar(AnimacaoDoTurno.DURACAO / 2);
        List<AnimacaoDoTurno.Embarque> embarques = a.embarques();
        assertEquals(1, embarques.size());
        assertSame(p, embarques.get(0).passageiro());
        assertEquals(0.5f, embarques.get(0).progresso(), DELTA);
        a.avancar(1);
        assertTrue(a.embarques().isEmpty(), "terminado o deslize, o passageiro já está a bordo");
    }

    @Test
    void novoTurnoNoMeioDoDeslizeComecaDaFotoNova() {
        AnimacaoDoTurno a = new AnimacaoDoTurno(Instantaneo.de(missao));
        Instantaneo antes = Instantaneo.de(missao);
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        a.iniciar(antes, Instantaneo.de(missao));
        a.avancar(AnimacaoDoTurno.DURACAO / 3);

        Instantaneo antes2 = Instantaneo.de(missao);      // a tecla seguinte não espera a animação
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        a.iniciar(antes2, Instantaneo.de(missao));
        pontoEm(1, 0, a.nave());                          // pula para o turno novo: nenhuma tecla se perde
        a.avancar(1);
        pontoEm(2, 0, a.nave());
    }

    @Test
    void aFotoNaoMudaQuandoAMissaoMuda() {
        Instantaneo foto = Instantaneo.de(missao);
        missao.adicionarPassageiro(new Professor("depois", new Posicao(1, 1)));
        missao.getNave().mover(Direcao.BAIXO, missao.getMapa());
        assertTrue(foto.passageiros().isEmpty());
        assertEquals(Posicao.ORIGEM, foto.nave());
    }
}
