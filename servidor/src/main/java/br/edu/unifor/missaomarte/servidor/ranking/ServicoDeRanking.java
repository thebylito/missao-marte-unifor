package br.edu.unifor.missaomarte.servidor.ranking;

import br.edu.unifor.missaomarte.servidor.partida.Modo;
import br.edu.unifor.missaomarte.servidor.partida.Partida;
import br.edu.unifor.missaomarte.servidor.partida.RepositorioDePartidas;
import br.edu.unifor.missaomarte.servidor.partida.Resultado;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Top 10 global: a melhor vitória de cada jogador, da maior para a menor pontuação. No empate, fica na frente quem
 * fez a pontuação primeiro. Só vitórias contam, como no ranking local do jogo (Top 5). Fase 7: cada modo de jogo
 * (clássico e pilotagem) tem o seu Top 10.
 */
@Service
public class ServicoDeRanking {

    public static final int TAMANHO = 10;

    private final RepositorioDePartidas partidas;

    public ServicoDeRanking(RepositorioDePartidas partidas) {
        this.partidas = partidas;
    }

    /**
     * As vitórias já vêm ordenadas (pontos, depois data); a primeira de cada jogador é a melhor dele. Simples e
     * correto para o volume do projeto; com muitos dados, a mesma regra caberia numa consulta com função de janela.
     */
    @Transactional(readOnly = true)
    public List<PosicaoNoRanking> top10(Modo modo, Long horda) {
        return melhores(modo, horda).stream().map(v -> {
            Partida p = v.partida();
            return new PosicaoNoRanking(v.posicao(), p.getJogador().getNome(), p.getPontos(), p.getDificuldade(),
                    p.getPassageiros(), p.getTempoSegundos(), p.getRegistradaEm());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<VitoriaNoRanking> melhores(Modo modo, Long horda) {
        List<Partida> ordenadas = modo == Modo.HORDA
                ? partidas.melhoresDaHorda(Resultado.VITORIA, modo, horda)
                : partidas.melhoresPrimeiro(Resultado.VITORIA, modo);
        List<VitoriaNoRanking> ranking = new ArrayList<>();
        Set<Long> jaListados = new HashSet<>();
        for (Partida p : ordenadas) {
            if (jaListados.add(p.getJogador().getId())) {
                ranking.add(new VitoriaNoRanking(ranking.size() + 1, p));
                if (ranking.size() == TAMANHO) {
                    break;
                }
            }
        }
        return ranking;
    }

    public record VitoriaNoRanking(int posicao, Partida partida) {}
}
