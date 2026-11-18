package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.Diario;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Esqueleto comum das telas (padrão Template Method). O {@link #render(float)} é sempre o mesmo: limpa
 * a tela, aplica a câmera e chama {@link #desenhar()}, que cada tela implementa. A entrada fica no
 * {@link InputAdapter} que cada tela devolve em {@link #entrada()}.
 *
 * <p>Câmera e viewport: as telas desenham num "mundo" fixo de 800x600. O {@link FitViewport} escala esse
 * mundo para o tamanho real da janela, mantendo a proporção (sobram faixas pretas se a proporção mudar).
 */
public abstract class TelaBase implements Screen {

    protected static final Color FUNDO = new Color(0.05f, 0.04f, 0.10f, 1f);
    protected static final float MARGEM = 20;

    protected final MissaoMarteGame jogo;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(MissaoMarteGame.LARGURA, MissaoMarteGame.ALTURA, camera);

    protected TelaBase(MissaoMarteGame jogo) {
        this.jogo = jogo;
    }

    /** Quem trata o teclado enquanto esta tela está ativa. */
    protected abstract InputAdapter entrada();

    /** Desenha o conteúdo da tela; a câmera já está aplicada. */
    protected abstract void desenhar();

    @Override
    public void show() {
        Diario.registrar("show: " + getClass().getSimpleName());
        Gdx.input.setInputProcessor(new FiltroDeEntrada(entrada()));
    }

    @Override
    public final void render(float delta) {
        ScreenUtils.clear(FUNDO);
        viewport.apply();
        formas().setProjectionMatrix(camera.combined);
        lote().setProjectionMatrix(camera.combined);
        desenhar();
    }

    @Override
    public void resize(int largura, int altura) {
        viewport.update(largura, altura, true);   // true: centraliza a câmera no mundo
    }

    @Override
    public void pause() {
        // jogo por turnos: nada anda sozinho, então não há o que pausar
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Diario.registrar("hide: " + getClass().getSimpleName());
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        // As telas da Fase 3 não criam recursos próprios: usam os do jogo. A partir da Fase 4,
        // texturas e sons de cada tela serão liberados aqui.
        Diario.registrar("dispose: " + getClass().getSimpleName());
    }

    // ---- atalhos para os recursos compartilhados ----

    protected ShapeRenderer formas() { return jogo.getFormas(); }
    protected SpriteBatch lote() { return jogo.getLote(); }
    protected BitmapFont fonte() { return jogo.getFonte(); }

    /** Escreve um texto; deve ser chamado entre lote().begin() e lote().end(). */
    protected void escrever(String texto, float x, float y, Color cor, float escala) {
        BitmapFont fonte = fonte();
        fonte.getData().setScale(escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, x, y);
    }

    /** Texto com largura máxima: quebra em várias linhas. Devolve a altura ocupada. */
    protected float escreverComQuebra(String texto, float x, float y, float largura, Color cor, float escala) {
        BitmapFont fonte = fonte();
        fonte.getData().setScale(escala);
        fonte.setColor(cor);
        return fonte.draw(lote(), texto, x, y, largura, Align.left, true).height;
    }

    protected void escreverCentralizado(String texto, float y, Color cor, float escala) {
        BitmapFont fonte = fonte();
        fonte.getData().setScale(escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, 0, y, MissaoMarteGame.LARGURA, Align.center, false);
    }
}
