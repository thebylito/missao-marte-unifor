package br.edu.unifor.missaomarte;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.console.EntradaConsole;
import br.edu.unifor.missaomarte.console.JogoConsole;
import br.edu.unifor.missaomarte.console.TelaConsole;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

/**
 * Ponto de entrada. Só monta as peças (composition root): escolhe as implementações concretas e as
 * entrega a quem precisa. Nenhuma regra do jogo fica aqui.
 */
public final class Main {

    private Main() {
    }

    /** Opcional: -Dmissaomarte.semente=N repete o mesmo sorteio (usado na validação). */
    static final String PROPRIEDADE_SEMENTE = "missaomarte.semente";

    public static void main(String[] args) {
        Relogio relogio = Relogio.DO_SISTEMA;
        Long semente = Long.getLong(PROPRIEDADE_SEMENTE);
        new JogoConsole(
                new EntradaConsole(new Scanner(System.in), System.out),
                new TelaConsole(System.out),
                new GeradorDeMissao(semente == null ? new Random() : new Random(semente)),
                new ServicoRanking(new RankingArquivoJson(Paths.get("ranking.json")), relogio),
                relogio
        ).executar();
    }
}
