package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking.Aba;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;

/**
 * Ranking com duas abas: Top 5 local (o ranking.json, o mesmo do console) e Top 10 global (servidor, Fase 5).
 * Setas ou Tab trocam de aba; Enter ou Esc volta ao menu. As decisões ficam no {@link ControladorDoRanking}.
 * Fase 7: cada modo tem o seu ranking; setas para cima e para baixo (ou os botões) trocam o modo.
 */
public class TelaRanking extends TelaBase {

    private static final Retangulo ABA_LOCAL = new Retangulo(240, 425, 150, 48);
    private static final Retangulo ABA_GLOBAL = new Retangulo(410, 425, 150, 48);
    private static final Retangulo BOTAO_VOLTAR = new Retangulo(290, 36, 220, 64);
    private static final Retangulo MODO_CLASSICO = new Retangulo(240, 480, 150, 44);
    private static final Retangulo MODO_PILOTAGEM = new Retangulo(410, 480, 150, 44);

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
                tecla(BOTAO_VOLTAR, Keys.ENTER, (char) 0));
    }

    @Override
    protected void desenhar() {
        boolean local = controlador.getAba() == Aba.LOCAL;
        boolean classico = controlador.getModo() == Modo.CLASSICO;
        lote().begin();
        escreverTitulo(local ? "RANKING TOP 5 PILOTOS" : "RANKING GLOBAL TOP 10", 560, Color.ORANGE, 2f);
        escreverCentralizado((classico ? "[ Clássico ]" : "  Clássico  ") + "      "
                + (classico ? "  Pilotagem  " : "[ Pilotagem ]"), 510, Color.ORANGE, 1.1f);
        escreverCentralizado((local ? "[ Local ]" : "  Local  ") + "      " + (local ? "  Global  " : "[ Global ]"),
                455, Color.SKY, 1.1f);
        float y = 410;
        float escala = local ? 1.1f : 0.95f;
        for (String linha : controlador.linhas()) {
            y -= escreverComQuebra(linha, 60, y, 680, Color.WHITE, escala) + (local ? 14 : 8);
        }
        if (!toque()) {
            escreverCentralizado("Setas: Local / Global  |  Cima/baixo: modo  |  Enter: voltar ao menu", 70, Color.SKY,
                    1.1f);
            lote().end();
        } else {
            lote().end();
            desenharBotoes(List.of(BOTAO_VOLTAR), List.of("Voltar ao menu"), 1.1f);
        }
    }
}
