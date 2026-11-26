package br.edu.unifor.missaomarte.grafico.online;

import java.util.function.Consumer;

/**
 * Retorno de um pedido assíncrono. O adaptador chama um dos dois métodos uma única vez, já na thread do jogo
 * (a da tela), então quem recebe pode mexer no estado da tela sem cuidado extra com threads.
 */
public interface Resposta<T> {

    void sucesso(T valor);

    void falha(Falha falha);

    static <T> Resposta<T> de(Consumer<T> aoReceber, Consumer<Falha> aoFalhar) {
        return new Resposta<>() {
            @Override public void sucesso(T valor) { aoReceber.accept(valor); }
            @Override public void falha(Falha falha) { aoFalhar.accept(falha); }
        };
    }
}
