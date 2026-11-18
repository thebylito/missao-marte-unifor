package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoMenu;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/** Menu principal: as 4 opções do console, escolhidas pelas teclas 1 a 4. */
public class TelaMenu extends TelaBase {

    private final ControladorDoMenu controlador;

    public TelaMenu(MissaoMarteGame jogo) {
        super(jogo);
        this.controlador = new ControladorDoMenu(jogo.getServicos().ranking());
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                switch (controlador.teclaPressionada(tecla)) {
                    case NOVA_PARTIDA: jogo.irParaNovaPartida(); break;
                    case RANKING: jogo.irParaRanking(); break;
                    case SAIR: jogo.sair(); break;
                    default: break;
                }
                return true;
            }
        };
    }

    @Override
    protected void desenhar() {
        lote().begin();
        escreverCentralizado("MISSÃO MARTE UNIFOR", 520, Color.ORANGE, 2.4f);
        escreverCentralizado("Pilote sua nave, salve os passageiros e desvie dos perigos!", 465, Color.LIGHT_GRAY, 1.1f);
        String[] opcoes = {
            "1. Iniciar Nova Missão",
            "2. Visualizar Ranking Top 5",
            "3. Resetar Histórico de Ranking",
            "4. Sair do Jogo",
        };
        float y = 380;
        for (String opcao : opcoes) {
            escrever(opcao, 260, y, Color.WHITE, 1.4f);
            y -= 42;
        }
        Color corDaMensagem = controlador.isConfirmandoApagar() ? Color.YELLOW : Color.SKY;
        escreverCentralizado(controlador.getMensagem(), 150, corDaMensagem, 1.2f);
        escreverCentralizado("Escolha uma opção (teclas 1 a 4)", 60, Color.GRAY, 1f);
        lote().end();
    }
}
