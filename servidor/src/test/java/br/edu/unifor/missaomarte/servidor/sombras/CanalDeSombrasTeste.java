package br.edu.unifor.missaomarte.servidor.sombras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste.RelogioDeTeste;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * O modo shadow de ponta a ponta: o servidor de verdade numa porta aleatória e clientes WebSocket de verdade. O
 * relógio é o de teste: o limite de 40 ms entre posições só "passa" quando o teste avança o relógio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:missaomarte-sombras;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais"})
@AutoConfigureMockMvc
class CanalDeSombrasTeste {

    private static final long ESPERA_MS = 3000;
    /** Tempo para concluir que uma mensagem NÃO chegou. */
    private static final long SILENCIO_MS = 300;

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    /** Um piloto conectado: guarda o que recebe. */
    private static final class Cliente extends TextWebSocketHandler {
        final BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        final CountDownLatch fechou = new CountDownLatch(1);
        volatile CloseStatus statusDoFechamento;
        WebSocketSession sessao;

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage m) {
            recebidas.add(m.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
            statusDoFechamento = status;
            fechou.countDown();
        }

        void enviar(String texto) throws Exception {
            sessao.sendMessage(new TextMessage(texto));
        }

        String proxima() throws InterruptedException {
            String m = recebidas.poll(ESPERA_MS, TimeUnit.MILLISECONDS);
            assertThat(m).as("mensagem esperada").isNotNull();
            return m;
        }

        void nadaChegou() throws InterruptedException {
            assertThat(recebidas.poll(SILENCIO_MS, TimeUnit.MILLISECONDS)).isNull();
        }
    }

    @LocalServerPort
    int porta;

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioDeTeste relogio;

    @Autowired
    CanalDeSombras canal;

    private final List<Cliente> abertos = new ArrayList<>();

    @BeforeEach
    void zerarRelogio() {
        relogio.agora = Instant.now();
    }

    @AfterEach
    void fecharTodos() throws Exception {
        for (Cliente c : abertos) {
            if (c.sessao != null && c.sessao.isOpen()) {
                c.sessao.close();
            }
        }
        for (Cliente c : abertos) {
            c.fechou.await(ESPERA_MS, TimeUnit.MILLISECONDS);
        }
        // o cliente vê o fechamento antes de o servidor tirar o piloto da lista: o próximo teste começa do zero
        esperarConectados(0);
    }

