package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * O adaptador WebSocket contra um servidor WebSocket de verdade, local (o da própria biblioteca), numa porta livre.
 * Confere o aperto de mão com o token, as mensagens nos dois sentidos e que os avisos chegam pelo executor do jogo.
 */
class ConexaoWebSocketTeste {

    private static final long ESPERA_S = 5;

    /** Servidor de eco: guarda o cabeçalho de autorização e o que recebe, e responde com "eco:" + texto. */
    private static final class ServidorDeEco extends WebSocketServer {
        final BlockingQueue<String> autorizacoes = new LinkedBlockingQueue<>();
        final BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        final CountDownLatch pronto = new CountDownLatch(1);
        final BlockingQueue<Integer> fechamentos = new LinkedBlockingQueue<>();

        ServidorDeEco() {
            super(new InetSocketAddress("127.0.0.1", 0));
            setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conexao, ClientHandshake aperto) {
            autorizacoes.add(aperto.getFieldValue("Authorization"));
        }

        @Override
        public void onClose(WebSocket conexao, int codigo, String motivo, boolean remoto) {
            fechamentos.add(codigo);
        }

        @Override
        public void onMessage(WebSocket conexao, String texto) {
            recebidas.add(texto);
            conexao.send("eco:" + texto);
        }

        @Override
        public void onError(WebSocket conexao, Exception erro) {
        }

        @Override
        public void onStart() {
            pronto.countDown();
        }
    }

    /** Ouvinte que guarda os avisos e em qual thread chegaram. */
    private static final class Avisos implements ConexaoDeSombras.Ouvinte {
        final CountDownLatch aberta = new CountDownLatch(1);
        final BlockingQueue<String> mensagens = new LinkedBlockingQueue<>();
        final BlockingQueue<String> fechadas = new LinkedBlockingQueue<>();
        final BlockingQueue<Boolean> naThreadDoJogo = new LinkedBlockingQueue<>();

        private void conferirThread() {
            naThreadDoJogo.add(Thread.currentThread().getName().equals("jogo"));
        }

        @Override public void aberta() { conferirThread(); aberta.countDown(); }
        @Override public void mensagem(String texto) { conferirThread(); mensagens.add(texto); }
        @Override public void fechada(String motivo) { conferirThread(); fechadas.add(motivo); }
    }

    private ServidorDeEco servidor;
    private final java.util.concurrent.ExecutorService jogo =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> new Thread(r, "jogo"));

    @AfterEach
    void parar() throws InterruptedException {
        jogo.shutdownNow();
        if (servidor != null) {
            servidor.stop(1000);
        }
    }

    private ConexaoWebSocket conectada(Avisos avisos) throws InterruptedException {
        servidor = new ServidorDeEco();
        servidor.start();
        assertTrue(servidor.pronto.await(ESPERA_S, TimeUnit.SECONDS));
        ConexaoWebSocket c = new ConexaoWebSocket(URI.create("ws://127.0.0.1:" + servidor.getPort()
                + ConexaoWebSocket.CAMINHO), jogo);
        c.abrir("meu-token", avisos);
        assertTrue(avisos.aberta.await(ESPERA_S, TimeUnit.SECONDS));
        return c;
    }

    @Test
    void oEnderecoDasSombrasVemDoEnderecoHttpDoServidor() {
        assertEquals(URI.create("ws://localhost:8080/ws/sombras"),
                ConexaoWebSocket.enderecoDasSombras("http://localhost:8080"));
        assertEquals(URI.create("ws://192.168.0.10:8080/ws/sombras"),
                ConexaoWebSocket.enderecoDasSombras(" http://192.168.0.10:8080/ "));
        assertEquals(URI.create("wss://marte.exemplo.com/ws/sombras"),
                ConexaoWebSocket.enderecoDasSombras("HTTPS://marte.exemplo.com"));
        assertThrows(IllegalArgumentException.class, () -> ConexaoWebSocket.enderecoDasSombras("localhost:8080"));
        assertThrows(IllegalArgumentException.class, () -> ConexaoWebSocket.enderecoDasSombras(null));
    }

    @Test
    void oServidorHttpOfereceAConexaoNoMesmoEndereco() {
        ServidorHttp http = new ServidorHttp("http://localhost:8080", () -> null, Runnable::run);
        assertTrue(http.conexaoDeSombras().isPresent());
        assertTrue(new ServidorHttp("sem-protocolo", () -> null, Runnable::run).conexaoDeSombras().isEmpty());
        assertTrue(ServidorDoJogo.DESLIGADO.conexaoDeSombras().isEmpty());
    }

    @Test
    void mandaOTokenNoApertoDeMaoETrocaMensagensPelaThreadDoJogo() throws InterruptedException {
        Avisos avisos = new Avisos();
        ConexaoWebSocket c = conectada(avisos);
        assertEquals("Bearer meu-token", servidor.autorizacoes.poll(ESPERA_S, TimeUnit.SECONDS));

        c.enviar("{\"tipo\":\"posicao\"}");
        assertEquals("{\"tipo\":\"posicao\"}", servidor.recebidas.poll(ESPERA_S, TimeUnit.SECONDS));
        assertEquals("eco:{\"tipo\":\"posicao\"}", avisos.mensagens.poll(ESPERA_S, TimeUnit.SECONDS));
        c.fechar();
        assertNotNull(servidor.fechamentos.poll(ESPERA_S, TimeUnit.SECONDS));
        jogo.submit(() -> { }).isDone();
        for (Boolean naThread : avisos.naThreadDoJogo) {
            assertTrue(naThread);
        }
    }

    @Test
    void quandoOServidorCaiOOuvinteEAvisado() throws InterruptedException {
        Avisos avisos = new Avisos();
        conectada(avisos);
        servidor.stop(1000);
        servidor = null;
        assertNotNull(avisos.fechadas.poll(ESPERA_S, TimeUnit.SECONDS));
    }

    @Test
    void depoisDeFecharNadaMaisChegaAoOuvinteENadaESenviado() throws InterruptedException {
        Avisos avisos = new Avisos();
        ConexaoWebSocket c = conectada(avisos);
        c.fechar();
        c.enviar("depois");   // descartado, sem erro
        assertNull(avisos.fechadas.poll(500, TimeUnit.MILLISECONDS));   // quem fecha não é avisado
        assertNull(servidor.recebidas.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void semServidorOOuvinteEAvisadoDeQueNaoAbriu() throws InterruptedException {
        Avisos avisos = new Avisos();
        // porta 1: ninguém escutando
        new ConexaoWebSocket(URI.create("ws://127.0.0.1:1/ws/sombras"), jogo).abrir("t", avisos);
        assertNotNull(avisos.fechadas.poll(ESPERA_S, TimeUnit.SECONDS));
        assertEquals(1, avisos.aberta.getCount());
    }
}
