package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.apresentacao.CenaDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import br.edu.unifor.missaomarte.grafico.audio.Sons;
import br.edu.unifor.missaomarte.grafico.audio.SonsLibgdx;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.grafico.recursos.Recursos;
import br.edu.unifor.missaomarte.grafico.telas.TelaCarregamento;
import br.edu.unifor.missaomarte.grafico.telas.TelaEntrada;
import br.edu.unifor.missaomarte.grafico.telas.TelaFimDePartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaHistorico;
import br.edu.unifor.missaomarte.grafico.telas.TelaMenu;
import br.edu.unifor.missaomarte.grafico.telas.TelaNovaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaPilotagem;
import br.edu.unifor.missaomarte.grafico.telas.TelaRanking;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.Optional;

/**
 * Classe principal do cliente libGDX. A libGDX chama os métodos do ciclo de vida
 * ({@code create}, {@code render}, {@code resize}, {@code pause}, {@code resume}, {@code dispose});
 * {@link Game} repassa cada um à tela atual.
 *
 * <p>Esta classe cuida de duas coisas: dos recursos compartilhados por todas as telas (criados no
 * {@code create}, quando já existe contexto OpenGL, e liberados no {@code dispose}) e da navegação entre
 * telas. As regras do jogo ficam no núcleo, acessado por {@link ServicosDoJogo}.
 *
 * <p>Fase 4: os assets (sprites, sons e fontes) são carregados pelo {@link Recursos} na {@link TelaCarregamento},
 * a primeira tela, e liberados todos juntos no {@code dispose}.
 */
public class MissaoMarteGame extends Game {

    /** Tamanho do "mundo" em unidades de tela; o viewport ajusta isso a qualquer tamanho de janela. */
    public static final float LARGURA = 800;
    public static final float ALTURA = 600;

    private final ServicosDoJogo servicos;
    private final Plataforma plataforma;
    private ShapeRenderer formas;
    private SpriteBatch lote;
    private BitmapFont fonte;
    private Recursos recursos;
    private Sons sons = Sons.NENHUM;
    /** Jogador conectado ao servidor (Fase 5); vazio no modo convidado. Só na memória. */
    private Sessao sessao;

    public MissaoMarteGame(ServicosDoJogo servicos) {
        this(servicos, Plataforma.padrao());
    }

    /** Fase 6: cada launcher diz se a plataforma é de toque e onde está o servidor. */
    public MissaoMarteGame(ServicosDoJogo servicos, Plataforma plataforma) {
        this.servicos = servicos;
        this.plataforma = plataforma;
    }

    @Override
    public void create() {
        Diario.registrar("create: janela e contexto OpenGL prontos; criando os recursos compartilhados");
        Gdx.input.setCatchKey(Keys.BACK, true);   // Android: o Voltar vira Esc em cada tela, em vez de fechar o app
        formas = new ShapeRenderer();
        lote = new SpriteBatch();
        fonte = new BitmapFont();   // fonte padrão da libGDX: só a tela de carregamento a usa
        recursos = new Recursos();
        recursos.enfileirarTudo();
        sons = new SonsLibgdx(recursos);
        trocarTela(new TelaCarregamento(this));
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
        if (recursos != null) {
            int antes = recursos.quantidadeCarregada();
            recursos.dispose();
            Diario.registrar("AssetManager: " + antes + " assets liberados; restam " + recursos.quantidadeCarregada());
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

    /** Fase 5: a primeira tela depois do carregamento. Entrar, cadastrar ou jogar como convidado. */
    public void irParaEntrada() {
        sessao = null;
        trocarTela(new TelaEntrada(this));
    }

    public void entrarComo(Sessao novaSessao) {
        Diario.registrar("conectado ao servidor como " + novaSessao.nome());   // o nome, nunca o token
        sessao = novaSessao;
        irParaMenu();
    }

    public void jogarComoConvidado() {
        Diario.registrar("modo convidado: só o ranking local");
        sessao = null;
        irParaMenu();
    }

    public void irParaHistorico() {
        trocarTela(new TelaHistorico(this));
    }

    public void irParaNovaPartida() {
        trocarTela(new TelaNovaPartida(this));
    }

    public void iniciarPartida(FormularioNovaPartida formulario) {
        if (formulario.getModo() == Modo.PILOTAGEM) {   // Fase 7
            Diario.registrar("pilotagem: " + formulario.getDificuldade());
            PartidaDePilotagem voo = PartidaDePilotagem.nova(formulario.getPiloto(), formulario.getDificuldade());
            trocarTela(new TelaPilotagem(this, new ControladorDaPilotagem(voo, servicos.rankingDaPilotagem())));
            return;
        }
        Partida partida = servicos.novaPartida(formulario.getPiloto(), formulario.getDificuldade(),
                formulario.getTamanho());
        ControladorDaPartida controlador = new ControladorDaPartida(partida, servicos.ranking());
        trocarTela(new TelaPartida(this, new CenaDaPartida(controlador, sons)));
    }

    public void irParaFimDePartida(FimDePartida fim) {
        trocarTela(new TelaFimDePartida(this, fim));
    }

    public void irParaRanking() {
        trocarTela(new TelaRanking(this, new ControladorDoRanking(servicos.ranking().carregar(),
                servicos.rankingDaPilotagem().carregar(), servicos.servidor())));
    }

    public void sair() {
        Diario.registrar("saindo: Gdx.app.exit() pede à libGDX que encerre o laço e chame dispose()");
        Gdx.app.exit();
    }

    // ---- recursos compartilhados (válidos entre create e dispose) ----

    public ServicosDoJogo getServicos() { return servicos; }
    public Plataforma getPlataforma() { return plataforma; }
    public ShapeRenderer getFormas() { return formas; }
    public SpriteBatch getLote() { return lote; }
    /** Fonte padrão da libGDX, usada antes de as fontes do jogo carregarem. */
    public BitmapFont getFonte() { return fonte; }
    public Recursos getRecursos() { return recursos; }
    public Sons getSons() { return sons; }
    public Optional<Sessao> getSessao() { return Optional.ofNullable(sessao); }
}
