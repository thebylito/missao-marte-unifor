package br.edu.unifor.missaomarte.console;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Ranking;
import java.io.IOException;
import java.util.Optional;

/**
 * Fluxo do jogo no console: menu, perguntas iniciais e laço de turnos. Traduz o que foi digitado em
 * {@link Comando}, entrega à {@link Partida} e manda a {@link TelaConsole} mostrar os eventos.
 */
public class JogoConsole {

    private static final String PILOTO_PADRAO = "Piloto Anônimo";
    private static final int TAMANHO_PADRAO = 5;

    private final EntradaConsole entrada;
    private final TelaConsole tela;
    private final GeradorDeMissao gerador;
    private final ServicoRanking servicoRanking;
    private final Relogio relogio;

    public JogoConsole(EntradaConsole entrada, TelaConsole tela, GeradorDeMissao gerador,
                       ServicoRanking servicoRanking, Relogio relogio) {
        this.entrada = entrada;
        this.tela = tela;
        this.gerador = gerador;
        this.servicoRanking = servicoRanking;
        this.relogio = relogio;
    }

    public void executar() {
        Ranking ranking = servicoRanking.carregar();
        tela.boasVindas();
        boolean rodando = true;
        while (rodando) {
            // Corrigido na Fase 1: o original esperava a entrada ANTES de mostrar o menu.
            tela.menu();
            Optional<String> opcao = entrada.lerLinhaOuFim("Escolha uma opção: ");
            if (opcao.isEmpty()) {
                tela.mensagem("\nEntrada encerrada. Obrigado por jogar!");
                break;
            }
            switch (opcao.get()) {
                case "1":
                    jogarPartida(ranking);
                    ranking = servicoRanking.carregar();
                    break;
                case "2":
                    tela.ranking(ranking);
                    break;
                case "3":
                    ranking = resetarRanking();
                    break;
                case "4":
                    rodando = false;
                    tela.mensagem("\nObrigado por jogar a Missão Marte Unifor!");
                    break;
                default:
                    tela.mensagem("Opção inválida. Tente novamente.");
            }
        }
        entrada.fechar();
    }

    private void jogarPartida(Ranking ranking) {
        String piloto = entrada.lerLinha("\nDigite o nome do piloto: ", PILOTO_PADRAO);
        if (piloto.isEmpty()) {
            piloto = PILOTO_PADRAO;
        }
        Dificuldade dificuldade = Dificuldade.deString(
                entrada.lerLinha("Escolha a Dificuldade (facil/medio/dificil): ", "medio"));
        int tamanho = lerTamanhoMapa();

        tela.iniciandoMissao(dificuldade);
        if (!entrada.temLinha()) {
            tela.mensagem("Entrada encerrada. Missão não iniciada.");
            return;
        }
        entrada.descartarLinha();

        Partida partida = new Partida(piloto, dificuldade, gerador.gerar(new Mapa(tamanho), dificuldade), relogio);
        while (partida.emAndamento()) {
            tela.mapa(partida);
            tela.status(partida);
            String digitado = entrada.lerLinha("Comando (w/s/a/d/c/q): ", "").toLowerCase();
            if (digitado.isEmpty()) {
                if (!entrada.temLinha()) {
                    tela.mensagem("Entrada encerrada. Missão abortada.");
                    return;
                }
                continue;
            }
            for (Evento e : partida.executar(Comando.daTecla(digitado.charAt(0)))) {
                tela.evento(e);
                if (e.tipo() == Evento.Tipo.VITORIA) {
                    concluirVitoria(partida.getResultado(), ranking);
                }
            }
        }
    }

    private void concluirVitoria(ResultadoPartida resultado, Ranking ranking) {
        tela.estatisticas(resultado, ranking);
        ServicoRanking.Registro registro = servicoRanking.registrarVitoria(ranking, resultado);
        if (registro.erroAoSalvar() != null) {
            tela.mensagem("Não foi possível salvar o ranking: " + registro.erroAoSalvar());
        }
        if (registro.entrou()) {
            tela.mensagem("Parabéns! Você entrou para o Top 5 de pilotos!");
        }
    }

    private int lerTamanhoMapa() {
        try {
            int tamanho = Integer.parseInt(
                    entrada.lerLinha("Tamanho do mapa (ex: 5 para mapa de -5 a +5): ", String.valueOf(TAMANHO_PADRAO)));
            return tamanho > 0 ? tamanho : TAMANHO_PADRAO;
        } catch (NumberFormatException e) {
            tela.mensagem("Entrada inválida, usando tamanho padrão (5).");
            return TAMANHO_PADRAO;
        }
    }

    private Ranking resetarRanking() {
        String resposta = entrada.lerLinha("Você realmente deseja limpar o histórico de ranking? (s/n): ", "n")
                .toLowerCase();
        if (resposta.equals("s") || resposta.equals("sim")) {
            try {
                servicoRanking.apagar();
                tela.mensagem("Ranking resetado com sucesso!");
            } catch (IOException e) {
                tela.mensagem("Erro ao resetar ranking: " + e.getMessage());
            }
            return Ranking.vazio();
        }
        tela.mensagem("Operação cancelada.");
        return servicoRanking.carregar();
    }
}
