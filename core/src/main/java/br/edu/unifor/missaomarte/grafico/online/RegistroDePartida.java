package br.edu.unifor.missaomarte.grafico.online;

/** Recompensa e saldo, quando o servidor tem suporte a moedas. */
public record RegistroDePartida(int moedasGanhas, int saldo, Integer colocacao) {
    public RegistroDePartida(int moedasGanhas, int saldo) {
        this(moedasGanhas, saldo, null);
    }
}
