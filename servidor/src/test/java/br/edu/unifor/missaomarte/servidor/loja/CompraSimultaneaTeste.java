package br.edu.unifor.missaomarte.servidor.loja;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.servidor.moedas.Origem;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Fase 8 (CT-14, CA-13): duas compras ao mesmo tempo, com saldo para uma só. O servidor de verdade numa porta
 * aleatória, o banco de verdade e dois pedidos HTTP disparados juntos, sem a transação de teste (cada pedido tem a
 * sua, como em produção). A compra trava a linha do jogador antes de conferir o saldo, então uma passa (201) e a
 * outra vê o saldo já descontado (409): o saldo nunca fica negativo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:missaomarte-compras;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais"})
class CompraSimultaneaTeste {

    /** Várias rodadas, cada uma com um jogador novo: uma corrida só passaria por sorte sem a trava. */
    private static final int RODADAS = 8;

    @LocalServerPort
    int porta;

    @Autowired
    ServicoDeMoedas moedas;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService duas = Executors.newFixedThreadPool(2);

    @AfterEach
    void fechar() {
        duas.shutdownNow();
    }

    private HttpResponse<String> pedir(String metodo, String rota, String token, String corpo) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + rota))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo));
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private record Conta(long id, String token) {
    }

    /** Um jogador novo com este saldo. */
    private Conta jogadorCom(int saldo) throws Exception {
        String nome = "J" + UUID.randomUUID().toString().substring(0, 8);
        String credenciais = "{\"nome\":\"" + nome + "\",\"senha\":\"senha-123\"}";
        HttpResponse<String> cadastro = pedir("POST", "/api/jogadores", null, credenciais);
        assertThat(cadastro.statusCode()).isEqualTo(201);
        long id = ((Number) JsonPath.read(cadastro.body(), "$.id")).longValue();
        String token = JsonPath.read(pedir("POST", "/api/sessoes", null, credenciais).body(), "$.token");
        moedas.creditar(id, Origem.SEMANA, "teste:compra", saldo);
        return new Conta(id, token);
    }

    /** Dispara as duas compras juntas e devolve as duas respostas. */
    private List<HttpResponse<String>> comprarJuntas(String token, String skinA, String skinB) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> pedidos = new ArrayList<>();
        for (String skin : List.of(skinA, skinB)) {
            pedidos.add(duas.submit(() -> {
                largada.await();
                return pedir("POST", "/api/loja/compras", token, "{\"skin\":\"" + skin + "\"}");
            }));
        }
        largada.countDown();
        List<HttpResponse<String>> respostas = new ArrayList<>();
        for (Future<HttpResponse<String>> p : pedidos) {
            respostas.add(p.get(20, TimeUnit.SECONDS));
        }
        return respostas;
    }

    private static List<Integer> statusDe(List<HttpResponse<String>> respostas) {
        return respostas.stream().map(HttpResponse::statusCode).sorted().toList();
    }

    private static String motivoDoConflito(List<HttpResponse<String>> respostas) {
        return JsonPath.read(respostas.stream().filter(r -> r.statusCode() == 409).findFirst().orElseThrow().body(),
                "$.motivo");
    }

    @Test
    void duasComprasJuntasComSaldoParaUmaSo() throws Exception {                    // CT-14
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            Conta conta = jogadorCom(60);   // padrao_2 e guara custam 60 cada

            List<HttpResponse<String>> respostas = comprarJuntas(conta.token(), "padrao_2", "guara");

            assertThat(statusDe(respostas)).as("rodada %d: %s", rodada, respostas.stream().map(HttpResponse::body)
                    .toList()).containsExactly(201, 409);
            assertThat(motivoDoConflito(respostas)).isEqualTo("SALDO_INSUFICIENTE");
            assertThat(moedas.saldo(conta.id())).as("rodada %d", rodada).isZero();
            HttpResponse<String> hangar = pedir("GET", "/api/jogadores/eu/hangar", conta.token(), null);
            assertThat((Integer) JsonPath.read(hangar.body(), "$.saldo")).isZero();
            assertThat((List<?>) JsonPath.read(hangar.body(), "$.skins")).hasSize(2);
        }
    }

    @Test
    void aMesmaSkinCompradaDuasVezesAoMesmoTempo() throws Exception {               // CT-14
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            Conta conta = jogadorCom(120);   // daria para pagar duas vezes: quem barra é a posse

            List<HttpResponse<String>> respostas = comprarJuntas(conta.token(), "padrao_2", "padrao_2");

            assertThat(statusDe(respostas)).as("rodada %d: %s", rodada, respostas.stream().map(HttpResponse::body)
                    .toList()).containsExactly(201, 409);
            assertThat(motivoDoConflito(respostas)).isEqualTo("JA_POSSUI");
            assertThat(moedas.saldo(conta.id())).as("rodada %d", rodada).isEqualTo(60);
        }
    }
}
