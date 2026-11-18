package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import java.util.Optional;

/**
 * Envio do resultado ao servidor no fim da partida, em segundo plano. A tela de fim já aparece com o resultado
 * local e só acompanha este estado: se o servidor estiver fora do ar, aparece um aviso e nada mais (a partida já
 * acabou e o ranking local já foi gravado). O convidado não envia nada.
 */
public final class EnvioDoResultado implements Resposta<Void> {

    public enum Estado { CONVIDADO, ENVIANDO, ENVIADO, FALHOU }

    private Estado estado;
    private String motivo = "";

    private EnvioDoResultado(Estado estado) {
        this.estado = estado;
    }

    public static EnvioDoResultado iniciar(Optional<Sessao> sessao, ServidorDoJogo servidor, PartidaParaEnviar partida) {
        if (sessao.isEmpty()) {
            return new EnvioDoResultado(Estado.CONVIDADO);
        }
        EnvioDoResultado envio = new EnvioDoResultado(Estado.ENVIANDO);
        servidor.registrarPartida(sessao.get(), partida, envio);
        return envio;
    }

    @Override
    public void sucesso(Void nada) {
        estado = Estado.ENVIADO;
    }

    @Override
    public void falha(Falha falha) {
        estado = Estado.FALHOU;
        motivo = falha.mensagem();
    }

    public Estado getEstado() {
        return estado;
    }

    /** A linha que a tela de fim mostra. */
    public String texto() {
        switch (estado) {
            case CONVIDADO: return "Modo convidado: o resultado fica só no ranking local.";
            case ENVIANDO: return "Enviando o resultado ao servidor...";
            case ENVIADO: return "Resultado registrado no servidor.";
            default: return "Não foi possível registrar no servidor. " + motivo;
        }
    }
}
