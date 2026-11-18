package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.Diario;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.Recursos;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.List;
import java.util.function.Consumer;

/**
 * Esqueleto comum das telas (padrão Template Method). O {@link #render(float)} é sempre o mesmo: limpa
 * a tela, aplica a câmera e chama {@link #desenhar()}, que cada tela implementa. A entrada fica no
 * {@link InputAdapter} que cada tela devolve em {@link #entrada()}.
 *
 * <p>Câmera e viewport (Fase 6): as telas desenham numa área de 800x600 que fica sempre visível e centralizada. O
 * {@link ExtendViewport} estende o mundo na direção que sobra (os lados num celular deitado, em cima e embaixo num
 * celular em pé), em vez de deixar faixas pretas. Numa janela de 800x600 o resultado é idêntico ao das fases
 * anteriores. {@link #visivel()} diz qual é a área visível de verdade, para quem quiser usá-la (a partida).
 *
 * <p>Toque (Fase 6): cada tela pode declarar {@link #alvosDeToque()}. Um toque (ou clique) dentro de um alvo executa
 * a ação dele, quase sempre "apertar a mesma tecla do teclado": o toque reaproveita todo o caminho do teclado.
 */
public abstract class TelaBase implements Screen {

    protected static final Color FUNDO = new Color(0.05f, 0.04f, 0.10f, 1f);
    protected static final float MARGEM = 20;
    private static final Color COR_DO_BOTAO = new Color(0.16f, 0.12f, 0.16f, 0.85f);
    private static final Color BORDA_DO_BOTAO = new Color(0.95f, 0.6f, 0.2f, 0.9f);

    /** Uma área tocável e o que acontece ao tocá-la. */
    protected record AlvoDeToque(Retangulo area, Runnable acao) {
    }

    protected final MissaoMarteGame jogo;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(MissaoMarteGame.LARGURA, MissaoMarteGame.ALTURA, camera);
    private final Vector2 toque = new Vector2();
    private InputAdapter entradaAtual;

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

    /** Áreas tocáveis desta tela (vazio: a tela só usa o teclado). Consultado a cada toque. */
    protected List<AlvoDeToque> alvosDeToque() {
        return List.of();
    }

    /** O botão Voltar do Android funciona como Esc. A partida desliga isso: um toque acidental não aborta a missão. */
    protected boolean voltarFuncionaComoEsc() {
        return true;
    }

