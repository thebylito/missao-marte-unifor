package br.edu.unifor.missaomarte.grafico.online;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

/**
 * O modo shadow por WebSocket (Fase 7), com a biblioteca Java-WebSocket: Java puro, funciona igual no desktop e no
 * Android (que não tem o {@code java.net.http} do Java 11). O token vai no cabeçalho {@code Authorization} do aperto
 * de mão, como nos pedidos HTTP; ele nunca aparece em log nem em mensagem de erro.
 *
 * <p>A biblioteca avisa numa thread própria; cada aviso é repassado à thread do jogo. Avisos de uma conexão antiga
 * (já fechada ou trocada por outra) são descartados.
 */
public final class ConexaoWebSocket implements ConexaoDeSombras {

    /** O caminho do canal no servidor (o mesmo do {@code ConfiguracaoDasSombras} do servidor). */
    public static final String CAMINHO = "/ws/sombras";
    /** Sem resposta do servidor por este tempo (pings da própria biblioteca), a conexão é dada como perdida. */
    private static final int SEGUNDOS_SEM_RESPOSTA = 10;

    private final URI endereco;
    private final Executor naThreadDoJogo;
    private volatile WebSocketClient atual;

    public ConexaoWebSocket(URI endereco, Executor naThreadDoJogo) {
        this.endereco = endereco;
        this.naThreadDoJogo = naThreadDoJogo;
    }

    /**
     * O endereço do canal a partir do endereço HTTP do servidor: {@code http://h:p} vira {@code ws://h:p/ws/sombras}
     * e {@code https://} vira {@code wss://} (criptografado, atrás de um proxy com HTTPS).
     */
    public static URI enderecoDasSombras(String enderecoHttp) {
        String e = enderecoHttp == null ? "" : enderecoHttp.trim();
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        String base;
        if (e.regionMatches(true, 0, "https://", 0, 8)) {
            base = "wss://" + e.substring(8);
        } else if (e.regionMatches(true, 0, "http://", 0, 7)) {
            base = "ws://" + e.substring(7);
        } else {
            throw new IllegalArgumentException("endereço do servidor sem http:// ou https://");
        }
        return URI.create(base + CAMINHO);
    }

    @Override
    public void abrir(String token, Ouvinte ouvinte) {
        abrir(token, "livre", ouvinte);
    }

    @Override
    public void abrir(String token, String sala, Ouvinte ouvinte) {
        if (!sala.matches("livre|horda-[0-9]+")) {
            throw new IllegalArgumentException("Sala de sombras inválida");
        }
        fechar();
        URI destino = URI.create(endereco + "?sala=" + sala);
        WebSocketClient cliente = new WebSocketClient(destino, Map.of("Authorization", "Bearer " + token)) {
            @Override
            public void onOpen(ServerHandshake aperto) {
                avisar(this, ouvinte::aberta);
            }

            @Override
            public void onMessage(String texto) {
                avisar(this, () -> ouvinte.mensagem(texto));
            }

            @Override
            public void onClose(int codigo, String motivo, boolean remoto) {
                avisar(this, () -> {
                    if (atual == this) {
                        atual = null;
                    }
                    ouvinte.fechada(codigo + (motivo == null || motivo.isBlank() ? "" : " " + motivo));
                });
            }

            @Override
            public void onError(Exception erro) {
                // o onClose vem logo depois, com o motivo
            }
        };
        cliente.setConnectionLostTimeout(SEGUNDOS_SEM_RESPOSTA);
        cliente.setDaemon(true);   // não segura o processo aberto quando o jogo fecha
        atual = cliente;
        cliente.connect();   // não bloqueia: conecta numa thread da biblioteca
    }

    /** Repassa o aviso à thread do jogo, se ainda for da conexão atual. */
    private void avisar(WebSocketClient de, Runnable aviso) {
        naThreadDoJogo.execute(() -> {
            if (atual == de) {
                aviso.run();
            }
        });
    }

    @Override
    public void enviar(String texto) {
        WebSocketClient cliente = atual;
        if (cliente != null && cliente.isOpen()) {
            try {
                cliente.send(texto);
            } catch (WebsocketNotConnectedException e) {
                // fechou entre o isOpen e o send: o onClose avisa
            }
        }
    }

    @Override
    public void fechar() {
        WebSocketClient cliente = atual;
        atual = null;   // o onClose desta conexão não chega mais ao ouvinte
        if (cliente != null) {
            cliente.close();
        }
    }
}
