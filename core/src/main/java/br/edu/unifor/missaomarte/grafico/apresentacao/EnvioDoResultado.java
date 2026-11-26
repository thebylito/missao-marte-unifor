package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.RegistroDePartida;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Envio do resultado ao servidor no fim da partida, em segundo plano. A tela de fim já aparece com o resultado
 * local e só acompanha este estado: se o servidor estiver fora do ar, aparece um aviso e nada mais (a partida já
 * acabou e o ranking local já foi gravado). O convidado não envia nada.
 */
public final class EnvioDoResultado implements Resposta<RegistroDePartida> {

    public enum Estado { CONVIDADO, ENVIANDO, ENVIADO, FALHOU }

    private Estado estado;
    private String motivo = "";
    private boolean sessaoRecusada;
    private RegistroDePartida registro;
    private final boolean horda;
    private final Consumer<RegistroDePartida> aoRegistrar;
    private final Consumer<Falha> aoFalhar;

    EnvioDoResultado(Estado estado, boolean horda, Consumer<RegistroDePartida> aoRegistrar, Consumer<Falha> aoFalhar) {
        this.estado = estado;
        this.horda = horda;
        this.aoRegistrar = aoRegistrar;
        this.aoFalhar = aoFalhar;
    }

    public static EnvioDoResultado iniciar(Optional<Sessao> sessao, ServidorDoJogo servidor, PartidaParaEnviar partida) {
        return InicioDaPartida.semBilhete(sessao, servidor).enviar(partida, registro -> { }, falha -> { });
    }

    @Override
    public void sucesso(RegistroDePartida registro) {
        estado = Estado.ENVIADO;
        this.registro = registro;
        if (registro != null) {
            aoRegistrar.accept(registro);
        }
    }

    public Optional<String> textoDaColocacao() {
        if (!horda || registro == null) return Optional.empty();
        return Optional.of(registro.colocacao() == null ? "Fora do Top 10 da Horda"
                : registro.colocacao() + "º lugar na Horda");
    }

    public Optional<String> textoDasMoedas() {
        return Optional.ofNullable(registro)
                .map(r -> "+" + r.moedasGanhas() + " moedas  |  Saldo: " + r.saldo());
    }

    @Override
    public void falha(Falha falha) {
        estado = Estado.FALHOU;
        motivo = horda && "HORDA_ENCERRADA".equals(falha.codigo())
                ? "O prazo para registrar esta Horda terminou." : falha.mensagem();
        sessaoRecusada = falha.motivo() == Falha.Motivo.NAO_AUTORIZADO;
        aoFalhar.accept(falha);
    }

    public Estado getEstado() {
        return estado;
    }

    /** O servidor recusou o token (vencido ou inválido): a tela avisa o jogo, que desconecta o piloto. */
    public boolean isSessaoRecusada() {
        return sessaoRecusada;
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
