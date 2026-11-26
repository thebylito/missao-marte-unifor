package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import com.badlogic.gdx.Input.Keys;
import java.io.IOException;
import java.util.Optional;

/**
 * Menu principal da janela, com as mesmas 4 opções do console e, na Fase 5, o histórico online (5) e a troca de
 * piloto (6). Decide o que cada tecla faz; a tela só
 * desenha e troca de tela conforme a {@link Acao} devolvida (Humble Object, como o ControladorDaPartida).
 */
public final class ControladorDoMenu {

    public enum Acao { NENHUMA, NOVA_PARTIDA, RANKING, SAIR, HISTORICO, TROCAR_PILOTO, HANGAR }

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
            case Keys.NUM_5:              // Fase 5: histórico no servidor
            case Keys.NUMPAD_5:
                mensagem = "";
                return Acao.HISTORICO;
            case Keys.NUM_6:              // Fase 5: entrar com outra conta ou como convidado
            case Keys.NUMPAD_6:
                mensagem = "";
                return Acao.TROCAR_PILOTO;
            case Keys.NUM_7:
            case Keys.NUMPAD_7:
                mensagem = "";
                return Acao.HANGAR;
            default:
                mensagem = "Opção inválida. Tente novamente.";
                return Acao.NENHUMA;
        }
    }

    public static Optional<String> textoDoSaldo(Optional<Sessao> sessao, Integer saldo) {
        return sessao.map(piloto -> "Moedas: " + (saldo == null ? "—" : saldo));
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
