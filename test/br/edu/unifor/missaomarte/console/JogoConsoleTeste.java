package br.edu.unifor.missaomarte.console;

import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.RepositorioRanking;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.teste.Teste;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** O fluxo do console roda inteiro em memória: entrada e saída são buffers, o ranking fica numa lista. */
class JogoConsoleTeste {

    private final ByteArrayOutputStream bufferDeSaida = new ByteArrayOutputStream();
    private final PrintStream saida = new PrintStream(bufferDeSaida, true, StandardCharsets.UTF_8);

    /** Entrada que registra o que já tinha sido escrito na tela quando o jogo pediu a primeira leitura. */
    private final class EntradaQueObserva extends InputStream {
        private final ByteArrayInputStream dados;
        String telaNaPrimeiraLeitura;

        EntradaQueObserva(String texto) {
            dados = new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int read() {
            observar();
            return dados.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            observar();
            return dados.read(b, off, len);
        }

        private void observar() {
            if (telaNaPrimeiraLeitura == null) {
                telaNaPrimeiraLeitura = bufferDeSaida.toString(StandardCharsets.UTF_8);
            }
        }
    }

    private static final class RankingEmMemoria implements RepositorioRanking {
        List<RegistroRanking> salvos = new ArrayList<>();

        @Override public List<RegistroRanking> carregar() { return Ranking.ordenar(salvos); }
        @Override public void salvar(List<RegistroRanking> registros) { salvos = new ArrayList<>(registros); }
        @Override public void apagar() { salvos.clear(); }
    }

    private String jogar(InputStream entrada) {
        new JogoConsole(new EntradaConsole(new Scanner(entrada, StandardCharsets.UTF_8), saida), new TelaConsole(saida),
                new GeradorDeMissao(new Random(1)), new ServicoRanking(new RankingEmMemoria(), Relogio.DO_SISTEMA),
                Relogio.DO_SISTEMA).executar();
        return bufferDeSaida.toString(StandardCharsets.UTF_8);
    }

    @Teste
    void menuApareceAntesDeEsperarAPrimeiraEscolha() {
        EntradaQueObserva entrada = new EntradaQueObserva("4\n");
        jogar(entrada);
        verdadeiro(entrada.telaNaPrimeiraLeitura.contains("--- MENU PRINCIPAL ---"),
                "o menu deveria estar na tela antes da leitura, mas a tela era:\n" + entrada.telaNaPrimeiraLeitura);
        verdadeiro(entrada.telaNaPrimeiraLeitura.endsWith("Escolha uma opção: "), "o prompt também");
    }

    @Teste
    void primeiroEnterNaoViraOpcaoInvalidaSemMenu() {
        String tela = jogar(new EntradaQueObserva("2\n4\n"));
        verdadeiro(tela.contains("RANKING TOP 5"), "a primeira linha digitada é a opção escolhida");
        verdadeiro(!tela.contains("Opção inválida"), "nenhuma opção inválida");
    }

    @Teste
    void fimDaEntradaNoMenuEncerraComMensagem() {
        String tela = jogar(new EntradaQueObserva(""));
        verdadeiro(tela.endsWith("Escolha uma opção: \nEntrada encerrada. Obrigado por jogar!\n"),
                "termina com o menu e a mensagem de encerramento, foi:\n" + tela);
        igual(1, tela.split("MENU PRINCIPAL", -1).length - 1, "menu mostrado uma vez");
    }
}
