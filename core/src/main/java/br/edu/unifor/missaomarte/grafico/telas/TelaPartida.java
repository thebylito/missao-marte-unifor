package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.AnimacaoDoTurno;
import br.edu.unifor.missaomarte.grafico.apresentacao.CenaDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.EfeitosDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.GeometriaDoMapa;
import br.edu.unifor.missaomarte.grafico.apresentacao.GeometriaDoMapa.Quadrado;
import br.edu.unifor.missaomarte.grafico.apresentacao.Hud;
import br.edu.unifor.missaomarte.grafico.apresentacao.Botao;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.FolhaDeSprites;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tela de jogo (Fase 4): mapa com sprites, deslize entre células, explosões, tremor e HUD. Não decide nada: lê a
 * {@link CenaDaPartida} (animação, efeitos, fim) e a Partida (estado), e desenha. Duas passadas por quadro: formas
 * (fundos translúcidos, células e barras) e depois sprites e textos.
 */
public class TelaPartida extends TelaBase {

    private static final Color COR_SUCESSO = new Color(0.45f, 0.95f, 0.45f, 1);
    private static final Color COR_ALERTA = new Color(1f, 0.85f, 0.3f, 1);
    private static final Color COR_PERIGO = new Color(1f, 0.38f, 0.3f, 1);
    private static final Color COR_ASSENTO_OCUPADO = new Color(0.25f, 0.45f, 0.55f, 0.9f);
    private static final Color COR_ASSENTO_LIVRE = new Color(0.16f, 0.13f, 0.15f, 0.9f);
    private static final Color COR_VIDA_PERDIDA = new Color(0.25f, 0.2f, 0.22f, 0.8f);

    private final CenaDaPartida cena;
    /** Fase 6: recalculados a cada quadro a partir da área visível (girar o celular muda tudo). */
    private LayoutDaPartida layout;
    private GeometriaDoMapa geometria;

    public TelaPartida(MissaoMarteGame jogo, CenaDaPartida cena) {
        super(jogo);
        this.cena = cena;
        posicionar();
    }

    private void posicionar() {
        layout = LayoutDaPartida.calcular(visivel(), toque());
        Retangulo m = layout.mapa();
        geometria = new GeometriaDoMapa(missao().getMapa(), m.x(), m.y(), m.largura());
    }

    /** O botão Voltar do Android não aborta a partida (use o botão Abortar ou Esc). */
    @Override
    protected boolean voltarFuncionaComoEsc() {
        return false;
    }

    /** Fase 6: cada botão de toque aperta a tecla equivalente; o resto é o mesmo caminho do teclado. */
    @Override
    protected List<AlvoDeToque> alvosDeToque() {
        posicionar();
        List<AlvoDeToque> alvos = new ArrayList<>();
        for (Map.Entry<Botao, Retangulo> b : layout.botoes().entrySet()) {
            alvos.add(tecla(b.getValue(), b.getKey().getTecla(), (char) 0));
        }
        return alvos;
    }

