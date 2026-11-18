package br.edu.unifor.missaomarte.grafico.online;

/** Jogador autenticado: o nome e o token para os pedidos protegidos. Fica só na memória do jogo, nunca em arquivo. */
public record Sessao(String nome, String token) {

    /** O token nunca aparece em texto (log, mensagem de erro, depurador que usa toString). */
    @Override
    public String toString() {
        return "Sessao[nome=" + nome + ", token=***]";
    }
}
