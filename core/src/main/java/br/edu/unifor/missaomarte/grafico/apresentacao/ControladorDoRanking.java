package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import com.badlogic.gdx.Input.Keys;
import java.util.List;

/**
 * Tela de ranking com duas abas: o Top 5 local (ranking.json, como antes) e o Top 10 global (servidor). A aba
 * global só consulta o servidor quando é aberta, e cada abertura da tela consulta de novo.
 */
public final class ControladorDoRanking {

    public enum Aba { LOCAL, GLOBAL }

    public enum Acao { NENHUMA, VOLTAR }

    private final Ranking local;
    private final ServidorDoJogo servidor;
    private Aba aba = Aba.LOCAL;
    private ConsultaRemota<List<PosicaoGlobal>> global;

    public ControladorDoRanking(Ranking local, ServidorDoJogo servidor) {
        this.local = local;
        this.servidor = servidor;
    }

    public Acao teclaPressionada(int tecla) {
        switch (tecla) {
            case Keys.LEFT:
            case Keys.RIGHT:
            case Keys.TAB:
                mostrar(aba == Aba.LOCAL ? Aba.GLOBAL : Aba.LOCAL);
                return Acao.NENHUMA;
            case Keys.L:
                mostrar(Aba.LOCAL);
                return Acao.NENHUMA;
            case Keys.G:
                mostrar(Aba.GLOBAL);
                return Acao.NENHUMA;
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
            case Keys.ESCAPE:
                return Acao.VOLTAR;
            default:
                return Acao.NENHUMA;
        }
    }

    private void mostrar(Aba nova) {
        aba = nova;
        if (nova == Aba.GLOBAL && global == null) {
            global = new ConsultaRemota<>();
            servidor.ranking(global);
        }
    }

    public Aba getAba() {
        return aba;
    }

    /** As linhas da aba atual. */
    public List<String> linhas() {
        if (aba == Aba.LOCAL) {
            return TextosDaInterface.ranking(local);
        }
        switch (global.getEstado()) {
            case CARREGANDO: return List.of("Consultando o servidor...");
            case FALHOU: return List.of(global.getMensagem());
            default: return TextosDaInterface.rankingGlobal(global.getValor());
        }
    }
}
