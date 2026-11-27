package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import br.edu.unifor.missaomarte.grafico.telas.TelaPilotagem;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Arena local de validação do combate leve da campanha. */
public final class TelaEncontro extends TelaPilotagem {
    private final EncontroDeResgate encontro;
    private final ControladorDoEncontro controle;
    private final Set<Integer> dedosPulso = new HashSet<>();
    private boolean tecladoPulso;

    public TelaEncontro(MissaoMarteGame jogo, EncontroDeResgate encontro) {
        this(jogo, encontro, new ControladorDoEncontro(encontro));
    }

    private TelaEncontro(MissaoMarteGame jogo, EncontroDeResgate encontro, ControladorDoEncontro controle) {
        super(jogo, controle);
        this.encontro = encontro;
        this.controle = controle;
    }

    public EncontroDeResgate getEncontro() { return encontro; }

    public Retangulo botaoPulso() {
        Retangulo v = visivel();
        Retangulo motor = LayoutDaPilotagem.calcular(v, true, .5).botoes().get(LayoutDaPilotagem.Botao.MOTOR);
        return new Retangulo(motor.x() - 168, v.y() + 20, 154, 92);
    }

    public Retangulo botaoAssistencia() {
        Retangulo v = visivel();
        return new Retangulo(botaoPulso().x(), v.y() + 126, 154, 54);
    }

    @Override protected InputAdapter entrada() {
        InputAdapter voo = super.entrada();
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if (tecla == Keys.F) { tecladoPulso = true; atualizarPulso(); return true; }
                if (tecla == Keys.G) { controle.alternarAssistencia(); return true; }
                return voo.keyDown(tecla);
            }
            @Override public boolean keyUp(int tecla) {
                if (tecla == Keys.F) { tecladoPulso = false; atualizarPulso(); return true; }
                return voo.keyUp(tecla);
            }
            @Override public boolean touchDown(int x, int y, int dedo, int botao) {
                float[] p = naTela(x, y);
                if (botaoPulso().contem(p[0], p[1])) {
                    dedosPulso.add(dedo);
                    atualizarPulso();
                    return true;
                }
                if (botaoAssistencia().contem(p[0], p[1])) {
                    controle.alternarAssistencia();
                    return true;
                }
                return voo.touchDown(x, y, dedo, botao);
            }
            @Override public boolean touchDragged(int x, int y, int dedo) {
                if (dedosPulso.contains(dedo)) {
                    float[] p = naTela(x, y);
                    if (!botaoPulso().contem(p[0], p[1])) {
                        dedosPulso.remove(dedo);
                        atualizarPulso();
                    }
                    return true;
                }
                return voo.touchDragged(x, y, dedo);
            }
            @Override public boolean touchUp(int x, int y, int dedo, int botao) {
                if (dedosPulso.remove(dedo)) { atualizarPulso(); return true; }
                return voo.touchUp(x, y, dedo, botao);
            }
        };
    }

    private void atualizarPulso() { controle.pulso(tecladoPulso || !dedosPulso.isEmpty()); }

    @Override public void pause() {
        super.pause();
        limparEntradas();
    }

    @Override public void resize(int largura, int altura) {
        super.resize(largura, altura);
        limparEntradas();
    }

    private void limparEntradas() {
        tecladoPulso = false;
        dedosPulso.clear();
        controle.limparComandos();
    }

    @Override protected float yDaMensagem() { return visivel().y() + 215; }

    @Override protected void atualizar(float delta) {
        EncontroDeResgate.Estado anterior = encontro.estado();
        double pulsoAnterior = encontro.pulsoRestante();
        super.atualizar(delta);
        if (encontro.estado() == EncontroDeResgate.Estado.AVISO && encontro.estado() != anterior) {
            jogo.getSons().tocar(Som.ALERTA);
        }
        if (encontro.pulsoRestante() > pulsoAnterior) jogo.getSons().tocar(Som.MOVER);
    }

    @Override protected void desenharElementosExtras() {
        float x = (float) encontro.inimigoX(), y = (float) encontro.inimigoY();
        int quadro = (int) (encontro.partida().getTempo() * 12) % 8;
        Color cor = switch (encontro.estado()) {
            case AVISO -> Color.ORANGE;
            case ESPERA -> Color.SALMON;
            case DESATIVADO -> Color.GRAY;
            case RECUPERANDO -> Color.CYAN;
            default -> Color.WHITE;
        };
        lote().setColor(cor);
        lote().draw(recursos().quadro(Folha.PATRULHEIRO, quadro), x - 48, y - 32, 96, 64);
        lote().setColor(Color.WHITE);
        if (encontro.estado() == EncontroDeResgate.Estado.AVISO) {
            escrever("!", x - 6, y + 62, Color.ORANGE, 2);
        } else if (encontro.estado() == EncontroDeResgate.Estado.DESATIVADO) {
            escrever("DESATIVADO", x - 54, y + 54, Color.CYAN, .75f);
        } else if (encontro.alvoNoCone()) {
            escrever("ALVO", x - 22, y + 54, Color.CYAN, .8f);
        }
        for (EncontroDeResgate.Projetil p : encontro.projeteis()) {
            lote().draw(recursos().quadro(Folha.DISPARO_HISTORIA, quadro % 4),
                    (float) p.x() - 12, (float) p.y() - 12, 24, 24);
        }
        if (encontro.pulsoRestante() > 0) {
            float progresso = 1 - (float) encontro.pulsoRestante() / .22f;
            double angulo = Math.toRadians(encontro.pulsoAngulo());
            float px = (float) (encontro.pulsoX() - Math.sin(angulo) * progresso * 300);
            float py = (float) (encontro.pulsoY() + Math.cos(angulo) * progresso * 300);
            TextureRegion textura = recursos().quadro(Folha.PULSO_HISTORIA, Math.min(7, (int) (progresso * 8)));
            lote().draw(textura, px - 48, py - 48, 48, 48, 96, 96, 1, 1,
                    (float) encontro.pulsoAngulo());
        }
    }

    @Override protected void desenharInterfaceExtra() {
        Retangulo v = visivel();
        lote().begin();
        escrever("Escudo: " + encontro.escudo() + "/2   EMP: " + encontro.bateria() + "/4",
                v.x() + 32, v.topo() - 255, Color.CYAN, 1);
        escrever("Resgate Maria e volte à base", v.x() + 32, v.topo() - 283, Color.WHITE, .85f);
        escrever("EMP frontal: 300 m. Recarrega na base.", v.x() + 32, v.topo() - 309, Color.LIGHT_GRAY, .75f);
        lote().end();
        desenharBotoes(List.of(botaoPulso(), botaoAssistencia()),
                List.of("PULSO [F]", controle.assistido() ? "Auto: ligado [G]" : "Auto: desligado [G]"), .85f);
    }

    @Override protected void concluirPartida(FimDePartida fim) {
        jogo.trocarTela(new TelaResultadoDoEncontro(jogo, encontro));
    }
}
