package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ConsultaRemota;
import br.edu.unifor.missaomarte.grafico.apresentacao.TextosDaInterface;
import br.edu.unifor.missaomarte.grafico.online.PartidaDoHistorico;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import java.util.List;

/** Histórico do jogador conectado, consultado no servidor (Fase 5). O convidado vê um aviso. Enter volta ao menu. */
public class TelaHistorico extends TelaBase {

    private final ConsultaRemota<List<PartidaDoHistorico>> consulta;

    public TelaHistorico(MissaoMarteGame jogo) {
        super(jogo);
        if (jogo.getSessao().isPresent()) {
            consulta = new ConsultaRemota<>();
            jogo.getServicos().servidor().historico(jogo.getSessao().get(), consulta);
        } else {
            consulta = null;
        }
    }

    private static final Retangulo BOTAO_VOLTAR =
            new Retangulo(290, 36, 220, 64);

    /** Fase 6: tocar em qualquer lugar volta ao menu (como o Enter). */
    @Override
    protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(visivel(), Keys.ENTER, (char) 0));
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

    private List<String> linhas() {
        if (consulta == null) {
            return List.of("Você está jogando como convidado.", "Entre com uma conta (opção 6 do menu) para ter um "
                    + "histórico de partidas no servidor.");
        }
        switch (consulta.getEstado()) {
            case CARREGANDO: return List.of("Consultando o servidor...");
            case FALHOU: return List.of(consulta.getMensagem());
            default: return TextosDaInterface.historico(consulta.getValor());
        }
    }

    @Override
    protected void desenhar() {
        lote().begin();
        escreverTitulo("MEU HISTÓRICO", 520, Color.ORANGE, 2f);
        jogo.getSessao().ifPresent(s -> escreverCentralizado("Piloto: " + s.nome(), 460, Color.SKY, 1.1f));
        float y = 420;
        for (String linha : linhas()) {
            if (y < 110) {
                escrever("...", 60, y, Color.GRAY, 0.9f);
                break;
            }
            y -= escreverComQuebra(linha, 60, y, 680, Color.WHITE, 0.9f) + 6;
        }
        if (!toque()) {
            escreverCentralizado("Enter: voltar ao menu", 70, Color.SKY, 1.1f);
        }
        lote().end();
        if (toque()) {
            desenharBotoes(List.of(BOTAO_VOLTAR), List.of("Voltar ao menu"), 1.1f);
        }
    }
}
