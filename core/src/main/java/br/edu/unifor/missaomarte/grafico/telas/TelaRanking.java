package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking.Aba;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/**
 * Ranking com duas abas: Top 5 local (o ranking.json, o mesmo do console) e Top 10 global (servidor, Fase 5).
 * Setas ou Tab trocam de aba; Enter ou Esc volta ao menu. As decisões ficam no {@link ControladorDoRanking}.
 */
public class TelaRanking extends TelaBase {

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

    @Override
    protected void desenhar() {
        boolean local = controlador.getAba() == Aba.LOCAL;
        lote().begin();
        escreverTitulo(local ? "RANKING TOP 5 PILOTOS" : "RANKING GLOBAL TOP 10", 520, Color.ORANGE, 2f);
        escreverCentralizado((local ? "[ Local ]" : "  Local  ") + "      " + (local ? "  Global  " : "[ Global ]"),
                455, Color.SKY, 1.1f);
        float y = 410;
        float escala = local ? 1.1f : 0.95f;
        for (String linha : controlador.linhas()) {
            y -= escreverComQuebra(linha, 60, y, 680, Color.WHITE, escala) + (local ? 14 : 8);
        }
        escreverCentralizado("Setas: Local / Global   |   Enter: voltar ao menu", 70, Color.SKY, 1.1f);
        lote().end();
    }
}
