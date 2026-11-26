package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessNet;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * O adaptador HTTP contra um servidor falso do próprio JDK, em localhost: pedidos e respostas de verdade, sem o
 * servidor Spring. Cobre o caminho feliz de cada rota, os erros que o servidor devolve e o servidor fora do ar.
 */
class ServidorHttpTeste {

    /** O que o servidor falso recebeu. */
    record Pedido(String metodo, String rota, String autorizacao, String tipo, String corpo, String consulta) {
    }

    /** O que o adaptador respondeu: valor ou falha. */
    record Retorno<T>(T valor, Falha falha) {
    }

    private final List<Pedido> pedidos = new CopyOnWriteArrayList<>();
    private volatile int codigo = 200;
    private volatile String respostaDoServidor = "";
    private HttpServer servidor;
    private ServidorHttp cliente;

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            pedidos.add(new Pedido(troca.getRequestMethod(), troca.getRequestURI().getPath(),
                    troca.getRequestHeaders().getFirst("Authorization"),
                    troca.getRequestHeaders().getFirst("Content-Type"), corpo, troca.getRequestURI().getQuery()));
            byte[] bytes = respostaDoServidor.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type",
                    codigo >= 400 ? "application/problem+json" : "application/json");
            troca.sendResponseHeaders(codigo, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();
        Net rede = new HeadlessNet(new HeadlessApplicationConfiguration());
        cliente = new ServidorHttp("http://127.0.0.1:" + servidor.getAddress().getPort() + "/", () -> rede,
                Runnable::run);
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    private void responder(int codigo, String json) {
        this.codigo = codigo;
        this.respostaDoServidor = json;
    }

    private static <T> Retorno<T> esperar(Consumer<Resposta<T>> chamada) throws Exception {
        CompletableFuture<Retorno<T>> futuro = new CompletableFuture<>();
        chamada.accept(new Resposta<>() {
            @Override public void sucesso(T valor) { futuro.complete(new Retorno<>(valor, null)); }
            @Override public void falha(Falha falha) { futuro.complete(new Retorno<>(null, falha)); }
        });
        return futuro.get(10, TimeUnit.SECONDS);
    }

    private static JsonValue json(String texto) {
        return new JsonReader().parse(texto);
    }

    @Test
    void entrarEnviaNomeESenhaEDevolveASessao() throws Exception {
        responder(200, "{\"token\":\"abc.def.ghi\",\"tipo\":\"Bearer\",\"expiraEm\":\"2026-11-18T14:00:00Z\","
                + "\"jogador\":{\"id\":1,\"nome\":\"Ana\",\"criadoEm\":\"2026-11-18T12:00:00Z\"}}");
        Retorno<Sessao> r = esperar(resposta -> cliente.entrar("ana", "segredo1", resposta));

        // a validade vem junto: a sessão guardada no aparelho sabe quando deixa de servir
        assertEquals(new Sessao("Ana", "abc.def.ghi", Instant.parse("2026-11-18T14:00:00Z")), r.valor());
        Pedido p = pedidos.get(0);
        assertEquals("POST", p.metodo());
        assertEquals("/api/sessoes", p.rota());                  // a barra no fim do endereço não duplica
        assertNull(p.autorizacao());
        assertTrue(p.tipo().startsWith("application/json"));
        assertEquals("ana", json(p.corpo()).getString("nome"));
        assertEquals("segredo1", json(p.corpo()).getString("senha"));
    }

    @Test
    void cadastroCriadoEhSucesso() throws Exception {
        responder(201, "{\"id\":7,\"nome\":\"Ana\",\"criadoEm\":\"2026-11-18T12:00:00Z\"}");
        Retorno<Void> r = esperar(resposta -> cliente.cadastrar("Ana", "segredo1", resposta));
        assertNull(r.falha());
        assertEquals("/api/jogadores", pedidos.get(0).rota());
    }

    @Test
    void nomeComAspasEBarrasVaiEscapadoNoJson() throws Exception {
        responder(201, "{}");
        ServidorHttpTeste.<Void>esperar(resposta -> cliente.cadastrar("A\"n\\a", "s\"e", resposta));
        assertEquals("A\"n\\a", json(pedidos.get(0).corpo()).getString("nome"));
        assertEquals("s\"e", json(pedidos.get(0).corpo()).getString("senha"));
    }

    @Test
    void senhaErradaViraNaoAutorizadoComAMensagemDoServidor() throws Exception {
        responder(401, "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                + "\"detail\":\"nome ou senha incorretos\"}");
        Retorno<Sessao> r = esperar(resposta -> cliente.entrar("Ana", "errada", resposta));
        assertEquals(new Falha(Falha.Motivo.NAO_AUTORIZADO, "Nome ou senha incorretos."), r.falha());
    }

    @Test
    void nomeEmUsoViraConflito() throws Exception {
        responder(409, "{\"status\":409,\"detail\":\"o nome \\\"Ana\\\" já está em uso\"}");
        Retorno<Void> r = esperar(resposta -> cliente.cadastrar("Ana", "segredo1", resposta));
        assertEquals(new Falha(Falha.Motivo.CONFLITO, "O nome \"Ana\" já está em uso."), r.falha());
    }

    @Test
    void dadosInvalidosJuntamAMensagemDeCadaCampo() throws Exception {
        responder(400, "{\"status\":400,\"detail\":\"dados inválidos\",\"campos\":{"
                + "\"nome\":\"o nome deve ter de 3 a 20 caracteres\",\"senha\":\"a senha deve ter de 6 a 72 caracteres\"}}");
        Retorno<Void> r = esperar(resposta -> cliente.cadastrar("x", "1", resposta));
        assertEquals(Falha.Motivo.DADOS_INVALIDOS, r.falha().motivo());
        assertEquals("Dados inválidos: o nome deve ter de 3 a 20 caracteres; a senha deve ter de 6 a 72 caracteres.",
                r.falha().mensagem());
    }

    @Test
    void erroInternoViraErroDoServidor() throws Exception {
        responder(500, "{\"status\":500,\"detail\":\"erro interno do servidor\"}");
        Retorno<List<PosicaoGlobal>> r = esperar(cliente::ranking);
        assertEquals(new Falha(Falha.Motivo.ERRO_DO_SERVIDOR, "Erro no servidor (500)."), r.falha());
    }

    @Test
    void respostaForaDoFormatoViraRespostaInvalida() throws Exception {
        responder(200, "<html>não sou JSON</html>");
        assertEquals(Falha.Motivo.RESPOSTA_INVALIDA, ServidorHttpTeste.<List<PosicaoGlobal>>esperar(cliente::ranking).falha().motivo());
        responder(200, "{\"token\":\"x\"}");                   // falta o jogador
        assertEquals(Falha.Motivo.RESPOSTA_INVALIDA,
                ServidorHttpTeste.<Sessao>esperar(resposta -> cliente.entrar("Ana", "s", resposta)).falha().motivo());
    }

    @Test
    void servidorForaDoArViraSemConexao() throws Exception {
        servidor.stop(0);
        Retorno<Sessao> r = esperar(resposta -> cliente.entrar("Ana", "segredo1", resposta));
        assertEquals(Falha.Motivo.SEM_CONEXAO, r.falha().motivo());
    }

    @Test
    void semRedeDisponivelTambemViraSemConexao() throws Exception {
        ServidorHttp semRede = new ServidorHttp("http://127.0.0.1:1", () -> null, Runnable::run);
        assertEquals(Falha.Motivo.SEM_CONEXAO, ServidorHttpTeste.<List<PosicaoGlobal>>esperar(semRede::ranking).falha().motivo());
    }

    @Test
    void registrarPartidaLevaOTokenEOsDados() throws Exception {
        responder(201, "{\"id\":3}");
        Retorno<Void> r = esperar(resposta -> cliente.registrarPartida(new Sessao("Ana", "tok-123"),
                new PartidaParaEnviar("VITORIA", "FACIL", 60, 4, 20, 35), resposta));
        assertNull(r.falha());
        Pedido p = pedidos.get(0);
        assertEquals("POST /api/partidas", p.metodo() + " " + p.rota());
        assertEquals("Bearer tok-123", p.autorizacao());
        JsonValue corpo = json(p.corpo());
        assertEquals("VITORIA", corpo.getString("resultado"));
        assertEquals("FACIL", corpo.getString("dificuldade"));
        assertEquals(60, corpo.getInt("pontos"));
        assertEquals(4, corpo.getInt("passageiros"));
        assertEquals(20, corpo.getInt("movimentos"));
        assertEquals(35, corpo.getLong("tempoSegundos"));
        assertEquals("CLASSICO", corpo.getString("modo"), "Fase 7: o resumo clássico diz o modo");
    }

    @Test
    void partidaDaPilotagemVaiComOModo() throws Exception {
        responder(201, "{\"id\":4}");
        ServidorHttpTeste.<Void>esperar(resposta -> cliente.registrarPartida(new Sessao("Ana", "tok"),
                new PartidaParaEnviar("VITORIA", "MEDIO", 61, 3, 6, 140, "PILOTAGEM"), resposta));
        assertEquals("PILOTAGEM", json(pedidos.get(0).corpo()).getString("modo"));
    }

    @Test
    void oRankingDeCadaModoEhPedidoPeloModo() throws Exception {
        responder(200, "[]");
        ServidorHttpTeste.<List<PosicaoGlobal>>esperar(cliente::ranking);
        ServidorHttpTeste.<List<PosicaoGlobal>>esperar(resposta -> cliente.ranking(Modo.PILOTAGEM, resposta));
        assertEquals("modo=CLASSICO", pedidos.get(0).consulta(), "sem modo, o clássico (Fases 5 e 6)");
        assertEquals("modo=PILOTAGEM", pedidos.get(1).consulta());
        assertEquals("/api/ranking", pedidos.get(1).rota());
    }

    @Test
    void historicoLeOModoDeCadaPartida() throws Exception {
        responder(200, "[{\"resultado\":\"VITORIA\",\"dificuldade\":\"MEDIO\",\"pontos\":61,\"passageiros\":3,"
                + "\"movimentos\":6,\"tempoSegundos\":140,\"registradaEm\":\"2026-11-18T12:00:00Z\",\"modo\":\"PILOTAGEM\"}]");
        Retorno<List<PartidaDoHistorico>> r = esperar(resposta -> cliente.historico(new Sessao("Ana", "t"), resposta));
        assertEquals("PILOTAGEM", r.valor().get(0).modo());
    }

    @Test
    void rankingEhLido() throws Exception {
        responder(200, "[{\"posicao\":1,\"nome\":\"Ana\",\"pontos\":60,\"dificuldade\":\"FACIL\",\"passageiros\":4,"
                + "\"tempoSegundos\":5,\"registradaEm\":\"2026-11-18T12:00:00Z\"},"
                + "{\"posicao\":2,\"nome\":\"Bia\",\"pontos\":45,\"dificuldade\":\"MEDIO\",\"passageiros\":5,"
                + "\"tempoSegundos\":80,\"registradaEm\":\"2026-11-17T09:30:00Z\"}]");
        Retorno<List<PosicaoGlobal>> r = esperar(cliente::ranking);
        assertEquals(List.of(new PosicaoGlobal(1, "Ana", 60, "FACIL", 4, 5, "2026-11-18T12:00:00Z"),
                new PosicaoGlobal(2, "Bia", 45, "MEDIO", 5, 80, "2026-11-17T09:30:00Z")), r.valor());
        assertEquals("GET /api/ranking", pedidos.get(0).metodo() + " " + pedidos.get(0).rota());
        assertNull(pedidos.get(0).autorizacao(), "o ranking é público");
    }

    @Test
    void historicoLevaOTokenELeALista() throws Exception {
        responder(200, "[{\"id\":9,\"resultado\":\"DERROTA\",\"dificuldade\":\"DIFICIL\",\"pontos\":0,"
                + "\"passageiros\":2,\"movimentos\":45,\"tempoSegundos\":0,\"registradaEm\":\"2026-11-18T12:00:00Z\"}]");
        Retorno<List<PartidaDoHistorico>> r = esperar(resposta ->
                cliente.historico(new Sessao("Ana", "tok-9"), resposta));
        assertEquals(List.of(new PartidaDoHistorico("DERROTA", "DIFICIL", 0, 2, 45, 0, "2026-11-18T12:00:00Z")),
                r.valor());
        assertEquals("Bearer tok-9", pedidos.get(0).autorizacao());
        assertEquals("/api/partidas/minhas", pedidos.get(0).rota());
    }

    @Test
    void sessaoNaoMostraOTokenEmTexto() {
        assertEquals("Sessao[nome=Ana, token=***]", new Sessao("Ana", "segredo-do-token").toString());
    }

    @Test
    void entrarSemValidadeLegivelAindaDevolveASessao() throws Exception {
        responder(200, "{\"token\":\"abc.def.ghi\",\"tipo\":\"Bearer\",\"expiraEm\":\"logo mais\","
                + "\"jogador\":{\"id\":1,\"nome\":\"Ana\",\"criadoEm\":\"2026-11-18T12:00:00Z\"}}");
        Retorno<Sessao> r = esperar(resposta -> cliente.entrar("ana", "segredo1", resposta));
        assertEquals(new Sessao("Ana", "abc.def.ghi", null), r.valor());
    }

    @Test
    void oEnderecoEhLidoACadaPedido() throws Exception {
        java.util.concurrent.atomic.AtomicReference<String> endereco = new java.util.concurrent.atomic.AtomicReference<>(
                "http://127.0.0.1:1");
        Net rede = new HeadlessNet(new HeadlessApplicationConfiguration());
        ServidorHttp configuravel = new ServidorHttp(endereco::get, () -> rede, Runnable::run);
        assertEquals(Falha.Motivo.SEM_CONEXAO,
                ServidorHttpTeste.<List<PosicaoGlobal>>esperar(configuravel::ranking).falha().motivo());
        endereco.set("http://127.0.0.1:" + servidor.getAddress().getPort());   // o jogador trocou o endereço
        responder(200, "[]");
        assertEquals(List.of(), ServidorHttpTeste.<List<PosicaoGlobal>>esperar(configuravel::ranking).valor());
    }
}
