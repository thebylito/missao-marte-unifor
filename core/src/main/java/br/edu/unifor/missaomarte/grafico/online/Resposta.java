package br.edu.unifor.missaomarte.grafico.online;

/**
 * Retorno de um pedido assíncrono. O adaptador chama um dos dois métodos uma única vez, já na thread do jogo
 * (a da tela), então quem recebe pode mexer no estado da tela sem cuidado extra com threads.
 */
public interface Resposta<T> {

    void sucesso(T valor);

    void falha(Falha falha);
}
