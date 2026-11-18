package br.edu.unifor.missaomarte.servidor.ranking;

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
 * fez a pontuação primeiro. Só vitórias contam, como no ranking local do jogo (Top 5).
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
    public List<PosicaoNoRanking> top10() {
        List<PosicaoNoRanking> ranking = new ArrayList<>();
        Set<Long> jaListados = new HashSet<>();
        for (Partida p : partidas.melhoresPrimeiro(Resultado.VITORIA)) {
            if (jaListados.add(p.getJogador().getId())) {
                ranking.add(new PosicaoNoRanking(ranking.size() + 1, p.getJogador().getNome(), p.getPontos(),
                        p.getDificuldade(), p.getPassageiros(), p.getTempoSegundos(), p.getRegistradaEm()));
                if (ranking.size() == TAMANHO) {
                    break;
                }
            }
        }
        return ranking;
    }
}
