package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.CameraDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem.Sombra;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.pilotagem.AsteroideEmOrbita;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo;
import br.edu.unifor.missaomarte.pilotagem.Meteoro;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.PlataformaDePouso;
import br.edu.unifor.missaomarte.pilotagem.Rocha;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pilotagem (Fase 7). Só desenha e repassa a entrada: as regras estão na {@link PartidaDePilotagem} e as decisões
 * da tela no {@link ControladorDaPilotagem}.
 *
 * <p>Duas câmeras: a do mundo, que segue a nave pela caverna (pelo menos 1280 x 720 da caverna na tela, estendido
 * no lado que sobra, como as outras telas), e a das telas (800 x 600), para o painel, o minimapa e os botões.
 *
 * <p>Modo shadow: as naves dos outros pilotos conectados aparecem translúcidas, com o nome em cima
 * ({@link SombrasDaPilotagem}).
 */
public class TelaPilotagem extends TelaBase {

    /** Quanto da caverna aparece na tela, no mínimo. */
    static final float LARGURA_DO_MUNDO = 1280;
    static final float ALTURA_DO_MUNDO = 720;
    /** Graus entre dois quadros da nave (64 ângulos, sentido anti-horário, quadro 0 com o bico para cima). */
    private static final float GRAUS_POR_QUADRO_DA_NAVE = 360f / Folha.NAVE.getQuadros();
    /** Depois da vitória, a nave fica um instante pousada na base antes da tela de fim. */
    private static final float ESPERA_DA_VITORIA = 1.2f;

    private static final Color PAINEL = new Color(0.08f, 0.06f, 0.09f, 0.78f);
    private static final Color SEGURO = new Color(0.45f, 0.95f, 0.45f, 1);
    private static final Color PERIGO = new Color(1f, 0.38f, 0.3f, 1);
    private static final Color ALERTA = new Color(1f, 0.85f, 0.3f, 1);
    private static final Color APERTADO = new Color(0.95f, 0.6f, 0.2f, 0.45f);
    /** As sombras: azuladas e translúcidas, para não confundir com a própria nave. */
    private static final Color SOMBRA = new Color(0.55f, 0.8f, 1f, 0.5f);
    private static final Color NOME_DA_SOMBRA = new Color(0.65f, 0.85f, 1f, 0.85f);

    private final ControladorDaPilotagem controlador;
    private final SombrasDaPilotagem sombras;
    private final OrthographicCamera cameraDoMundo = new OrthographicCamera();
    private final ExtendViewport mundo = new ExtendViewport(LARGURA_DO_MUNDO, ALTURA_DO_MUNDO, cameraDoMundo);
    private final CameraDaPilotagem seguir;
    private LayoutDaPilotagem layout;
    private float relogio;
    private float ultimoQuadro;
    private float esperaDoFim;
    private float tempoDaExplosao = -1;
    private float explosaoX;
    private float explosaoY;
    private float tremor;
    private List<Meteoro> meteoros = List.of();
    private List<Meteoro> rastro = List.of();
    private float tempoDoRastro;

    public TelaPilotagem(MissaoMarteGame jogo, ControladorDaPilotagem controlador) {
        this(jogo, controlador, SombrasDaPilotagem.desligadas());
    }

