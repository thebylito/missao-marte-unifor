package br.edu.unifor.missaomarte.grafico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ciclo de vida das telas sem janela. {@code Game.setScreen} consulta o tamanho da janela, então o teste
 * usa o {@link MockGraphics} do backend headless no lugar do OpenGL. O {@code create()} não é chamado: ele
 * criaria ShapeRenderer e fontes, que precisam de OpenGL de verdade.
 */
class MissaoMarteGameTeste {

    /** Tela falsa que anota a ordem em que a libGDX (e o jogo) chamam cada método. */
    static final class TelaEspia implements Screen {
        final String nome;
        final List<String> chamadas;

        TelaEspia(String nome, List<String> chamadas) {
            this.nome = nome;
            this.chamadas = chamadas;
        }

        @Override public void show() { chamadas.add(nome + ".show"); }
        @Override public void render(float delta) { chamadas.add(nome + ".render"); }
        @Override public void resize(int largura, int altura) { chamadas.add(nome + ".resize"); }
        @Override public void pause() { chamadas.add(nome + ".pause"); }
        @Override public void resume() { chamadas.add(nome + ".resume"); }
        @Override public void hide() { chamadas.add(nome + ".hide"); }
        @Override public void dispose() { chamadas.add(nome + ".dispose"); }
    }

    @TempDir
    Path pasta;

    private Graphics graficosAntes;
    private final List<String> chamadas = new ArrayList<>();
    private MissaoMarteGame jogo;

    @BeforeEach
    void preparar() {
        graficosAntes = Gdx.graphics;
        Gdx.graphics = new MockGraphics();
        ServicoRanking ranking = new ServicoRanking(
                new RankingArquivoJson(pasta.resolve("ranking.json")), Relogio.DO_SISTEMA);
        jogo = new MissaoMarteGame(new ServicosDoJogo(new GeradorDeMissao(new Random(1)), ranking, Relogio.DO_SISTEMA));
    }

    @AfterEach
    void restaurar() {
        Gdx.graphics = graficosAntes;
    }

    @Test
    void trocarDeTelaEscondeELiberaAAnterior() {
        TelaEspia menu = new TelaEspia("menu", chamadas);
        TelaEspia partida = new TelaEspia("partida", chamadas);

        jogo.trocarTela(menu);
        jogo.trocarTela(partida);

        assertEquals(List.of("menu.show", "menu.resize", "menu.hide", "partida.show", "partida.resize",
                "menu.dispose"), chamadas);
        assertSame(partida, jogo.getScreen());
    }

    @Test
    void trocarParaAMesmaTelaNaoALibera() {
        TelaEspia menu = new TelaEspia("menu", chamadas);
        jogo.trocarTela(menu);
        jogo.trocarTela(menu);
        assertEquals(0, chamadas.stream().filter(c -> c.equals("menu.dispose")).count());
    }

    @Test
    void ciclosDoJogoChegamATelaAtualEODisposeALibera() {
        TelaEspia partida = new TelaEspia("partida", chamadas);
        jogo.trocarTela(partida);
        chamadas.clear();

        jogo.render();
        jogo.pause();
        jogo.resume();
        jogo.resize(1024, 768);
        jogo.dispose();

        assertEquals(List.of("partida.render", "partida.pause", "partida.resume", "partida.resize",
                "partida.hide", "partida.dispose"), chamadas);
    }
}
