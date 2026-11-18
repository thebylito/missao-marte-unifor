package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Resposta;

/**
 * Estado de uma consulta ao servidor que a tela mostra enquanto espera: carregando, pronta ou com falha. Recebe a
 * resposta do adaptador (é uma {@link Resposta}); a tela só lê o estado a cada quadro.
 */
public final class ConsultaRemota<T> implements Resposta<T> {

    public enum Estado { CARREGANDO, PRONTA, FALHOU }

    private Estado estado = Estado.CARREGANDO;
    private T valor;
    private String mensagem = "";

    @Override
    public void sucesso(T valor) {
        this.valor = valor;
        this.estado = Estado.PRONTA;
    }

    @Override
    public void falha(Falha falha) {
        this.mensagem = falha.mensagem();
        this.estado = Estado.FALHOU;
    }

    public Estado getEstado() { return estado; }
    /** Presente só quando PRONTA. */
    public T getValor() { return valor; }
    /** Motivo, quando FALHOU. */
    public String getMensagem() { return mensagem; }
}