    @Override
    public void show() {
        Diario.registrar("show: " + getClass().getSimpleName());
        entradaAtual = entrada();
        Gdx.input.setInputProcessor(new FiltroDeEntrada(new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                if (tecla == Keys.BACK) {
                    return !voltarFuncionaComoEsc() || entradaAtual.keyDown(Keys.ESCAPE);
                }
                return entradaAtual.keyDown(tecla);
            }

            @Override
            public boolean keyTyped(char caractere) {
                return entradaAtual.keyTyped(caractere);
            }

            @Override
            public boolean keyUp(int tecla) {
                return entradaAtual.keyUp(tecla);
            }

            @Override
            public boolean touchDown(int x, int y, int dedo, int botao) {
                return tocar(x, y);
            }
        }));
    }

    /** Converte o toque (pixels da tela) para o mundo e executa o primeiro alvo que o contém. */
    private boolean tocar(int xNaTela, int yNaTela) {
        viewport.unproject(toque.set(xNaTela, yNaTela));
        for (AlvoDeToque alvo : alvosDeToque()) {
            if (alvo.area().contem(toque.x, toque.y)) {
                alvo.acao().run();
                return true;
            }
        }
        return entradaAtual.touchDown(xNaTela, yNaTela, 0, 0);
    }

    /** Um alvo que "aperta" uma tecla: keyDown, o caractere (se houver) e keyUp, como o teclado faria. */
    protected AlvoDeToque tecla(Retangulo area, int tecla, char caractere) {
        return new AlvoDeToque(area, () -> {
            entradaAtual.keyDown(tecla);
            if (caractere != 0) {
                entradaAtual.keyTyped(caractere);
            }
            entradaAtual.keyUp(tecla);
        });
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
            desenharFundo();
        }
        desenhar();
    }

    /** O céu cobre toda a área visível, sem distorcer (corta as bordas que sobrarem). */
    private void desenharFundo() {
        Retangulo v = visivel();
        TextureRegion ceu = recursos().quadro(Folha.FUNDO, 0);
        float escala = Math.max(v.largura() / ceu.getRegionWidth(), v.altura() / ceu.getRegionHeight());
        float w = ceu.getRegionWidth() * escala;
        float h = ceu.getRegionHeight() * escala;
        lote().begin();
        lote().setColor(0.62f, 0.62f, 0.66f, 1);   // escurece o céu para o texto e os sprites se destacarem
        lote().draw(ceu, v.centroX() - w / 2, v.centroY() - h / 2, w, h);
        lote().setColor(Color.WHITE);
        lote().end();
    }

    @Override
    public void resize(int largura, int altura) {
        viewport.update(largura, altura, false);
        // a área de 800x600 fica sempre no centro: o espaço extra aparece igualmente dos dois lados
        camera.position.set(MissaoMarteGame.LARGURA / 2, MissaoMarteGame.ALTURA / 2, 0);
        camera.update();
    }

    /** A área do mundo que aparece na tela agora (pelo menos 800x600, centrada em 400,300). */
    protected Retangulo visivel() {
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        return new Retangulo(MissaoMarteGame.LARGURA / 2 - w / 2, MissaoMarteGame.ALTURA / 2 - h / 2, w, h);
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

    // ---- Fase 6: toque ----

    /** Modo toque: mostra os botões na tela (Android sempre; desktop com -Dmissaomarte.toque=true). */
    protected boolean toque() {
        return jogo.getPlataforma().toque();
    }

    /**
     * Pede um texto ao jogador. No Android, abre a caixa de texto do sistema (com o teclado virtual; senha
     * mascarada); a resposta chega na thread do jogo. No desktop o teclado físico já digita na tela, então não faz
     * nada aqui (devolve false).
     */
    protected boolean pedirTexto(String titulo, String atual, boolean senha, Consumer<String> aoConfirmar) {
        return pedirTexto(titulo, atual, "", senha, aoConfirmar);
    }

    /** Com uma dica (o texto cinza que some ao digitar), para mostrar o valor atual sem precisar apagá-lo. */
    protected boolean pedirTexto(String titulo, String atual, String dica, boolean senha, Consumer<String> aoConfirmar) {
        if (Gdx.app == null || Gdx.app.getType() != Application.ApplicationType.Android) {
            return false;
        }
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override
            public void input(String texto) {
                // sem isto o teclado virtual continua aberto depois do OK e cobre metade do jogo (visto no emulador)
                Gdx.input.setOnscreenKeyboardVisible(false);
                aoConfirmar.accept(texto);
            }

            @Override
            public void canceled() {
                Gdx.input.setOnscreenKeyboardVisible(false);
            }
        }, titulo, atual, dica, senha ? Input.OnscreenKeyboardType.Password : Input.OnscreenKeyboardType.Default);
        return true;
    }

    /** Desenha botões (retângulo + rótulo) fora de begin/end. Usado só no modo toque. */
    protected void desenharBotoes(List<Retangulo> areas, List<String> rotulos, float escalaDoTexto) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas().begin(ShapeType.Filled);
        formas().setColor(COR_DO_BOTAO);
        for (Retangulo r : areas) {
            formas().rect(r.x(), r.y(), r.largura(), r.altura());
        }
        formas().end();
        formas().begin(ShapeType.Line);
        formas().setColor(BORDA_DO_BOTAO);
        for (Retangulo r : areas) {
            formas().rect(r.x(), r.y(), r.largura(), r.altura());
        }
        formas().end();
        lote().begin();
        for (int i = 0; i < areas.size(); i++) {
            Retangulo r = areas.get(i);
            BitmapFont fonte = fonte(Recursos.Fonte.TITULO, escalaDoTexto);
            fonte.setColor(Color.WHITE);
            fonte.draw(lote(), rotulos.get(i), r.x(), r.centroY() + fonte.getCapHeight() / 2, r.largura(), Align.center,
                    false);
        }
        lote().end();
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

    /** Faixa horizontal tocável em volta de uma linha de texto (para opções de menu). */
    protected static Retangulo linha(float x, float yDoTexto, float largura, float altura) {
        return new Retangulo(x, yDoTexto - altura + 8, largura, altura);
    }
}
