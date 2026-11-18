package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.FolhaDeSprites;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/**
 * Resultado da partida: vitória com estatísticas, derrota ou abandono. Enter volta ao menu. Na Fase 4 ganhou arte
 * (a nave na vitória, a explosão na derrota) e o som do fim, tocado uma vez ao mostrar a tela.
 */
public class TelaFimDePartida extends TelaBase {

    private final FimDePartida fim;
    private float tempo;

    public TelaFimDePartida(MissaoMarteGame jogo, FimDePartida fim) {
        super(jogo);
        this.fim = fim;
    }

    @Override
    public void show() {
        super.show();
        ReacaoAosEventos.somDoFim(fim.estado()).ifPresent(jogo.getSons()::tocar);
    }

    @Override
    protected void atualizar(float delta) {
        tempo += delta;
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
        desenharArte();
        escreverTitulo(TextosDaInterface.tituloDoFim(fim), 480, corDoTitulo, 2.4f);
        float y = 410;
        for (String linha : TextosDaInterface.linhasDoFim(fim)) {
            y -= escreverComQuebra(linha, 140, y, 520, Color.WHITE, 1.2f) + 10;
        }
        escreverCentralizado("Enter: voltar ao menu", 70, Color.SKY, 1.1f);
        lote().end();
    }

    /** Vitória: a nave decolando (sobe e desce devagar). Derrota: a explosão em laço. Abandono: a nave parada. */
    private void desenharArte() {
        if (!recursosProntos()) {
            return;
        }
        switch (fim.estado()) {
            case VITORIA:
                desenharCentralizado(Folha.NAVE, 0, 400, 545 + 6 * (float) Math.sin(tempo * 2.2), 96);
                break;
            case DERROTA:
                desenharCentralizado(Folha.EXPLOSAO, FolhaDeSprites.quadroEmLaco(Folha.EXPLOSAO, tempo, 14), 400, 545, 110);
                break;
            default:
                lote().setColor(0.6f, 0.6f, 0.6f, 1);
                desenharCentralizado(Folha.NAVE, 0, 400, 545, 80);
                lote().setColor(Color.WHITE);
                break;
        }
    }
}