    private Missao missao() {
        return cena.getControlador().getPartida().getMissao();
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                cena.teclaPressionada(tecla);
                return true;
            }
        };
    }

    @Override
    protected void atualizar(float delta) {
        cena.avancar(delta);
        cena.fimParaMostrar().ifPresent(jogo::irParaFimDePartida);
    }

    @Override
    protected void desenhar() {
        EfeitosDaPartida efeitos = cena.getEfeitos();
        float dx = efeitos.tremorX();
        float dy = efeitos.tremorY();
        Hud hud = Hud.de(cena.getControlador().getPartida());
        posicionar();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas().begin(ShapeType.Filled);
        desenharTabuleiro(dx, dy);
        desenharFundoDoPainel(hud);
        formas().end();

        lote().begin();
        desenharSprites(dx, dy);
        desenharPainel(hud);
        lote().end();
        desenharControles();
    }

    // ------------------------------------------------------------------ mapa

    private void desenharTabuleiro(float dx, float dy) {
        float lado = geometria.getLadoDaCelula();
        formas().setColor(0.05f, 0.03f, 0.04f, 0.55f);
        Retangulo m = layout.mapa();
        formas().rect(m.x() - 4 + dx, m.y() - 4 + dy, m.largura() + 8, m.altura() + 8);
        Posicao plataforma = missao().getMapa().getPlataforma();
        for (int y = missao().getMapa().getMin(); y <= missao().getMapa().getMax(); y++) {
            for (int x = missao().getMapa().getMin(); x <= missao().getMapa().getMax(); x++) {
                Quadrado q = geometria.celula(x, (float) y);
                boolean ePlataforma = plataforma.x() == x && plataforma.y() == y;
                if (ePlataforma) {
                    formas().setColor(0.2f, 0.75f, 0.35f, 0.35f);
                } else {
                    formas().setColor(0.42f, 0.22f, 0.16f, (x + y) % 2 == 0 ? 0.30f : 0.22f);   // solo marciano
                }
                formas().rect(q.x() + 1 + dx, q.y() + 1 + dy, lado - 2, lado - 2);
            }
        }
    }

    private void desenharSprites(float dx, float dy) {
        float lado = geometria.getLadoDaCelula();
        float t = cena.getRelogio();
        Missao missao = missao();
        AnimacaoDoTurno animacao = cena.getAnimacao();

        // plataforma de pouso, deitada na parte de baixo da célula (0,0)
        Quadrado l = geometria.celula(missao.getMapa().getPlataforma());
        desenharCentralizado(Folha.PLATAFORMA, 0, l.centroX() + dx, l.y() + lado * 0.2f + dy, lado * 0.24f);

        int i = 0;
        for (Asteroide a : missao.getAsteroides()) {
            Quadrado q = geometria.celula(a.getPosicao());
            int quadro = FolhaDeSprites.quadroEmLaco(Folha.ASTEROIDE, t + i++ * 0.37f, 12);
            desenharCentralizado(Folha.ASTEROIDE, quadro, q.centroX() + dx, q.centroY() + dy, lado * 0.95f);
        }

        i = 0;
        for (Passageiro p : animacao.passageirosNoMapa()) {
            Quadrado q = geometria.celula(p.getPosicao());
            int quadro = FolhaDeSprites.quadroEmLaco(folhaDo(p.getTipo()), t + i++ * 0.29f, 8);
            desenharCentralizado(folhaDo(p.getTipo()), quadro, q.centroX() + dx, q.centroY() + dy, lado * 0.82f);
        }

        // passageiros embarcando: vão até a nave, encolhendo e sumindo
        AnimacaoDoTurno.Ponto nave = animacao.nave();
        Quadrado qNave = geometria.celula(nave.x(), nave.y());
        for (AnimacaoDoTurno.Embarque e : animacao.embarques()) {
            Quadrado q = geometria.celula(e.passageiro().getPosicao());
            float k = e.progresso();
            float cx = q.centroX() + (qNave.centroX() - q.centroX()) * k;
            float cy = q.centroY() + (qNave.centroY() - q.centroY()) * k;
            lote().setColor(1, 1, 1, 1 - k);
            desenharCentralizado(folhaDo(e.passageiro().getTipo()), 0, cx + dx, cy + dy, lado * 0.82f * (1 - 0.6f * k));
            lote().setColor(Color.WHITE);
        }

        i = 0;
        for (AnimacaoDoTurno.Ponto p : animacao.inimigos()) {
            Quadrado q = geometria.celula(p.x(), p.y());
            int quadro = FolhaDeSprites.quadroEmLaco(Folha.INIMIGO, t + i++ * 0.21f, 10);
            desenharCentralizado(Folha.INIMIGO, quadro, q.centroX() + dx, q.centroY() + dy, lado * 0.92f);
        }

        if (!cena.getEfeitos().isNaveDestruida()) {
            int quadro = FolhaDeSprites.quadroDaNave(cena.getDirecaoDaNave());
            desenharCentralizado(Folha.NAVE, quadro, qNave.centroX() + dx, qNave.centroY() + dy, lado * 1.25f);
        }

        for (EfeitosDaPartida.ExplosaoVisivel e : cena.getEfeitos().explosoes()) {
            Quadrado q = geometria.celula(e.celula());
            desenharCentralizado(Folha.EXPLOSAO, e.quadro(), q.centroX() + dx, q.centroY() + dy, lado * e.tamanho());
        }
    }

    private static Folha folhaDo(TipoPassageiro tipo) {
        switch (tipo) {
            case ASTRONAUTA: return Folha.ASTRONAUTA;
            case ENGENHEIRO: return Folha.ENGENHEIRO;
            default: return Folha.PROFESSOR;
        }
    }

    // ------------------------------------------------------------------ HUD

    private static final float LADO_DO_ASSENTO = 30;
    private static final Color COR_DO_DIRECIONAL = new Color(0.95f, 0.75f, 0.4f, 0.95f);

    /**
     * Posições do painel. Em paisagem ele é uma coluna estreita (as mesmas posições das Fases 4 e 5); em retrato é uma
     * faixa larga dividida em duas colunas: identidade, vidas e pontos à esquerda; passageiros e mensagens à direita.
     */
    private record Painel(float x, float topo, float largura, float xDireita, float topoDireita, float larguraDaColuna,
                          float yCombustivel, float yAssentos) {

        static Painel de(Retangulo p, boolean largo) {
            if (!largo) {
                return new Painel(p.x(), p.topo(), p.largura(), p.x(), p.topo() - 198, p.largura(),
                        p.topo() - 185, p.topo() - 250);
            }
            float coluna = p.largura() / 2 - 16;
            return new Painel(p.x() + 8, p.topo(), coluna, p.centroX() + 10, p.topo() - 8, coluna,
                    p.topo() - 175, p.topo() - 78);
        }
    }

    private void desenharFundoDoPainel(Hud hud) {
        Retangulo p = layout.painel();
        Painel pos = Painel.de(p, layout.retrato());
        formas().setColor(0.05f, 0.03f, 0.05f, 0.62f);
        formas().rect(p.x() - 8, p.y() - 4, p.largura() + 12, p.altura() + 8);
        // barra de combustível (pontos): vermelha quando acaba, verde quando cheia
        float larguraDaBarra = pos.largura() - 8;
        formas().setColor(0.18f, 0.14f, 0.16f, 1);
        formas().rect(pos.x(), pos.yCombustivel(), larguraDaBarra, 12);
        float c = hud.combustivel();
        formas().setColor(1 - c, 0.25f + 0.65f * c, 0.2f, 1);
        formas().rect(pos.x(), pos.yCombustivel(), larguraDaBarra * c, 12);
        // assentos da nave
        for (int i = 0; i < hud.capacidade(); i++) {
            formas().setColor(i < hud.aBordo().size() ? COR_ASSENTO_OCUPADO : COR_ASSENTO_LIVRE);
            formas().rect(pos.xDireita() + i * (LADO_DO_ASSENTO + 3), pos.yAssentos(), LADO_DO_ASSENTO, LADO_DO_ASSENTO);
        }
    }

    private void desenharPainel(Hud hud) {
        Painel pos = Painel.de(layout.painel(), layout.retrato());
        float x = pos.x();
        float y = pos.topo() - 8;
        escreverTitulo("MISSÃO MARTE", x, y, Color.ORANGE, 1.25f);
        y -= 34;
        escrever(hud.piloto(), x, y, Color.WHITE, 1f);
        y -= 22;
        escrever(hud.dificuldade(), x, y, Color.LIGHT_GRAY, 0.9f);

        // vidas: uma nave por vida; as perdidas ficam apagadas
        y -= 40;
        escrever("Vidas", x, y + 16, Color.LIGHT_GRAY, 0.85f);
        for (int i = 0; i < hud.vidasIniciais(); i++) {
            lote().setColor(i < hud.vidas() ? Color.WHITE : COR_VIDA_PERDIDA);
            desenharCentralizado(Folha.NAVE, 0, x + 70 + i * 34, y + 10, 40);
        }
        lote().setColor(Color.WHITE);

        escrever("Pontos: " + hud.pontos(), x, pos.yCombustivel() + 32, Color.WHITE, 1f);

        float xd = pos.xDireita();
        escrever("A bordo " + hud.aBordo().size() + "/" + hud.capacidade(), xd, pos.yAssentos() + 52, Color.WHITE, 0.95f);
        for (int i = 0; i < hud.aBordo().size(); i++) {
            float cx = xd + i * (LADO_DO_ASSENTO + 3) + LADO_DO_ASSENTO / 2;
            desenharCentralizado(folhaDo(hud.aBordo().get(i)), 0, cx, pos.yAssentos() + LADO_DO_ASSENTO / 2,
                    LADO_DO_ASSENTO - 2);
        }

        y = pos.yAssentos() - 26;
        escrever("Restantes no mapa: " + hud.restantes(), xd, y, Color.LIGHT_GRAY, 0.85f);
        y -= 20;
        escrever("Movimentos: " + hud.movimentos(), xd, y, Color.LIGHT_GRAY, 0.85f);

        y -= 30;
        List<Evento> eventos = cena.getControlador().getUltimosEventos();
        for (Evento e : eventos) {
            y -= escreverComQuebra(TextosDaInterface.evento(e), xd, y, pos.larguraDaColuna() - 6, cor(Hud.tom(e)), 0.85f)
                    + 6;
        }

        if (!toque() && !layout.retrato()) {
            float base = layout.painel().y();
            escrever("Som: " + (cena.isSomLigado() ? "ligado" : "desligado") + " (M)", x, base + 92, Color.GRAY, 0.8f);
            escrever("WASD/setas: mover", x, base + 68, Color.GRAY, 0.8f);
            escrever("C/espaço: embarcar", x, base + 48, Color.GRAY, 0.8f);
            escrever("Q/Esc: abortar", x, base + 28, Color.GRAY, 0.8f);
        }
    }

    /** Fase 6: direcional (setas desenhadas) e botões de ação, só no modo toque. */
    private void desenharControles() {
        if (!toque()) {
            return;
        }
        List<Retangulo> areas = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        for (Map.Entry<Botao, Retangulo> b : layout.botoes().entrySet()) {
            areas.add(b.getValue());
            rotulos.add(b.getKey() == Botao.SOM ? (cena.isSomLigado() ? "Som: ligado" : "Som: mudo")
                    : b.getKey().isDirecao() ? "" : b.getKey().getRotulo());
        }
        desenharBotoes(areas, rotulos, 0.95f);
        formas().begin(ShapeType.Filled);
        formas().setColor(COR_DO_DIRECIONAL);
        for (Map.Entry<Botao, Retangulo> b : layout.botoes().entrySet()) {
            if (b.getKey().isDirecao()) {
                seta(b.getKey(), b.getValue());
            }
        }
        formas().end();
    }

    private void seta(Botao direcao, Retangulo r) {
        float cx = r.centroX();
        float cy = r.centroY();
        float t = r.largura() * 0.28f;
        switch (direcao) {
            case CIMA: formas().triangle(cx, cy + t, cx - t, cy - t, cx + t, cy - t); break;
            case BAIXO: formas().triangle(cx, cy - t, cx - t, cy + t, cx + t, cy + t); break;
            case ESQUERDA: formas().triangle(cx - t, cy, cx + t, cy + t, cx + t, cy - t); break;
            default: formas().triangle(cx + t, cy, cx - t, cy + t, cx - t, cy - t); break;
        }
    }

    private static Color cor(Hud.Tom tom) {
        switch (tom) {
            case SUCESSO: return COR_SUCESSO;
            case ALERTA: return COR_ALERTA;
            case PERIGO: return COR_PERIGO;
            default: return Color.LIGHT_GRAY;
        }
    }
}
