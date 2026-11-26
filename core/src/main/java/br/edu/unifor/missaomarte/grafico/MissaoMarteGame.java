package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.apresentacao.CenaDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoHangar;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoMenu;
import br.edu.unifor.missaomarte.grafico.apresentacao.EnvioDoResultado;
import br.edu.unifor.missaomarte.grafico.apresentacao.FaixaDaHorda;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.InicioDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem;
import br.edu.unifor.missaomarte.grafico.audio.Sons;
import br.edu.unifor.missaomarte.grafico.audio.SonsLibgdx;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Hangar;
import br.edu.unifor.missaomarte.grafico.online.HordaAtual;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.grafico.recursos.Recursos;
import br.edu.unifor.missaomarte.grafico.recursos.RecursosDasNaves;
import br.edu.unifor.missaomarte.grafico.telas.TelaCarregamento;
import br.edu.unifor.missaomarte.grafico.telas.TelaEntrada;
import br.edu.unifor.missaomarte.grafico.telas.TelaFimDePartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaHangar;
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
import java.util.function.Consumer;

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
    private RecursosDasNaves naves;
    private Sons sons = Sons.NENHUM;
    /** Jogador conectado ao servidor (Fase 5); vazio no modo convidado. Guardado também no aparelho (DA-38). */
    private Sessao sessao;
    private Integer saldo;
    // Pedidos de uma sessão antiga não podem mudar a atual. Uma consulta do menu não sobrescreve
    // confirmações que chegaram enquanto ela estava em curso.
    private long geracaoDaSessao;
    private long versaoDoPerfil;
    private InicioDaPartida inicioDaPartida;
    private FaixaDaHorda faixaDaHorda;

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
        naves = new RecursosDasNaves(Gdx.files.internal(""));
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
        if (naves != null) naves.dispose();
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
        if (recursos != null && recursos.isCarregado()) {
            naves.selecionar(getSessao().map(Sessao::skin).orElse(Sessao.SKIN_PADRAO));
        }
        setScreen(nova);
        if (anterior != null && anterior != nova) {
            anterior.dispose();
        }
    }

    // ---- navegação: cada método cria a próxima tela ----

    public void irParaMenu() {
        trocarTela(new TelaMenu(this));
        getSessao().ifPresent(piloto -> {
            long geracao = geracaoDaSessao;
            long versao = versaoDoPerfil;
            servicos.servidor().hangar(piloto, Resposta.de(perfil -> {
                if (versao == versaoDoPerfil) {
                    atualizarPerfil(geracao, perfil);
                }
            }, falha -> verificarSessao(geracao, falha)));
        });
    }

    public void irParaHangar() {
        long geracao = geracaoDaSessao;
        var controlador = new ControladorDoHangar(servicos.servidor(), getSessao(), naves.catalogo(),
                perfil -> atualizarPerfil(geracao, perfil), equipada -> atualizarSkin(geracao, equipada),
                falha -> verificarSessao(geracao, falha));
        trocarTela(new TelaHangar(this, controlador));
    }

    private void atualizarPerfil(long geracao, Hangar perfil) {
        atualizarSaldo(geracao, perfil.saldo());
        if (perfil.equipada() != null) {
            atualizarSkin(geracao, perfil.equipada());
        }
    }

    private void atualizarSaldo(long geracao, int novoSaldo) {
        if (geracao == geracaoDaSessao && sessao != null) {
            saldo = novoSaldo;
            versaoDoPerfil++;
        }
    }

    private void atualizarSkin(long geracao, String equipada) {
        if (geracao == geracaoDaSessao && sessao != null) {
            versaoDoPerfil++;
            if (!sessao.skin().equals(equipada)) {
                sessao = sessao.comSkin(equipada);
                plataforma.sessaoGuardada().guardar(sessao, plataforma.servidor().endereco());
                reexibirMenu();
            }
        }
    }

    private void verificarSessao(long geracao, Falha falha) {
        if (geracao == geracaoDaSessao && falha.motivo() == Falha.Motivo.NAO_AUTORIZADO) {
            sessaoRecusada();
        }
    }

    private void reexibirMenu() {
        if (getScreen() instanceof TelaMenu menu) {
            // Ativa de novo a mesma tela, preservando o controlador. A troca aplica a folha confirmada.
            trocarTela(menu);
        }
    }

    /**
     * Depois do carregamento. DA-38: se o aparelho guardou uma sessão que ainda vale para este servidor, o piloto
     * continua conectado e vai direto ao menu (o Android pode ter matado o jogo em segundo plano). Senão, a entrada.
     */
    public void irParaInicio() {
        Optional<Sessao> guardada = plataforma.sessaoGuardada().recuperar(plataforma.servidor().endereco());
        if (guardada.isEmpty()) {
            irParaEntrada();
            return;
        }
        sessao = guardada.get();
        geracaoDaSessao++;
        Diario.registrar("sessão recuperada do aparelho: " + sessao.nome());   // o nome, nunca o token
        irParaMenu();
    }

    /** Fase 5: entrar, cadastrar ou jogar como convidado. Chegar aqui ("Trocar de piloto") desconecta o piloto. */
    public void irParaEntrada() {
        esquecerSessao();
        trocarTela(new TelaEntrada(this));
    }

    public void entrarComo(Sessao novaSessao) {
        Diario.registrar("conectado ao servidor como " + novaSessao.nome());   // o nome, nunca o token
        sessao = novaSessao;
        geracaoDaSessao++;
        saldo = null;
        plataforma.sessaoGuardada().guardar(novaSessao, plataforma.servidor().endereco());
        irParaMenu();
    }

    /**
     * O servidor recusou o token (vencido, ou assinado com outro segredo): o piloto volta a ser convidado, e a
     * mensagem da tela já pede para entrar de novo. Chamado a cada quadro enquanto a tela mostra a falha, por isso
     * só age uma vez.
     */
    public void sessaoRecusada() {
        if (sessao != null) {
            Diario.registrar("o servidor recusou a sessão de " + sessao.nome() + ": modo convidado até entrar de novo");
            esquecerSessao();
            reexibirMenu();
        }
    }

    private void esquecerSessao() {
        sessao = null;
        saldo = null;
        geracaoDaSessao++;
        plataforma.sessaoGuardada().esquecer();
    }

    public void jogarComoConvidado() {
        Diario.registrar("modo convidado: só o ranking local");
        esquecerSessao();
        irParaMenu();
    }

    public void irParaHistorico() {
        trocarTela(new TelaHistorico(this));
    }

    public void irParaNovaPartida() {
        trocarTela(new TelaNovaPartida(this));
    }

    public void iniciarPartida(FormularioNovaPartida formulario) {
        Modo modo = switch (formulario.getModo()) {
            case CLASSICO -> Modo.CLASSICO;
            case PILOTAGEM -> Modo.PILOTAGEM;
            case HORDA -> throw new IllegalArgumentException("A Horda exige um desafio publicado e um bilhete.");
        };
        inicioDaPartida = InicioDaPartida.pedir(getSessao(), servicos.servidor(), modo, formulario.getDificuldade());
        if (modo == Modo.PILOTAGEM) {
            Diario.registrar("pilotagem: " + formulario.getDificuldade());
            PartidaDePilotagem voo = PartidaDePilotagem.nova(formulario.getPiloto(), formulario.getDificuldade());
            abrirPilotagem(new ControladorDaPilotagem(voo, servicos.rankingDaPilotagem()));
            return;
        }
        Partida partida = servicos.novaPartida(formulario.getPiloto(), formulario.getDificuldade(),
                formulario.getTamanho());
        ControladorDaPartida controlador = new ControladorDaPartida(partida, servicos.ranking());
        trocarTela(new TelaPartida(this, new CenaDaPartida(controlador, sons)));
    }

    private void abrirPilotagem(ControladorDaPilotagem controle) {
        var canal = new SombrasDaPilotagem(getSessao(), servicos.servidor().conexaoDeSombras(), controle.sala());
        trocarTela(new TelaPilotagem(this, controle, canal));
    }

    public void iniciarHorda(HordaAtual atual, Consumer<String> aoFalhar) {
        if (sessao == null) {
            aoFalhar.accept(ControladorDoMenu.CONVITE_PARA_ENTRAR);
            return;
        }
        Screen origem = getScreen();
        long geracao = geracaoDaSessao;
        InicioDaPartida.pedirHorda(sessao, servicos.servidor(), atual.descricao(), Resposta.de(controle -> {
            if (getScreen() == origem && geracao == geracaoDaSessao) {
                // O controlador já carrega o bilhete obrigatório junto do número da horda.
                inicioDaPartida = InicioDaPartida.semBilhete(getSessao(), servicos.servidor());
                abrirPilotagem(controle);
            }
        }, falha -> {
            if (getScreen() == origem && geracao == geracaoDaSessao) {
                aoFalhar.accept("Horda não iniciada. " + falha.mensagem());
                verificarSessao(geracao, falha);
            }
        }));
    }

    public EnvioDoResultado enviarResultado(FimDePartida fim) {
        InicioDaPartida inicio = inicioDaPartida == null
                ? InicioDaPartida.semBilhete(getSessao(), servicos.servidor()) : inicioDaPartida;
        inicioDaPartida = null;
        long geracao = geracaoDaSessao;
        return inicio.enviar(fim.paraOServidor(), registro -> atualizarSaldo(geracao, registro.saldo()),
                falha -> verificarSessao(geracao, falha));
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
    public FaixaDaHorda getFaixaDaHorda() {
        if (faixaDaHorda == null) {
            faixaDaHorda = new FaixaDaHorda(servicos.servidor());
        }
        return faixaDaHorda;
    }
    public Plataforma getPlataforma() { return plataforma; }
    public ShapeRenderer getFormas() { return formas; }
    public SpriteBatch getLote() { return lote; }
    /** Fonte padrão da libGDX, usada antes de as fontes do jogo carregarem. */
    public BitmapFont getFonte() { return fonte; }
    public Recursos getRecursos() { return recursos; }
    public RecursosDasNaves getNaves() { return naves; }
    public Integer getSaldo() { return saldo; }
    public Sons getSons() { return sons; }
    public Optional<Sessao> getSessao() { return Optional.ofNullable(sessao); }
}
