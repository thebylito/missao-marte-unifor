package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import com.badlogic.gdx.Input.Keys;
import java.io.IOException;

/**
 * Menu principal da janela, com as mesmas 4 opções do console. Decide o que cada tecla faz; a tela só
 * desenha e troca de tela conforme a {@link Acao} devolvida (Humble Object, como o ControladorDaPartida).
 */
public final class ControladorDoMenu {

    public enum Acao { NENHUMA, NOVA_PARTIDA, RANKING, SAIR }

    public static final String PERGUNTA_APAGAR = "Você realmente deseja limpar o histórico de ranking? (S/N)";

    private final ServicoRanking servicoRanking;
    private boolean confirmandoApagar;
    private String mensagem = "";

    public ControladorDoMenu(ServicoRanking servicoRanking) {
        this.servicoRanking = servicoRanking;
    }

    public Acao teclaPressionada(int tecla) {
        if (confirmandoApagar) {
            confirmandoApagar = false;
            mensagem = tecla == Keys.S ? apagarRanking() : "Operação cancelada.";
            return Acao.NENHUMA;
        }
        switch (tecla) {
            case Keys.NUM_1:
            case Keys.NUMPAD_1:
                mensagem = "";
                return Acao.NOVA_PARTIDA;
            case Keys.NUM_2:
            case Keys.NUMPAD_2:
                mensagem = "";
                return Acao.RANKING;
            case Keys.NUM_3:
            case Keys.NUMPAD_3:
                confirmandoApagar = true;
                mensagem = PERGUNTA_APAGAR;
                return Acao.NENHUMA;
            case Keys.NUM_4:
            case Keys.NUMPAD_4:
                return Acao.SAIR;
            default:
                mensagem = "Opção inválida. Tente novamente.";
                return Acao.NENHUMA;
        }
    }

    private String apagarRanking() {
        try {
            servicoRanking.apagar();
            return "Ranking resetado com sucesso!";
        } catch (IOException e) {
            return "Erro ao resetar ranking: " + e.getMessage();
        }
    }

    public boolean isConfirmandoApagar() {
        return confirmandoApagar;
    }

    /** Resposta à última tecla ("Opção inválida...", "Ranking resetado..."), ou vazio. */
    public String getMensagem() {
        return mensagem;
    }
}
