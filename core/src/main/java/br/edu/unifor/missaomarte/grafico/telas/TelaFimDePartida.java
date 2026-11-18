package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/** Resultado da partida: vitória com estatísticas, derrota ou abandono. Enter volta ao menu. */
public class TelaFimDePartida extends TelaBase {

    private final FimDePartida fim;

    public TelaFimDePartida(MissaoMarteGame jogo, FimDePartida fim) {
        super(jogo);
        this.fim = fim;
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
        Color corDoTitulo = fim.estado() == Partida.Estado.VITORIA ? Color.LIME
                : fim.estado() == Partida.Estado.ABORTADA ? Color.LIGHT_GRAY : Color.SCARLET;
        lote().begin();
        escreverCentralizado(TextosDaInterface.tituloDoFim(fim), 500, corDoTitulo, 2.4f);
        float y = 410;
        for (String linha : TextosDaInterface.linhasDoFim(fim)) {
            y -= escreverComQuebra(linha, 140, y, 520, Color.WHITE, 1.2f) + 10;
        }
        escreverCentralizado("Enter: voltar ao menu", 70, Color.SKY, 1.1f);
        lote().end();
    }
}
