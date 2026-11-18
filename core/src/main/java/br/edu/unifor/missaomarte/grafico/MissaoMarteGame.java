package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaFimDePartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaMenu;
import br.edu.unifor.missaomarte.grafico.telas.TelaNovaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaRanking;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Classe principal do cliente libGDX. A libGDX chama os métodos do ciclo de vida
 * ({@code create}, {@code render}, {@code resize}, {@code pause}, {@code resume}, {@code dispose});
 * {@link Game} repassa cada um à tela atual.
 *
 * <p>Esta classe cuida de duas coisas: dos recursos de desenho compartilhados por todas as telas (criados no
 * {@code create}, quando já existe contexto OpenGL, e liberados no {@code dispose}) e da navegação entre
 * telas. As regras do jogo ficam no núcleo, acessado por {@link ServicosDoJogo}.
 */
public class MissaoMarteGame extends Game {

    /** Tamanho do "mundo" em unidades de tela; o viewport ajusta isso a qualquer tamanho de janela. */
    public static final float LARGURA = 800;
    public static final float ALTURA = 600;

    private final ServicosDoJogo servicos;
    private ShapeRenderer formas;
    private SpriteBatch lote;
    private BitmapFont fonte;

    public MissaoMarteGame(ServicosDoJogo servicos) {
        this.servicos = servicos;
    }

    @Override
    public void create() {
        Diario.registrar("create: janela e contexto OpenGL prontos; criando os recursos compartilhados");
        formas = new ShapeRenderer();
        lote = new SpriteBatch();
        fonte = new BitmapFont();   // fonte padrão embutida na libGDX (Latin-1, sem emojis)
        irParaMenu();
    }

    @Override
    public void resize(int largura, int altura) {
        Diario.registrar("resize: " + largura + "x" + altura);
        super.resize(largura, altura);
    }

    @Override
    public void pause() {
        Diario.registrar("pause");
        super.pause();
    }

    @Override
    public void resume() {
        Diario.registrar("resume");
        super.resume();
    }

    @Override
    public void dispose() {
        Diario.registrar("dispose: liberando a tela atual e os recursos compartilhados");
        super.dispose();             // Game.dispose() só chama hide() na tela atual...
        Screen atual = getScreen();
        if (atual != null) {
            atual.dispose();         // ...então a tela é liberada aqui
        }
        if (formas != null) formas.dispose();
        if (lote != null) lote.dispose();
        if (fonte != null) fonte.dispose();
    }

    /**
     * Troca a tela e libera a anterior. {@link Game#setScreen} chama {@code hide()} na tela antiga e
     * {@code show()} na nova, mas nunca {@code dispose()}: sem isto, cada troca deixaria a tela antiga
     * (e, a partir da Fase 4, as texturas e sons dela) ocupando memória.
     */
    public void trocarTela(Screen nova) {
        Screen anterior = getScreen();
        setScreen(nova);
        if (anterior != null && anterior != nova) {
            anterior.dispose();
        }
    }

    // ---- navegação: cada método cria a próxima tela ----

    public void irParaMenu() {
        trocarTela(new TelaMenu(this));
    }

    public void irParaNovaPartida() {
        trocarTela(new TelaNovaPartida(this));
    }

    public void iniciarPartida(FormularioNovaPartida formulario) {
        Partida partida = servicos.novaPartida(formulario.getPiloto(), formulario.getDificuldade(),
                formulario.getTamanho());
        trocarTela(new TelaPartida(this, new ControladorDaPartida(partida, servicos.ranking())));
    }

    public void irParaFimDePartida(FimDePartida fim) {
        trocarTela(new TelaFimDePartida(this, fim));
    }

    public void irParaRanking() {
        trocarTela(new TelaRanking(this, servicos.ranking().carregar()));
    }

    public void sair() {
        Diario.registrar("saindo: Gdx.app.exit() pede à libGDX que encerre o laço e chame dispose()");
        Gdx.app.exit();
    }

    // ---- recursos compartilhados (válidos entre create e dispose) ----

    public ServicosDoJogo getServicos() { return servicos; }
    public ShapeRenderer getFormas() { return formas; }
    public SpriteBatch getLote() { return lote; }
    public BitmapFont getFonte() { return fonte; }
}