    public TelaPilotagem(MissaoMarteGame jogo, ControladorDaPilotagem controlador, SombrasDaPilotagem sombras) {
        super(jogo);
        this.controlador = controlador;
        this.sombras = sombras;
        Caverna c = controlador.getPartida().getCaverna();
        this.seguir = new CameraDaPilotagem(c.getLargura(), c.getAltura());
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public void show() {
        super.show();
        jogo.getNaves().prepararSombras();
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                controlador.teclaPressionada(tecla);
                return true;
            }

            @Override
            public boolean keyUp(int tecla) {
                controlador.teclaSolta(tecla);
                return true;
            }

            @Override
            public boolean touchDown(int x, int y, int dedo, int botao) {
                float[] p = naTela(x, y);
                controlador.tocou(dedo, p[0], p[1]);
                return true;
            }

            @Override
            public boolean touchDragged(int x, int y, int dedo) {
                float[] p = naTela(x, y);
                controlador.arrastou(dedo, p[0], p[1]);
                return true;
            }

            @Override
            public boolean touchUp(int x, int y, int dedo, int botao) {
                controlador.soltou(dedo);
                return true;
            }
        };
    }

    /** O Voltar do Android não aborta o voo (como na partida clássica): só o botão Abortar ou o Esc. */
    @Override
    protected boolean voltarFuncionaComoEsc() {
        return false;
    }

    /** O céu é desenhado aqui mesmo, com um pouco de paralaxe. */
    @Override
    protected boolean mostrarFundo() {
        return false;
    }

    @Override
    public void resize(int largura, int altura) {
        super.resize(largura, altura);
        mundo.update(largura, altura, false);
        posicionar();
    }

    /** Para a validação (o piloto automático da janela lê o estado do voo). */
    public ControladorDaPilotagem getControlador() {
        return controlador;
    }

    private void posicionar() {
        Caverna c = controlador.getPartida().getCaverna();
        layout = LayoutDaPilotagem.calcular(visivel(), toque(), c.getAltura() / c.getLargura());
        controlador.usarLayout(layout);
    }

    /** Saiu da tela (fim da partida ou jogo fechando): a sombra desta nave some para os outros na hora. */
    @Override
    public void hide() {
        sombras.fechar();
        super.hide();
    }

    // ------------------------------------------------------------------ tempo

    @Override
    protected void atualizar(float delta) {
        if (layout == null) {
            posicionar();
        }
        relogio += delta;
        ultimoQuadro = delta;
        sombras.atualizar(delta, controlador.getPartida());
        if (sombras.consumirAlerta()) jogo.getSons().tocar(Som.ALERTA);
        List<EventoDeVoo> eventos = controlador.atualizar(delta);
        sombras.tratarEventos(eventos);
        tempoDoRastro += delta;
        if (tempoDoRastro >= 0.08f) {
            rastro = meteoros;
            tempoDoRastro = 0;
        }
        meteoros = controlador.getPartida().getMeteoros();
        for (EventoDeVoo e : eventos) {
            ControladorDaPilotagem.som(e.tipo()).ifPresent(jogo.getSons()::tocar);
            if (e.tipo() == EventoDeVoo.Tipo.EXPLODIU) {
                tempoDaExplosao = 0;
                explosaoX = (float) e.x();
                explosaoY = (float) e.y();
                tremor = 0.5f;
                // os outros pilotos veem a explosão no mesmo ponto (sai antes de a tela fechar, se a partida acabou)
                sombras.explodiu(e.x(), e.y(), controlador.getPartida().getCaverna());
            }
        }
        if (tempoDaExplosao >= 0) {
            tempoDaExplosao += delta;
        }
        tremor = Math.max(0, tremor - delta);
        controlador.getFim().ifPresent(fim -> {
            esperaDoFim += delta;
            if (fim.estado() != Partida.Estado.VITORIA || esperaDoFim >= ESPERA_DA_VITORIA) {
                jogo.irParaFimDePartida(fim);
            }
        });
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void desenhar() {
        if (!recursosProntos()) {
            return;
        }
        PartidaDePilotagem p = controlador.getPartida();
        NaveEmVoo nave = p.getNave();
        double[] centro = seguir.seguir(nave.getX(), nave.getY(), nave.getVelocidadeX(), nave.getVelocidadeY(),
                cameraDoMundo.viewportWidth, cameraDoMundo.viewportHeight, ultimoQuadro);
        float sacudida = tremor * 16;
        cameraDoMundo.position.set((float) centro[0] + (float) Math.sin(relogio * 90) * sacudida,
                (float) centro[1] + (float) Math.cos(relogio * 77) * sacudida, 0);
        cameraDoMundo.update();

        usarCameraDaTela();
        desenharCeu(p.getCaverna());

        mundo.apply();
        lote().setProjectionMatrix(cameraDoMundo.combined);
        formas().setProjectionMatrix(cameraDoMundo.combined);
        lote().begin();
        desenharCaverna(p.getCaverna());
        desenharPassageiros(p);
        desenharAsteroides(p.getCaverna());
        desenharMeteoros(rastro, 0.76f, 0.35f);
        desenharMeteoros(meteoros, 1.25f, 1);
        p.getCapsulaVip().ifPresent(this::desenharVip);
        desenharSombras();
        desenharExplosoesDasSombras();
        desenharNave(nave);
        desenharExplosao();
        lote().end();

        usarCameraDaTela();
        desenharNevoa((float) p.intensidadeDaTempestade());
        desenharPainel(p);
        desenharMinimapa(p);
        desenharBotoesDeVoo();
        sombras.mensagemDoEvento().ifPresent(this::desenharFaixaDoEvento);
    }

    private void desenharFaixaDoEvento(String aviso) {
        Retangulo v = visivel();
        Retangulo motor = layout.botoes().get(Botao.MOTOR);
        float y = v.y() + MARGEM + (motor == null ? 60 : motor.altura() + 46);
        float x = v.centroX() - 246;
        formas().begin(ShapeType.Filled);
        formas().setColor(PAINEL);
        formas().rect(x, y, 492, 40);
        formas().setColor(ALERTA);
        formas().triangle(x + 12, y + 8, x + 38, y + 8, x + 25, y + 32);
        formas().end();
        lote().begin();
        escrever("!", x + 23, y + 24, Color.BLACK, 1f);
        // A fonte do jogo não inclui o pictograma; o triângulo acima o desenha em qualquer plataforma.
        escreverCentralizado(aviso.replace("⚠", "").trim(), y + 28, Color.GOLD, 1.18f);
        lote().end();
    }

    /** O céu cobre a tela, andando um pouco com a câmera (paralaxe: parece mais longe que a rocha). */
    private void desenharCeu(Caverna c) {
        Retangulo v = visivel();
        TextureRegion ceu = recursos().quadro(Folha.FUNDO, 0);
        float escala = Math.max(v.largura() * 1.25f / ceu.getRegionWidth(), v.altura() * 1.15f / ceu.getRegionHeight());
        float w = ceu.getRegionWidth() * escala;
        float h = ceu.getRegionHeight() * escala;
        float tx = (float) (cameraDoMundo.position.x / c.getLargura()) - 0.5f;
        float ty = (float) (cameraDoMundo.position.y / c.getAltura()) - 0.5f;
        lote().begin();
        lote().setColor(0.62f, 0.62f, 0.66f, 1);
        lote().draw(ceu, v.centroX() - w / 2 - tx * (w - v.largura()), v.centroY() - h / 2 - ty * (h - v.altura()),
                w, h);
        lote().setColor(Color.WHITE);
        lote().end();
    }

    /** As paredes com o ladrilho de rocha repetido (a textura se repete, sem esticar) e as plataformas. */
    private void desenharCaverna(Caverna c) {
        Texture rocha = recursos().quadro(Folha.ROCHA, 0).getTexture();
        float lado = Folha.ROCHA.getLargura();
        for (Rocha r : c.getRochas()) {
            float x = (float) r.x();
            float y = (float) r.y();
            float w = (float) r.largura();
            float h = (float) r.altura();
            lote().draw(rocha, x, y, w, h, x / lado, -y / lado, (x + w) / lado, -(y + h) / lado);
        }
        for (PlataformaDePouso pl : c.getPlataformas()) {
            boolean base = pl.getTipo() == PlataformaDePouso.Tipo.BASE;
            Folha folha = base ? Folha.PLATAFORMA : Folha.PLATAFORMA_PASSAGEIRO;
            float topoAteASuperficie = base ? 16 : 12;   // "landingY" do manifesto: onde os pés tocam
            lote().draw(recursos().quadro(folha, 0), (float) pl.getX(),
                    (float) pl.getSuperficie() - (folha.getAltura() - topoAteASuperficie), (float) pl.getLargura(),
                    folha.getAltura());
        }
    }

    /** Quem espera resgate, acenando; embarcando, anda até a nave e some. */
    private void desenharPassageiros(PartidaDePilotagem p) {
        int quadro = (int) (relogio * 8);
        NaveEmVoo nave = p.getNave();
        for (PlataformaDePouso pl : p.getCaverna().getPlataformas()) {
            if (pl.getEsperando().isEmpty()) {
                continue;
            }
            Folha folha = folhaDe(pl.getEsperando().get());
            float x = (float) pl.getCentroX() + 44;
            float y = (float) pl.getSuperficie();
            boolean embarcando = nave.getEstado() == NaveEmVoo.Estado.POUSADA && nave.getPlataforma() == pl
                    && nave.getABordo().size() < nave.getCapacidade();
            float t = embarcando ? (float) p.progressoDoEmbarque() : 0;
            x += ((float) nave.getX() - x) * t;
            lote().setColor(1, 1, 1, 1 - t * 0.8f);
            lote().draw(recursos().quadro(folha, quadro % folha.getQuadros()), x - folha.getLargura() / 2f, y);
            lote().setColor(Color.WHITE);
        }
    }

    private static Folha folhaDe(Passageiro passageiro) {
        switch (passageiro.getTipo()) {
            case ASTRONAUTA: return Folha.ASTRONAUTA;
            case ENGENHEIRO: return Folha.ENGENHEIRO;
            default: return Folha.PROFESSOR;
        }
    }

    /** Os asteroides são pré-renderizados em 32 ângulos: o quadro acompanha a rotação. */
    private void desenharAsteroides(Caverna c) {
        Folha folha = Folha.ASTEROIDE;
        for (AsteroideEmOrbita a : c.getAsteroides()) {
            float tamanho = (float) a.getRaio() * 2 * 1.3f;   // o desenho é um pouco maior que o raio de colisão
            int quadro = (int) (a.getRotacao() / 360 * folha.getQuadros()) % folha.getQuadros();
            lote().draw(recursos().quadro(folha, quadro), (float) a.getX() - tamanho / 2,
                    (float) a.getY() - tamanho / 2, tamanho, tamanho);
        }
    }

    private void desenharMeteoros(List<Meteoro> posicoes, float escala, float alfa) {
        lote().setColor(1, 0.55f, 0.24f, alfa);
        int quadro = (int) (relogio * 12) % Folha.ASTEROIDE.getQuadros();
        for (Meteoro m : posicoes) {
            desenharCentralizado(Folha.ASTEROIDE, quadro, (float) m.x(), (float) m.y(), (float) m.raio() * 2 * escala);
        }
        lote().setColor(Color.WHITE);
    }

    private void desenharVip(PlataformaDePouso vip) {
        Folha folha = Folha.PLATAFORMA_PASSAGEIRO;
        lote().setColor(Color.GOLD);
        lote().draw(recursos().quadro(folha, 0), (float) vip.getX(),
                (float) vip.getSuperficie() - (folha.getAltura() - 12), (float) vip.getLargura(), folha.getAltura());
        lote().setColor(Color.WHITE);
        escrever("VIP", (float) vip.getCentroX() - 15, (float) vip.getSuperficie() + 35, Color.GOLD, 1.4f);
    }

    private void desenharNevoa(float intensidade) {
        if (intensidade <= 0) return;
        Retangulo tela = visivel();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas().begin(ShapeType.Filled);
        formas().setColor(0.68f, 0.40f, 0.24f, intensidade * 0.66f);
        formas().rect(tela.x(), tela.y(), tela.largura(), tela.altura());
        formas().end();
    }

    /** A nave tem 64 ângulos renderizados no Blender (a luz não gira com ela); a chama sai do bocal, girada. */
    private void desenharNave(NaveEmVoo nave) {
        if (nave.getEstado() == NaveEmVoo.Estado.DESTRUIDA) {
            return;
        }
        float x = (float) nave.getX();
        float y = (float) nave.getY();
        float angulo = (float) nave.getAngulo();
        if (nave.isMotorLigado()) {
            double[] bocal = nave.paraMundo(0, -28);
            TextureRegion chama = recursos().quadro(Folha.CHAMA, (int) (relogio * 20) % Folha.CHAMA.getQuadros());
            float w = chama.getRegionWidth();
            float h = chama.getRegionHeight();
            float pulso = 0.85f + 0.25f * (float) Math.abs(Math.sin(relogio * 37));
            lote().draw(chama, (float) bocal[0] - w / 2, (float) bocal[1] - h, w / 2, h, w, h, 0.8f, pulso, angulo);
        }
        int quadro = Math.round(angulo / GRAUS_POR_QUADRO_DA_NAVE);
        quadro = ((quadro % Folha.NAVE.getQuadros()) + Folha.NAVE.getQuadros()) % Folha.NAVE.getQuadros();
        desenharNave(quadro, x, y, Folha.NAVE.getLargura());
    }

    /** As naves dos outros pilotos: translúcidas, azuladas e com o nome em cima. */
    private void desenharSombras() {
        for (Sombra s : sombras.getSombras()) {
            float x = (float) s.x();
            float y = (float) s.y();
            float angulo = (float) s.angulo();
            if (s.motor()) {
                double rad = Math.toRadians(angulo);
                float bx = x + (float) (28 * Math.sin(rad));    // o bocal: 28 abaixo do centro, girado
                float by = y - (float) (28 * Math.cos(rad));
                TextureRegion chama = recursos().quadro(Folha.CHAMA, (int) (relogio * 20) % Folha.CHAMA.getQuadros());
                float w = chama.getRegionWidth();
                float h = chama.getRegionHeight();
                lote().setColor(1, 1, 1, 0.45f);
                lote().draw(chama, bx - w / 2, by - h, w / 2, h, w, h, 0.8f, 1f, angulo);
            }
            lote().setColor(SOMBRA);
            float lado = Folha.NAVE.getLargura();
            lote().draw(jogo.getNaves().sombra(s.skin(), angulo), x - lado / 2, y - lado / 2, lado, lado);
            lote().setColor(Color.WHITE);
            escrever(s.nome(), x - s.nome().length() * 5.5f, y + Folha.NAVE.getAltura() / 2f + 22, NOME_DA_SOMBRA,
                    1.3f);
        }
    }

    /**
     * A explosão da nave de outro piloto: a mesma animação da sua, translúcida como as sombras e sem som nem tremor
     * (quem bateu foi o outro).
     */
    private void desenharExplosoesDasSombras() {
        int quadros = Folha.EXPLOSAO.getQuadros();
        float lado = Folha.EXPLOSAO.getLargura() * 1.6f;
        for (SombrasDaPilotagem.Explosao e : sombras.getExplosoes()) {
            int i = (int) (e.tempo() * 20);
            if (i >= quadros) {
                continue;
            }
            float alfa = i < quadros - 4 ? 0.6f : 0.6f * (quadros - i) / 5f;
            lote().setColor(1, 1, 1, alfa);
            lote().draw(recursos().quadro(Folha.EXPLOSAO, i), (float) e.x() - lado / 2, (float) e.y() - lado / 2,
                    lado, lado);
        }
        lote().setColor(Color.WHITE);
    }

    private void desenharExplosao() {
        if (tempoDaExplosao < 0) {
            return;
        }
        int quadros = Folha.EXPLOSAO.getQuadros();
        int i = (int) (tempoDaExplosao * 20);
        if (i >= quadros) {
            tempoDaExplosao = -1;
            return;
        }
        float alfa = i < quadros - 4 ? 1f : (quadros - i) / 5f;   // a fumaça do fim some aos poucos
        float lado = Folha.EXPLOSAO.getLargura() * 2f;
        lote().setColor(1, 1, 1, alfa);
        lote().draw(recursos().quadro(Folha.EXPLOSAO, i), explosaoX - lado / 2, explosaoY - lado / 2, lado, lado);
        lote().setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------ painel

    private void desenharPainel(PartidaDePilotagem p) {
        Retangulo v = visivel();
        float x = v.x() + MARGEM;
        float topo = v.topo() - MARGEM;
        float largura = 300;
        Optional<String> linhaDasSombras = sombras.descricao();
        float altura = linhaDasSombras.isPresent() ? 240 : 214;
        NaveEmVoo nave = p.getNave();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas().begin(ShapeType.Filled);
        formas().setColor(PAINEL);
        formas().rect(x, topo - altura, largura, altura);
        // combustível: barra que muda de cor
        double fracao = nave.getCombustivel() / p.getConfig().tanque();
        formas().setColor(0.2f, 0.18f, 0.2f, 1);
        formas().rect(x + 112, topo - 120, 170, 14);
        formas().setColor(fracao > 0.5 ? SEGURO : fracao > 0.25 ? ALERTA : PERIGO);
        formas().rect(x + 112, topo - 120, (float) (170 * fracao), 14);
        formas().end();

        lote().begin();
        escrever(controlador.tituloDoVoo(), x + 12, topo - 10, Color.ORANGE, 1.25f);
        escrever(p.getPiloto() + "  |  " + p.getDificuldade(), x + 12, topo - 40, Color.WHITE, 0.95f);
        escrever("Pontos: " + p.getPontos(), x + 12, topo - 66, Color.WHITE, 1f);
        escrever("Vidas: " + p.getVidas(), x + 170, topo - 66, Color.WHITE, 1f);
        escrever("Combustível", x + 12, topo - 106, Color.LIGHT_GRAY, 0.9f);
        escrever("A bordo: " + nave.getABordo().size() + "/" + nave.getCapacidade() + "   Restantes: "
                + p.getCaverna().esperando(), x + 12, topo - 136, Color.WHITE, 0.95f);
        // indicadores de pouso: verdes se um pouso agora seria seguro
        boolean voando = nave.getEstado() == NaveEmVoo.Estado.VOANDO;
        escrever("Pouso:", x + 12, topo - 166, Color.LIGHT_GRAY, 0.9f);
        escrever(String.format("queda %d", Math.round(Math.max(0, -nave.getVelocidadeY()))), x + 80, topo - 166,
                !voando || p.quedaSegura() ? SEGURO : PERIGO, 0.9f);
        escrever(String.format("lado %d", Math.round(Math.abs(nave.getVelocidadeX()))), x + 160, topo - 166,
                !voando || p.deslizeSeguro() ? SEGURO : PERIGO, 0.9f);
        escrever(String.format("ângulo %d°", Math.round(nave.getAngulo())), x + 80, topo - 192,
                !voando || p.inclinacaoSegura() ? SEGURO : PERIGO, 0.9f);
        linhaDasSombras.ifPresent(texto -> escrever(texto, x + 12, topo - 218,
                sombras.getSituacao() == SombrasDaPilotagem.Situacao.CONECTADAS ? Color.SKY : Color.LIGHT_GRAY, 0.9f));
        // as mensagens ficam embaixo (acima dos botões), longe da nave, que a câmera mantém no meio da tela
        Retangulo motor = layout.botoes().get(Botao.MOTOR);
        float yDaMensagem = v.y() + MARGEM + (motor != null ? motor.altura() + 34 : 46);
        String mensagem = controlador.getMensagem();
        if (!mensagem.isEmpty()) {
            escreverCentralizado(mensagem, yDaMensagem, Color.WHITE, 1.1f);
        } else if (relogio < 6) {
            escreverCentralizado(toque() ? "Segure MOTOR para subir; gire com as setas. Pouse devagar e reto."
                    : "Setas (ou A/D) giram, Seta para cima / W / Espaço liga o motor, Esc aborta.",
                    yDaMensagem, Color.SKY, 1f);
        }
        lote().end();
    }

    private void desenharMinimapa(PartidaDePilotagem p) {
        Retangulo m = layout.minimapa();
        Caverna c = p.getCaverna();
        float escala = (float) (m.largura() / c.getLargura());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        formas().begin(ShapeType.Filled);
        formas().setColor(PAINEL);
        formas().rect(m.x() - 4, m.y() - 4, m.largura() + 8, m.altura() + 8);
        formas().setColor(0.55f, 0.3f, 0.2f, 0.95f);
        for (Rocha r : c.getRochas()) {
            formas().rect(m.x() + (float) r.x() * escala, m.y() + (float) r.y() * escala,
                    (float) r.largura() * escala, (float) r.altura() * escala);
        }
        for (PlataformaDePouso pl : c.getPlataformas()) {
            formas().setColor(pl.getTipo() == PlataformaDePouso.Tipo.BASE ? SEGURO
                    : pl.getEsperando().isPresent() ? ALERTA : Color.GRAY);
            formas().rect(m.x() + (float) pl.getX() * escala, m.y() + (float) pl.getSuperficie() * escala,
                    (float) pl.getLargura() * escala, 3);
        }
        formas().setColor(PERIGO);
        for (AsteroideEmOrbita a : c.getAsteroides()) {
            formas().circle(m.x() + (float) a.getX() * escala, m.y() + (float) a.getY() * escala, 2.5f);
        }
        formas().setColor(Color.ORANGE);
        for (Meteoro meteoro : meteoros) {
            formas().circle(m.x() + (float) meteoro.x() * escala, m.y() + (float) meteoro.y() * escala, 2.5f);
        }
        p.getCapsulaVip().ifPresent(vip -> {
            formas().setColor(Color.GOLD);
            formas().rect(m.x() + (float) vip.getX() * escala, m.y() + (float) vip.getSuperficie() * escala,
                    (float) vip.getLargura() * escala, 5);
        });
        formas().setColor(Color.SKY);
        for (Sombra s : sombras.getSombras()) {
            formas().circle(m.x() + (float) s.x() * escala, m.y() + (float) s.y() * escala, 3f);
        }
        NaveEmVoo nave = p.getNave();
        if (nave.getEstado() != NaveEmVoo.Estado.DESTRUIDA) {
            formas().setColor(Color.WHITE);
            formas().circle(m.x() + (float) nave.getX() * escala, m.y() + (float) nave.getY() * escala, 3.5f);
        }
        formas().end();
    }

    /** No toque: ◄ ►, MOTOR e Abortar; o botão apertado acende. */
    private void desenharBotoesDeVoo() {
        if (!toque()) {
            return;
        }
        List<Retangulo> areas = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        for (Map.Entry<Botao, Retangulo> b : layout.botoes().entrySet()) {
            areas.add(b.getValue());
            rotulos.add(b.getKey() == Botao.MOTOR ? "MOTOR" : b.getKey() == Botao.ABORTAR ? "Abortar" : "");
        }
        desenharBotoes(areas, rotulos, 1f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        formas().begin(ShapeType.Filled);
        for (Map.Entry<Botao, Retangulo> b : layout.botoes().entrySet()) {
            Retangulo r = b.getValue();
            if (controlador.dedoEm(b.getKey())) {
                formas().setColor(APERTADO);
                formas().rect(r.x(), r.y(), r.largura(), r.altura());
            }
            float t = r.largura() * 0.26f;
            formas().setColor(0.95f, 0.75f, 0.4f, 1);
            if (b.getKey() == Botao.ESQUERDA) {
                formas().triangle(r.centroX() - t, r.centroY(), r.centroX() + t, r.centroY() + t, r.centroX() + t,
                        r.centroY() - t);
            } else if (b.getKey() == Botao.DIREITA) {
                formas().triangle(r.centroX() + t, r.centroY(), r.centroX() - t, r.centroY() + t, r.centroX() - t,
                        r.centroY() - t);
            }
        }
        formas().end();
    }
}
