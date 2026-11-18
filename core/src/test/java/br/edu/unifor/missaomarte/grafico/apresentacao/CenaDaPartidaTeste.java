package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.MovimentoInimigo;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import com.badlogic.gdx.Input.Keys;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A cena da partida dirigida por teclas, com sons anotados e o tempo avançado à mão: sem janela, áudio nem relógio
 * real (critério da issue #6).
 */
class CenaDaPartidaTeste {

    private final Apoio.SonsGravados sons = new Apoio.SonsGravados();
    private final Apoio.RelogioParado relogio = new Apoio.RelogioParado();
    private final ServicoRanking ranking = new ServicoRanking(new Apoio.RepositorioEmMemoria(), relogio);

    /** Professor à direita da plataforma e um asteroide embaixo dela. */
    private CenaDaPartida cena() {
        Missao m = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);
        m.adicionarPassageiro(new Professor("Dr. Silva", new Posicao(1, 0)));
        m.adicionarAsteroide(new Asteroide(new Posicao(0, 1)));
        Partida p = new Partida("Ana", Dificuldade.MEDIO, m, relogio);
        return new CenaDaPartida(new ControladorDaPartida(p, ranking), sons);
    }

    @Test
    void movimentoTocaOPropulsorViraANaveEAnima() {
        CenaDaPartida c = cena();
        assertEquals(Direcao.CIMA, c.getDirecaoDaNave());
        assertTrue(c.teclaPressionada(Keys.D));
        assertEquals(List.of(Som.MOVER), sons.tocados);
        assertEquals(Direcao.DIREITA, c.getDirecaoDaNave());
        assertTrue(c.getAnimacao().emAndamento());
        c.avancar(1);
        assertEquals(1f, c.getAnimacao().nave().x());
    }

    @Test
    void embarqueNaoMudaADirecaoETocaOSomDoEmbarque() {
        CenaDaPartida c = cena();
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.C);
        assertEquals(List.of(Som.MOVER, Som.EMBARQUE, Som.ALERTA), sons.tocados);   // era o único: volte à base
        assertEquals(Direcao.DIREITA, c.getDirecaoDaNave());
        assertEquals(1, c.getAnimacao().embarques().size());
    }

    @Test
    void colisaoExplodeTremeENaoTerminaAPartida() {
        CenaDaPartida c = cena();
        c.teclaPressionada(Keys.S);                                   // em cima do asteroide
        assertEquals(List.of(Som.MOVER, Som.COLISAO), sons.tocados);
        assertEquals(1, c.getEfeitos().explosoes().size());
        assertEquals(new Posicao(0, 1), c.getEfeitos().explosoes().get(0).celula());
        assertFalse(c.getEfeitos().isNaveDestruida());
        c.avancar(2);
        assertTrue(c.fimParaMostrar().isEmpty());
    }

    @Test
    void naveDestruidaSoVaiParaOFimDepoisDaExplosao() {
        CenaDaPartida c = cena();
        c.teclaPressionada(Keys.S);       // 1ª colisão
        c.teclaPressionada(Keys.W);
        c.teclaPressionada(Keys.S);       // 2ª
        c.teclaPressionada(Keys.W);
        c.teclaPressionada(Keys.S);       // 3ª: destruída
        assertTrue(c.getControlador().getFim().isPresent(), "a Partida já decidiu o fim");
        assertTrue(c.getEfeitos().isNaveDestruida());
        assertTrue(sons.tocados.contains(Som.EXPLOSAO));
        assertTrue(c.fimParaMostrar().isEmpty(), "mas a tela espera a explosão");
        c.avancar(0.5f);
        assertTrue(c.fimParaMostrar().isEmpty());
        c.avancar(0.5f);
        assertEquals(Partida.Estado.DERROTA, c.fimParaMostrar().orElseThrow().estado());
    }

    @Test
    void abandonarVaiDiretoParaOFim() {
        CenaDaPartida c = cena();
        c.teclaPressionada(Keys.Q);
        assertEquals(Partida.Estado.ABORTADA, c.fimParaMostrar().orElseThrow().estado());
        assertTrue(sons.tocados.isEmpty());
    }

    @Test
    void vitoriaEsperaSoODeslizeTerminar() {
        CenaDaPartida c = cena();
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.C);
        c.teclaPressionada(Keys.A);
        assertTrue(c.fimParaMostrar().isEmpty(), "a nave ainda está deslizando até a plataforma");
        c.avancar(AnimacaoDoTurno.DURACAO);
        assertEquals(Partida.Estado.VITORIA, c.fimParaMostrar().orElseThrow().estado());
    }

    @Test
    void mLigaEDesligaOSomSemGastarTurno() {
        CenaDaPartida c = cena();
        assertTrue(c.isSomLigado());
        assertFalse(c.teclaPressionada(Keys.M));
        assertFalse(c.isSomLigado());
        assertEquals(0, c.getControlador().getPartida().getMovimentos());
        c.teclaPressionada(Keys.D);
        assertTrue(sons.tocados.isEmpty(), "mudo: nada toca");
        c.teclaPressionada(Keys.M);
        c.teclaPressionada(Keys.A);
        assertEquals(List.of(Som.MOVER), sons.tocados);
    }

    @Test
    void teclaQueNaoEhDoJogoNaoFazNada() {
        CenaDaPartida c = cena();
        assertFalse(c.teclaPressionada(Keys.X));
        assertTrue(sons.tocados.isEmpty());
        assertFalse(c.getAnimacao().emAndamento());
    }

    @Test
    void relogioSomaOTempoParaAsAnimacoesEmLaco() {
        CenaDaPartida c = cena();
        c.avancar(0.25f);
        c.avancar(0.5f);
        assertEquals(0.75f, c.getRelogio(), 1e-6);
    }
}