    /** Cadastra e entra com um nome novo (o banco não é desfeito entre os testes); devolve o token. */
    private String token(String nome) throws Exception {
        String senha = "senha-" + UUID.randomUUID();
        mvc.perform(post("/api/jogadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}")).andExpect(status().isCreated());
        String corpo = mvc.perform(post("/api/sessoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private static String nomeUnico(String prefixo) {
        return prefixo + UUID.randomUUID().toString().substring(0, 8);
    }

    private Cliente conectar(String token) throws Exception {
        WebSocketHttpHeaders cabecalhos = new WebSocketHttpHeaders();
        if (token != null) {
            cabecalhos.add("Authorization", "Bearer " + token);
        }
        Cliente c = new Cliente();
        c.sessao = new StandardWebSocketClient().execute(c, cabecalhos,
                URI.create("ws://localhost:" + porta + ConfiguracaoDasSombras.ENDERECO)).get(ESPERA_MS,
                TimeUnit.MILLISECONDS);
        abertos.add(c);
        return c;
    }

    /**
     * Espera o servidor chegar a este número de pilotos: o aperto de mão termina antes do registro, e o fechamento
     * chega ao cliente antes de o servidor tirar o piloto da lista.
     */
    private void esperarConectados(int quantos) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (canal.conectados() != quantos && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
        assertThat(canal.conectados()).isEqualTo(quantos);
    }

    private static String posicao(double x, double y, double angulo, boolean motor) {
        return "{\"tipo\":\"posicao\",\"x\":" + x + ",\"y\":" + y + ",\"angulo\":" + angulo + ",\"motor\":" + motor
                + "}";
    }

    private void avancarRelogio() {
        relogio.avancar(Duration.ofMillis(CanalDeSombras.INTERVALO_MINIMO_MS));
    }

    @Test
    void semTokenOApertoDeMaoERecusado() {
        assertThatThrownBy(() -> conectar(null)).hasMessageContaining("401");
    }

    @Test
    void comTokenInvalidoOApertoDeMaoERecusado() {
        assertThatThrownBy(() -> conectar("nao-e-um-token")).hasMessageContaining("401");
    }

    @Test
    void aPosicaoDeUmPilotoChegaAosOutrosComONomeDoToken() throws Exception {
        String ana = nomeUnico("Ana");
        Cliente a = conectar(token(ana));
        Cliente b = conectar(token(nomeUnico("Bia")));
        esperarConectados(2);

        a.enviar("{\"tipo\":\"posicao\",\"x\":1940.5,\"y\":300,\"angulo\":-12.5,\"motor\":true,\"nome\":\"Falso\"}");

        String sombra = b.proxima();
        assertThat((String) JsonPath.read(sombra, "$.tipo")).isEqualTo("sombra");
        assertThat((String) JsonPath.read(sombra, "$.nome")).isEqualTo(ana);   // do token, não da mensagem
        assertThat((Double) JsonPath.read(sombra, "$.x")).isEqualTo(1940.5);
        assertThat((Double) JsonPath.read(sombra, "$.y")).isEqualTo(300.0);
        assertThat((Double) JsonPath.read(sombra, "$.angulo")).isEqualTo(-12.5);
        assertThat((Boolean) JsonPath.read(sombra, "$.motor")).isTrue();
        assertThat((Integer) JsonPath.read(sombra, "$.piloto")).isPositive();
        a.nadaChegou();   // ninguém recebe a própria sombra
    }

    @Test
    void mensagensInvalidasSaoIgnoradasSemDerrubarAConexao() throws Exception {
        Cliente a = conectar(token(nomeUnico("Caio")));
        Cliente b = conectar(token(nomeUnico("Davi")));
        esperarConectados(2);

        for (String ruim : List.of("não é json", "[]", "{\"tipo\":\"outro\"}",
                posicao(-1, 100, 0, false), posicao(3841, 100, 0, false), posicao(100, 1441, 0, false),
                posicao(100, 100, 181, false), "{\"tipo\":\"posicao\",\"x\":\"10\",\"y\":10,\"angulo\":0,\"motor\":false}",
                "{\"tipo\":\"posicao\",\"x\":10,\"y\":10,\"angulo\":0}")) {
            a.enviar(ruim);
            avancarRelogio();
        }
        b.nadaChegou();

        a.enviar(posicao(10, 20, 0, false));   // a conexão continua valendo
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(10.0);
    }

    @Test
    void posicoesRapidasDemaisSaoDescartadas() throws Exception {
        Cliente a = conectar(token(nomeUnico("Eva")));
        Cliente b = conectar(token(nomeUnico("Fabio")));
        esperarConectados(2);

        a.enviar(posicao(100, 100, 0, false));
        a.enviar(posicao(200, 100, 0, false));   // mesmo instante: descartada
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(100.0);
        b.nadaChegou();

        avancarRelogio();
        a.enviar(posicao(300, 100, 0, false));
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(300.0);
    }

    @Test
    void quemSaiEAvisadoAosOutros() throws Exception {
        Cliente a = conectar(token(nomeUnico("Gil")));
        Cliente b = conectar(token(nomeUnico("Hugo")));
        esperarConectados(2);
        a.enviar(posicao(100, 100, 0, false));
        int piloto = JsonPath.read(b.proxima(), "$.piloto");

        a.sessao.close();

        String saiu = b.proxima();
        assertThat((String) JsonPath.read(saiu, "$.tipo")).isEqualTo("saiu");
        assertThat((Integer) JsonPath.read(saiu, "$.piloto")).isEqualTo(piloto);
    }

    @Test
    void mensagemGrandeDemaisFechaAConexao() throws Exception {
        Cliente a = conectar(token(nomeUnico("Iara")));
        esperarConectados(1);

        a.enviar("{\"tipo\":\"posicao\",\"lixo\":\"" + "x".repeat(CanalDeSombras.TAMANHO_MAXIMO) + "\"}");

        assertThat(a.fechou.await(ESPERA_MS, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(a.statusDoFechamento.getCode()).isEqualTo(CloseStatus.TOO_BIG_TO_PROCESS.getCode());
    }
}
