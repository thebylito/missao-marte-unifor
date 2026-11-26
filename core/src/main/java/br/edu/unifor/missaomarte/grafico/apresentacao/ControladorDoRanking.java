package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import br.edu.unifor.missaomarte.grafico.online.RankingSemanal;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import com.badlogic.gdx.Input.Keys;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Rankings local e global do Clássico e da Pilotagem, Top 10 da Horda atual e placar da semana.
 * Cada consulta pública ocorre na primeira visita à respectiva aba, por abertura da tela. */
public final class ControladorDoRanking {

    public enum Aba { LOCAL, GLOBAL, ESTA_HORDA, SEMANA }

    public enum Acao { NENHUMA, VOLTAR }

    private final Map<Modo, Ranking> locais = new EnumMap<>(Modo.class);
    private final Map<Modo, ConsultaRemota<List<PosicaoGlobal>>> globais = new EnumMap<>(Modo.class);
    private final ServidorDoJogo servidor;
    private ConsultaRemota<List<PosicaoGlobal>> destaHorda;
    private ConsultaRemota<RankingSemanal> daSemana;
    private Aba ultimaAbaLivre = Aba.LOCAL;
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
                mostrar(aba == Aba.LOCAL || aba == Aba.ESTA_HORDA ? segundaAba() : primeiraAba());
                return Acao.NENHUMA;
            case Keys.L:
                mostrar(primeiraAba());
                return Acao.NENHUMA;
            case Keys.G:
                mostrar(segundaAba());
                return Acao.NENHUMA;
            case Keys.UP:
                if (isHorda()) trocarModo(Modo.PILOTAGEM);
                else if (modo == Modo.PILOTAGEM) trocarModo(Modo.CLASSICO);
                else mostrar(Aba.ESTA_HORDA);
                return Acao.NENHUMA;
            case Keys.DOWN:
                if (isHorda()) trocarModo(Modo.CLASSICO);
                else if (modo == Modo.CLASSICO) trocarModo(Modo.PILOTAGEM);
                else mostrar(Aba.ESTA_HORDA);
                return Acao.NENHUMA;
            case Keys.H:
                mostrar(Aba.ESTA_HORDA);
                return Acao.NENHUMA;
            case Keys.M: // atalho clássico/pilotagem das versões anteriores
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

    public boolean isHorda() { return aba == Aba.ESTA_HORDA || aba == Aba.SEMANA; }
    private Aba primeiraAba() { return isHorda() ? Aba.ESTA_HORDA : Aba.LOCAL; }
    private Aba segundaAba() { return isHorda() ? Aba.SEMANA : Aba.GLOBAL; }

    private void mostrar(Aba nova) {
        aba = nova;
        if (!isHorda()) ultimaAbaLivre = nova;
        if (nova == Aba.ESTA_HORDA && destaHorda == null) {
            destaHorda = new ConsultaRemota<>();
            servidor.rankingDaHorda(null, destaHorda);
        }
        if (nova == Aba.SEMANA && daSemana == null) {
            daSemana = new ConsultaRemota<>();
            servidor.rankingSemanal(daSemana);
        }
        if (nova == Aba.GLOBAL && !globais.containsKey(modo)) {
            ConsultaRemota<List<PosicaoGlobal>> consulta = new ConsultaRemota<>();
            globais.put(modo, consulta);
            servidor.ranking(modo, consulta);
        }
    }

    private void trocarModo(Modo novo) {
        modo = novo;
        mostrar(ultimaAbaLivre);   // na aba global, consulta o ranking do novo modo (uma vez por abertura da tela)
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
        if (aba == Aba.SEMANA) return linhasDaConsulta(daSemana, ControladorDoRanking::semana);
        return linhasDaConsulta(aba == Aba.ESTA_HORDA ? destaHorda : globais.get(modo), TextosDaInterface::rankingGlobal);
    }

    private static <T> List<String> linhasDaConsulta(ConsultaRemota<T> consulta, Function<T, List<String>> formatar) {
        return switch (consulta.getEstado()) {
            case CARREGANDO -> List.of("Consultando o servidor...");
            case FALHOU -> List.of(consulta.getMensagem());
            case PRONTA -> formatar.apply(consulta.getValor());
        };
    }

    private static List<String> semana(RankingSemanal ranking) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Semana de " + ranking.semana());
        if (ranking.posicoes().isEmpty()) linhas.add("Nenhuma pontuação nesta semana.");
        for (var p : ranking.posicoes()) {
            linhas.add(p.posicao() + ". " + p.nome() + " - " + p.pontos() + " pts | Hordas: " + p.hordas()
                    + " | Eventos: " + p.eventos());
        }
        return linhas;
    }
}
