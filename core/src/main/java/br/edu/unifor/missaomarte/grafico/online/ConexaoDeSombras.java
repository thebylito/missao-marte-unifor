package br.edu.unifor.missaomarte.grafico.online;

/**
 * Porta do modo shadow (Fase 7): uma conexão de mão dupla com o servidor, por onde vai a posição da nave e chegam as
 * dos outros pilotos. A implementação de verdade é a {@link ConexaoWebSocket}; os testes usam uma falsa.
 *
 * <p>Tudo é assíncrono: {@link #abrir} volta na hora, e os avisos do {@link Ouvinte} chegam depois, sempre na thread
 * do jogo (a mesma do desenho), então quem os recebe não precisa de sincronização.
 */
public interface ConexaoDeSombras {

    /** Quem recebe os avisos da conexão. */
    interface Ouvinte {
        void aberta();

        void mensagem(String texto);

        /** A conexão caiu, foi recusada (token inválido: 401) ou não chegou a abrir. */
        void fechada(String motivo);
    }

    /** Abre a conexão com o token do jogador (fecha antes a anterior, se houver). */
    void abrir(String token, Ouvinte ouvinte);

    /** Manda um texto, se a conexão estiver aberta; senão, descarta (uma posição perdida não faz falta). */
    void enviar(String texto);

    /** Fecha sem avisar o ouvinte (quem fecha já sabe). */
    void fechar();
}
