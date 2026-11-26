package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.horda.MontadorDaHorda;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessNet;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HordaHttpTeste {
    private HttpServer servidor;
    private ServidorDoJogo cliente;
    private volatile String metodo, rota, corpo, token;
    private volatile String resposta;
    private final Sessao ana = new Sessao("Ana", "abc");

    @BeforeEach void subir() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            metodo = troca.getRequestMethod();
            rota = troca.getRequestURI().toString();
            token = troca.getRequestHeaders().getFirst("Authorization");
            corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] json = resposta.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders("POST".equals(metodo) ? 201 : 200, json.length);
            try (var saida = troca.getResponseBody()) { saida.write(json); }
        });
        servidor.start();
        var rede = new HeadlessNet(new HeadlessApplicationConfiguration());
        cliente = new ServidorHttp("http://127.0.0.1:" + servidor.getAddress().getPort(), () -> rede, Runnable::run);
    }

    @AfterEach void descer() { servidor.stop(0); }

    private static <T> T chamar(Consumer<Resposta<T>> pedido) throws Exception {
        var futuro = new CompletableFuture<T>();
        pedido.accept(new Resposta<>() {
            @Override public void sucesso(T valor) { futuro.complete(valor); }
            @Override public void falha(Falha falha) { futuro.completeExceptionally(new AssertionError(falha)); }
        });
        return futuro.get(5, TimeUnit.SECONDS);
    }

    @Test void montaAHordaDoExemploCompartilhado() throws Exception {
        String exemplo = Files.readString(Path.of("../docs/api/exemplos/horda.json"));
        resposta = """
                {"numero":42,"segundosParaAcabar":7,"segundosParaProxima":9,"proximaAs":"20:00","descricao":
                """ + exemplo + "}";
        HordaAtual atual = chamar(cliente::hordaAtual);
        assertEquals("GET", metodo);
        assertEquals("/api/hordas/atual", rota);
        assertNull(token);
        assertEquals(42, atual.numero());
        assertEquals(7, atual.segundosParaAcabar());
        assertEquals(9, atual.segundosParaProxima());
        assertEquals("20:00", atual.proximaAs());
        assertEquals(Dificuldade.FACIL, atual.descricao().dificuldade());
        assertEquals(br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda.sortear(
                42, 20261271L, List.of(atual.descricao().caverna()), null), atual.descricao());
        var montada = MontadorDaHorda.montar(atual.descricao());
        assertEquals(11, montada.caverna().getRochas().size());
        assertEquals(4, montada.caverna().getPlataformas().size());
        assertEquals(3, montada.caverna().getAsteroides().size());
        assertEquals(18.627877685732873, montada.ventoConstante());
        assertEquals(-5032694206115545595L, montada.roteiro().get(1).semente());
    }

    @Test void pedeBilheteERegistraNaMesmaHordaComColocacaoOpcional() throws Exception {
        resposta = """
                {"bilhete":"b","validoAte":"2026-11-26T21:00:00Z"}
                """;
        Bilhete bilhete = chamar(r -> cliente.iniciarHorda(ana, Dificuldade.DIFICIL, 42, r));
        assertEquals("b", bilhete.bilhete());
        assertEquals("/api/partidas/inicio", rota);
        assertEquals("POST", metodo);
        assertEquals("Bearer abc", token);
        JsonValue inicio = new JsonReader().parse(corpo);
        assertEquals("HORDA", inicio.getString("modo"));
        assertEquals("DIFICIL", inicio.getString("dificuldade"));
        assertEquals(42, inicio.getLong("horda"));

        var p = new PartidaParaEnviar("VITORIA", "DIFICIL", 70, 3, 4, 110, "HORDA")
                .comHorda(42).comBilhete(bilhete.bilhete());
        resposta = "{\"moedasGanhas\":5,\"saldo\":12,\"colocacao\":3}";
        RegistroDePartida registro = chamar(r -> cliente.registrarPartidaComMoedas(ana, p, r));
        assertEquals(new RegistroDePartida(5, 12, 3), registro);
        assertEquals("/api/partidas", rota);
        JsonValue partida = new JsonReader().parse(corpo);
        assertEquals("b", partida.getString("bilhete"));
        assertEquals(42, partida.getLong("horda"));
        assertEquals("HORDA", partida.getString("modo"));
        resposta = "{\"moedasGanhas\":5,\"saldo\":17,\"colocacao\":null}";
        registro = chamar(r -> cliente.registrarPartidaComMoedas(ana, p, r));
        assertNull(registro.colocacao());
    }

    @Test void consultaOsDoisPlacaresPublicos() throws Exception {
        resposta = """
                [{"posicao":1,"nome":"Ana","pontos":72,"dificuldade":"FACIL","passageiros":3,
                  "tempoSegundos":123,"registradaEm":"ontem"}]
                """;
        List<PosicaoGlobal> lista = chamar(r -> cliente.rankingDaHorda(42L, r));
        assertEquals("/api/ranking?modo=HORDA&horda=42", rota);
        assertEquals("Ana", lista.get(0).nome());
        assertNull(token);
        lista = chamar(r -> cliente.rankingDaHorda(null, r));
        assertEquals("/api/ranking?modo=HORDA", rota);
        assertEquals(72, lista.get(0).pontos());
        resposta = """
                {"semana":"2026-11-23","posicoes":[{"posicao":1,"nome":"Ana","pontos":42,"hordas":5,"eventos":3}]}
                """;
        RankingSemanal semana = chamar(cliente::rankingSemanal);
        assertEquals("/api/ranking/semanal", rota);
        assertEquals("2026-11-23", semana.semana());
        assertEquals(List.of(new RankingSemanal.Posicao(1, "Ana", 42, 5, 3)), semana.posicoes());
    }
}
