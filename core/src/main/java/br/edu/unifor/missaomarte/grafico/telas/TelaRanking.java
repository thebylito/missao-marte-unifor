package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/** Top 5 lido do ranking.json (o mesmo arquivo do console). Enter ou Esc volta ao menu. */
public class TelaRanking extends TelaBase {

    private final Ranking ranking;

    public TelaRanking(MissaoMarteGame jogo, Ranking ranking) {
        super(jogo);
        this.ranking = ranking;
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                if (tecla == Keys.ENTER || tecla == Keys.NUMPAD_ENTER || tecla == Keys.ESCAPE) {
                    jogo.irParaMenu();
                }
                return true;
            }
        };
    }

    @Override
    protected void desenhar() {
        lote().begin();
        escreverCentralizado("RANKING TOP 5 PILOTOS", 500, Color.ORANGE, 2f);
        float y = 410;
        for (String linha : TextosDaInterface.ranking(ranking)) {
            y -= escreverComQuebra(linha, 60, y, 680, Color.WHITE, 1.1f) + 14;
        }
        escreverCentralizado("Enter: voltar ao menu", 70, Color.SKY, 1.1f);
        lote().end();
    }
}
