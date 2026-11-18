package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import com.badlogic.gdx.Input.Keys;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tela de ranking com duas abas: o Top 5 local (ranking.json, como antes) e o Top 10 global (servidor). A aba
 * global só consulta o servidor quando é aberta, e cada abertura da tela consulta de novo.
 *
 * <p>Fase 7: cada modo (clássico e pilotagem) tem o seu ranking, local e global. Setas para cima e para baixo (ou M)
 * trocam o modo; as abas continuam nas setas para os lados.
 */
public final class ControladorDoRanking {

    public enum Aba { LOCAL, GLOBAL }

    public enum Acao { NENHUMA, VOLTAR }

    private final Map<Modo, Ranking> locais = new EnumMap<>(Modo.class);
    private final Map<Modo, ConsultaRemota<List<PosicaoGlobal>>> globais = new EnumMap<>(Modo.class);
    private final ServidorDoJogo servidor;
    private Aba aba = Aba.LOCAL;
    private Modo modo = Modo.CLASSICO;

    /** Só o modo clássico (Fases 5 e 6). */
    public ControladorDoRanking(Ranking local, ServidorDoJogo servidor) {
        this(local, Ranking.vazio(), servidor);
    }

    public ControladorDoRanking(Ranking classico, Ranking pilotagem, ServidorDoJogo servidor) {
        this.locais.put(Modo.CLASSICO, classico);
        this.locais.put(Modo.PILOTAGEM, pilotagem);
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
            case Keys.UP:
            case Keys.DOWN:
            case Keys.M:
                trocarModo(modo.outro());
                return Acao.NENHUMA;
            case Keys.C:
                trocarModo(Modo.CLASSICO);
                return Acao.NENHUMA;
            case Keys.P:
                trocarModo(Modo.PILOTAGEM);
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
        if (nova == Aba.GLOBAL && !globais.containsKey(modo)) {
            ConsultaRemota<List<PosicaoGlobal>> consulta = new ConsultaRemota<>();
            globais.put(modo, consulta);
            servidor.ranking(modo, consulta);
        }
    }

    private void trocarModo(Modo novo) {
        modo = novo;
        mostrar(aba);   // na aba global, consulta o ranking do novo modo (uma vez por abertura da tela)
    }

    public Aba getAba() {
        return aba;
    }

    public Modo getModo() {
        return modo;
    }

    /** As linhas da aba atual. */
    public List<String> linhas() {
        if (aba == Aba.LOCAL) {
            return TextosDaInterface.ranking(locais.get(modo));
        }
        ConsultaRemota<List<PosicaoGlobal>> global = globais.get(modo);
        switch (global.getEstado()) {
            case CARREGANDO: return List.of("Consultando o servidor...");
            case FALHOU: return List.of(global.getMensagem());
            default: return TextosDaInterface.rankingGlobal(global.getValor());
        }
    }
}
