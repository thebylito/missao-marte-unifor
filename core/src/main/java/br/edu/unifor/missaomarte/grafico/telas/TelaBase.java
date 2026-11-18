package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.Diario;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.Recursos;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    /** Desenha o conteúdo da tela; a câmera já está aplicada e o fundo já foi desenhado. */
    protected abstract void desenhar();

    /** Avança o que depende do tempo (animações, carregamento). Chamado antes de desenhar, a cada quadro. */
    protected void atualizar(float delta) {
    }

    /** As telas mostram o céu de Marte ao fundo, menos a de carregamento (ele ainda não carregou). */
    protected boolean mostrarFundo() {
        return true;
    }

    @Override
    public void show() {
        Diario.registrar("show: " + getClass().getSimpleName());
        Gdx.input.setInputProcessor(new FiltroDeEntrada(entrada()));
    }

    @Override
    public final void render(float delta) {
        atualizar(delta);
        if (jogo.getScreen() != this) {
            return;     // o atualizar trocou de tela (fim do carregamento, fim da partida): esta já foi liberada
        }
        ScreenUtils.clear(FUNDO);
        viewport.apply();
        formas().setProjectionMatrix(camera.combined);
        lote().setProjectionMatrix(camera.combined);
        if (mostrarFundo() && recursosProntos()) {
            lote().begin();
            lote().setColor(0.62f, 0.62f, 0.66f, 1);   // escurece o céu para o texto e os sprites se destacarem
            lote().draw(recursos().quadro(Folha.FUNDO, 0), 0, 0, MissaoMarteGame.LARGURA, MissaoMarteGame.ALTURA);
            lote().setColor(Color.WHITE);
            lote().end();
        }
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
        // As telas não criam recursos próprios: texturas, sons e fontes são do AssetManager (classe Recursos),
        // carregados uma vez e liberados pelo jogo no dispose final. Aqui só fica o registro no log.
        Diario.registrar("dispose: " + getClass().getSimpleName());
    }

    // ---- atalhos para os recursos compartilhados ----

    protected ShapeRenderer formas() { return jogo.getFormas(); }
    protected SpriteBatch lote() { return jogo.getLote(); }
    protected Recursos recursos() { return jogo.getRecursos(); }

    protected boolean recursosProntos() {
        return recursos() != null && recursos().isCarregado();
    }

    /**
     * Fonte já na escala pedida. A escala 1 equivale à fonte padrão da libGDX (a medida usada desde a Fase 3); a
     * fonte do jogo é gerada grande e reduzida, para ficar nítida. Antes do carregamento, usa a fonte padrão.
     */
    private BitmapFont fonte(Recursos.Fonte tipo, float escala) {
        BitmapFont fonte;
        float base;
        if (recursosProntos()) {
            fonte = recursos().fonte(tipo);
            base = recursos().escalaBase(tipo);
        } else {
            fonte = jogo.getFonte();
            base = 1;
        }
        fonte.getData().setScale(escala * base);
        return fonte;
    }

    /** Escreve um texto; deve ser chamado entre lote().begin() e lote().end(). */
    protected void escrever(String texto, float x, float y, Color cor, float escala) {
        BitmapFont fonte = fonte(Recursos.Fonte.TEXTO, escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, x, y);
    }

    /** Texto com largura máxima: quebra em várias linhas. Devolve a altura ocupada. */
    protected float escreverComQuebra(String texto, float x, float y, float largura, Color cor, float escala) {
        BitmapFont fonte = fonte(Recursos.Fonte.TEXTO, escala);
        fonte.setColor(cor);
        return fonte.draw(lote(), texto, x, y, largura, Align.left, true).height;
    }

    protected void escreverCentralizado(String texto, float y, Color cor, float escala) {
        BitmapFont fonte = fonte(Recursos.Fonte.TEXTO, escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, 0, y, MissaoMarteGame.LARGURA, Align.center, false);
    }

    /** Título centralizado, com a fonte em negrito. */
    protected void escreverTitulo(String texto, float y, Color cor, float escala) {
        BitmapFont fonte = fonte(Recursos.Fonte.TITULO, escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, 0, y, MissaoMarteGame.LARGURA, Align.center, false);
    }

    /** Título alinhado à esquerda (painel da partida). */
    protected void escreverTitulo(String texto, float x, float y, Color cor, float escala) {
        BitmapFont fonte = fonte(Recursos.Fonte.TITULO, escala);
        fonte.setColor(cor);
        fonte.draw(lote(), texto, x, y);
    }

    /** Desenha um quadro de sprite com o centro em (cx, cy) e a altura dada, mantendo a proporção. */
    protected void desenharCentralizado(Folha folha, int quadro, float cx, float cy, float altura) {
        TextureRegion r = recursos().quadro(folha, quadro);
        float largura = altura * r.getRegionWidth() / r.getRegionHeight();
        lote().draw(r, cx - largura / 2, cy - altura / 2, largura, altura);
    }
}
