package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking.Aba;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;

/** Escolha do modo e das abas do ranking por teclado ou toque. */
public class TelaRanking extends TelaBase {

    private static final Retangulo ABA_LOCAL = new Retangulo(205, 402, 185, 72);
    private static final Retangulo ABA_GLOBAL = new Retangulo(410, 402, 185, 72);
    private static final Retangulo BOTAO_VOLTAR = new Retangulo(290, 20, 220, 72);
    private static final Retangulo MODO_CLASSICO = new Retangulo(110, 480, 180, 72);
    private static final Retangulo MODO_PILOTAGEM = new Retangulo(310, 480, 180, 72);
    private static final Retangulo MODO_HORDA = new Retangulo(510, 480, 180, 72);

    private final ControladorDoRanking controlador;

    public TelaRanking(MissaoMarteGame jogo, ControladorDoRanking controlador) {
        super(jogo);
        this.controlador = controlador;
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                if (controlador.teclaPressionada(tecla) == ControladorDoRanking.Acao.VOLTAR) {
                    jogo.irParaMenu();
                }
                return true;
            }
        };
    }

    /** Fase 6: tocar numa aba aperta L ou G; o botão Voltar aperta Enter. */
    @Override
    protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(ABA_LOCAL, Keys.L, 'l'), tecla(ABA_GLOBAL, Keys.G, 'g'),
                tecla(MODO_CLASSICO, Keys.C, 'c'), tecla(MODO_PILOTAGEM, Keys.P, 'p'),
                tecla(MODO_HORDA, Keys.H, 'h'), tecla(BOTAO_VOLTAR, Keys.ENTER, (char) 0));
    }

    @Override
    protected void desenhar() {
        boolean local = controlador.getAba() == Aba.LOCAL;
        boolean horda = controlador.isHorda();
        boolean classico = !horda && controlador.getModo() == Modo.CLASSICO;
        boolean primeira = local || controlador.getAba() == Aba.ESTA_HORDA;
        String titulo = switch (controlador.getAba()) {
            case LOCAL -> "RANKING TOP 5 PILOTOS";
            case GLOBAL -> "RANKING GLOBAL TOP 10";
            case ESTA_HORDA -> "TOP 10 DA HORDA";
            case SEMANA -> "PLACAR DA SEMANA";
        };
        desenharBotoes(List.of(MODO_CLASSICO, MODO_PILOTAGEM, MODO_HORDA, ABA_LOCAL, ABA_GLOBAL), List.of(
                rotulo("Clássico", classico), rotulo("Pilotagem", !horda && !classico), rotulo("Horda", horda),
                rotulo(horda ? "Esta horda" : "Local", primeira), rotulo(horda ? "Semana" : "Global", !primeira)), 1.1f);
        lote().begin();
        escreverTitulo(titulo, 590, Color.ORANGE, 1.8f);
        float y = 387;
        float escala = local ? 1.1f : 0.95f;
        for (String linha : controlador.linhas()) {
            y -= escreverComQuebra(linha, 60, y, 680, Color.WHITE, escala) + (local ? 14 : 8);
        }
        if (!toque()) {
            escreverCentralizado("Esquerda/direita: aba  |  Cima/baixo: modo  |  Enter: voltar ao menu", 70, Color.SKY,
                    1.1f);
            lote().end();
        } else {
            lote().end();
            desenharBotoes(List.of(BOTAO_VOLTAR), List.of("Voltar ao menu"), 1.1f);
        }
    }

    private static String rotulo(String nome, boolean selecionado) {
        return selecionado ? "[ " + nome + " ]" : nome;
    }
}
