package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SalaDoWebSocketTeste {
    @Test void oApertoDeMaoLevaASalaNaQueryDoWebSocket() throws Exception {
        var ouvinte = new ConexaoDeSombras.Ouvinte() {
            public void aberta() {}
            public void mensagem(String texto) {}
            public void fechada(String motivo) {}
        };
        try (var local = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            local.setSoTimeout(3000);
            URI destino = URI.create("ws://127.0.0.1:" + local.getLocalPort() + "/ws/sombras");
            ConexaoWebSocket conexao = new ConexaoWebSocket(destino, Runnable::run);
            try {
                for (String sala : new String[]{"livre", "horda-123"}) {
                    conexao.abrir("token", sala, ouvinte);
                    try (Socket cliente = local.accept()) {
                        cliente.setSoTimeout(3000);
                        var pedido = new BufferedReader(new InputStreamReader(cliente.getInputStream(),
                                StandardCharsets.US_ASCII));
                        assertEquals("GET /ws/sombras?sala=" + sala + " HTTP/1.1", pedido.readLine());
                    }
                }
            } finally {
                conexao.fechar();
            }
        }
    }
}
