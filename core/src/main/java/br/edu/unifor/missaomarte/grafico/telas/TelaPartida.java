package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Conteudo;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.GeometriaDoMapa;
import br.edu.unifor.missaomarte.grafico.apresentacao.GeometriaDoMapa.Quadrado;
import br.edu.unifor.missaomarte.grafico.apresentacao.QuadroDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.List;

/**
 * A partida na janela. Esta tela é "humilde": repassa as teclas ao {@link ControladorDaPartida} e desenha o
 * estado que ele expõe. Não sabe o que é colisão, embarque ou vitória.
 *
 * <p>Fase 3: o desenho usa só formas geométricas e a fonte padrão. Sprites, animações, sons e HUD
 * entram na Fase 4.
 */
public class TelaPartida extends TelaBase {

    private static final float LADO_DO_MAPA = MissaoMarteGame.ALTURA - 2 * MARGEM;
    private static final float X_DO_PAINEL = MARGEM + LADO_DO_MAPA + 20;
    private static final float LARGURA_DO_PAINEL = MissaoMarteGame.LARGURA - X_DO_PAINEL - MARGEM;

    private final ControladorDaPartida controlador;
    private final GeometriaDoMapa geometria;

    public TelaPartida(MissaoMarteGame jogo, ControladorDaPartida controlador) {
        super(jogo);
        this.controlador = controlador;
        this.geometria = new GeometriaDoMapa(controlador.getPartida().getMissao().getMapa(),
                MARGEM, MARGEM, LADO_DO_MAPA);
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                controlador.teclaPressionada(tecla);
                controlador.getFim().ifPresent(jogo::irParaFimDePartida);
                return true;
            }
        };
    }

    @Override
    protected void desenhar() {
        List<QuadroDaPartida.Celula> celulas = QuadroDaPartida.de(controlador.getPartida().getMissao());
        desenharFormas(celulas);
        desenharTextos(celulas);
    }

    private void desenharFormas(List<QuadroDaPartida.Celula> celulas) {
        float lado = geometria.getLadoDaCelula();
        formas().begin(ShapeType.Filled);
        for (QuadroDaPartida.Celula c : celulas) {
            Quadrado q = geometria.celula(c.posicao());
            formas().setColor(0.16f, 0.09f, 0.08f, 1);           // solo marciano
            formas().rect(q.x() + 1, q.y() + 1, lado - 2, lado - 2);
            float cx = q.centroX();
            float cy = q.centroY();
            switch (c.conteudo()) {
                case NAVE:
                    formas().setColor(Color.CYAN);
                    formas().triangle(cx, cy + lado * 0.38f, cx - lado * 0.3f, cy - lado * 0.3f,
                            cx + lado * 0.3f, cy - lado * 0.3f);
                    break;
                case PROFESSOR:
                case ENGENHEIRO:
                case ASTRONAUTA:
                    formas().setColor(corDoPassageiro(c.conteudo()));
                    formas().circle(cx, cy, lado * 0.3f, 24);
                    break;
                case ASTEROIDE:
                    formas().setColor(Color.GRAY);
                    formas().circle(cx, cy, lado * 0.36f, 9);  // poucos lados: parece uma rocha
                    break;
                case INIMIGO:
                    formas().setColor(Color.SCARLET);
                    formas().rectLine(q.x() + lado * 0.2f, q.y() + lado * 0.2f,
                            q.x() + lado * 0.8f, q.y() + lado * 0.8f, lado * 0.12f);
                    formas().rectLine(q.x() + lado * 0.2f, q.y() + lado * 0.8f,
                            q.x() + lado * 0.8f, q.y() + lado * 0.2f, lado * 0.12f);
                    break;
                default:
                    break;
            }
        }
        formas().end();

        // contorno da plataforma de pouso
        formas().begin(ShapeType.Line);
        formas().setColor(Color.LIME);
        Quadrado l = geometria.celula(controlador.getPartida().getMissao().getMapa().getPlataforma());
        formas().rect(l.x() + 2, l.y() + 2, lado - 4, lado - 4);
        formas().end();
    }

    private static Color corDoPassageiro(Conteudo conteudo) {
        switch (conteudo) {
            case ENGENHEIRO: return Color.GOLD;
            case ASTRONAUTA: return Color.WHITE;
            default: return Color.ROYAL;
        }
    }

    private void desenharTextos(List<QuadroDaPartida.Celula> celulas) {
        float lado = geometria.getLadoDaCelula();
        float escalaDaCelula = Math.max(0.8f, lado / 36f);
        lote().begin();
        // letra de cada passageiro e da plataforma, como no console (P, E, T, L)
        for (QuadroDaPartida.Celula c : celulas) {
            Conteudo k = c.conteudo();
            if (k == Conteudo.PROFESSOR || k == Conteudo.ENGENHEIRO || k == Conteudo.ASTRONAUTA
                    || k == Conteudo.PLATAFORMA) {
                Quadrado q = geometria.celula(c.posicao());
                Color cor = k == Conteudo.PLATAFORMA ? Color.LIME : Color.BLACK;
                escrever(String.valueOf(k.getSimbolo()), q.centroX() - 5 * escalaDaCelula,
                        q.centroY() + 7 * escalaDaCelula, cor, escalaDaCelula);
            }
        }

        // painel de status (o HUD de verdade vem na Fase 4)
        Partida partida = controlador.getPartida();
        float y = MissaoMarteGame.ALTURA - MARGEM;
        escrever("MISSÃO MARTE", X_DO_PAINEL, y, Color.ORANGE, 1.3f);
        y -= 34;
        for (String linha : TextosDaInterface.status(partida)) {
            escrever(linha, X_DO_PAINEL, y, Color.WHITE, 1f);
            y -= 22;
        }
        y -= 12;
        for (Evento e : controlador.getUltimosEventos()) {
            y -= escreverComQuebra(TextosDaInterface.evento(e), X_DO_PAINEL, y, LARGURA_DO_PAINEL,
                    Color.YELLOW, 1f) + 8;
        }
        escrever("WASD/setas: mover", X_DO_PAINEL, 92, Color.GRAY, 1f);
        escrever("C/espaço: embarcar", X_DO_PAINEL, 70, Color.GRAY, 1f);
        escrever("Q/Esc: abortar", X_DO_PAINEL, 48, Color.GRAY, 1f);
        lote().end();
    }
}
