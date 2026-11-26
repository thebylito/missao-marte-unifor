package br.edu.unifor.missaomarte.servidor.banco;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.servidor.ServidorApplication;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fase 8 (DA-41): a virada de produção, de ponta a ponta. O servidor sobe com um PostgreSQL vazio e
 * {@code MISSAOMARTE_IMPORTAR_H2} apontando para um H2 da Fase 7 (colunas ENUM, como em produção), e tudo continua
 * igual: o ranking dos dois modos, o histórico, o login com a senha de sempre. A importação termina antes de o
 * servidor atender o primeiro pedido, e uma segunda subida não duplica nada.
 */
@Testcontainers(disabledWithoutDocker = true)
class ImportacaoDoH2NoPostgresTeste {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @TempDir
    static Path pasta;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static Path origem;
    private static ConfigurableApplicationContext servidor;
    private static QuandoOServidorComecaAAtender primeiraSubida;

    /** Conta as linhas no instante em que o Tomcat começa a aceitar conexões. */
    static final class QuandoOServidorComecaAAtender implements ApplicationListener<WebServerInitializedEvent> {
        volatile Integer jogadores;
        volatile Integer partidas;

        @Override
        public void onApplicationEvent(WebServerInitializedEvent evento) {
            JdbcTemplate jdbc = new JdbcTemplate(evento.getApplicationContext().getBean(DataSource.class));
            jogadores = jdbc.queryForObject("select count(*) from jogadores", Integer.class);
            partidas = jdbc.queryForObject("select count(*) from partidas", Integer.class);
        }
    }

    @BeforeAll
    static void primeiraSubida() throws Exception {
        origem = BancoDaFase7.criar(pasta);
        primeiraSubida = new QuandoOServidorComecaAAtender();
        servidor = subir(primeiraSubida);
    }

    @AfterAll
    static void parar() {
        if (servidor != null) {
            servidor.close();
        }
    }

    /** Sobe o servidor como em produção: as variáveis de ambiente entram como argumentos (--NOME=valor). */
    private static ConfigurableApplicationContext subir(QuandoOServidorComecaAAtender ouvinte) {
        return new SpringApplicationBuilder(ServidorApplication.class)
                .listeners(ouvinte)
                .run("--server.port=0",
                        "--MISSAOMARTE_DB_URL=" + POSTGRES.getJdbcUrl(),
                        "--MISSAOMARTE_DB_USUARIO=" + POSTGRES.getUsername(),
                        "--MISSAOMARTE_DB_SENHA=" + POSTGRES.getPassword(),
                        "--MISSAOMARTE_IMPORTAR_H2=" + origem,
                        "--MISSAOMARTE_JWT_SEGREDO=segredo-so-para-os-testes-com-32-bytes-ou-mais");
    }

    @Test
    void aImportacaoTerminaAntesDeOServidorAtender() {
        assertEquals(BancoDaFase7.JOGADORES, primeiraSubida.jogadores);
        assertEquals(BancoDaFase7.PARTIDAS, primeiraSubida.partidas);
    }

    @Test
    void jogadoresImportadosRecebemSkinPadraoESaldoZero() {
        JdbcTemplate jdbc = new JdbcTemplate(servidor.getBean(DataSource.class));
        assertEquals(List.of("padrao"), jdbc.queryForList("select distinct skin from jogadores", String.class));
        assertEquals(0, jdbc.queryForObject("select coalesce(sum(valor), 0) from moedas", Integer.class));
    }

    @Test
    void oRankingClassicoEODaFase7() throws Exception {
        Instant veterana = registradaEmNaOrigem(1);
        assertEquals(List.of(
                "1 Comandante 80 FACIL 4 33 " + Instant.parse("2026-11-21T12:00:00.500Z"),
                "2 Cadete 50 DIFICIL 4 75 " + Instant.parse("2026-11-10T09:00:00Z"),
                "3 Veterana 50 MEDIO 3 40 " + veterana), ranking(""));
    }

    @Test
    void oRankingDaPilotagemEODaFase7() throws Exception {
        Instant veterana = registradaEmNaOrigem(2);
        assertEquals(List.of(
                "1 Comandante 60 MEDIO 3 95 " + Instant.parse("2026-11-21T12:30:00Z"),
                "2 Veterana 60 DIFICIL 3 130 " + veterana), ranking("?modo=PILOTAGEM"));
    }

    @Test
    void aComandanteEntraComASenhaDeSempreEVeOHistorico() throws Exception {
        String token = entrar(BancoDaFase7.COMANDANTE, BancoDaFase7.SENHA_DA_COMANDANTE);

        String corpo = pedir(HttpRequest.newBuilder(endereco("/api/partidas/minhas"))
                .header("Authorization", "Bearer " + token), 200);
        List<Map<String, Object>> partidas = JsonPath.read(corpo, "$");
        List<String> historico = new ArrayList<>();
        for (Map<String, Object> p : partidas) {
            historico.add(p.get("id") + " " + p.get("resultado") + " " + p.get("dificuldade") + " " + p.get("modo")
                    + " " + p.get("pontos") + " " + p.get("passageiros") + " " + p.get("movimentos") + " "
                    + p.get("tempoSegundos") + " " + Instant.parse((String) p.get("registradaEm")));
        }
        assertEquals(List.of(
                "20 ABORTADA MEDIO PILOTAGEM 0 0 0 12 2026-11-23T07:45:00Z",
                "15 DERROTA DIFICIL CLASSICO 999 1 40 200 2026-11-22T18:00:00Z",
                "12 VITORIA MEDIO PILOTAGEM 60 3 0 95 2026-11-21T12:30:00Z",
                "11 VITORIA FACIL CLASSICO 80 4 18 33 2026-11-21T12:00:00.500Z"), historico);
        String perfil = pedir(HttpRequest.newBuilder(endereco("/api/jogadores/eu"))
                .header("Authorization", "Bearer " + token), 200);
        assertEquals(7, (int) JsonPath.read(perfil, "$.id"));
        assertEquals(Instant.parse("2026-11-20T10:00:00.123456Z"),
                Instant.parse(JsonPath.read(perfil, "$.criadoEm")));
    }

    @Test
    void umCadastroNovoRecebeOMaiorIdMaisUm() throws Exception {
        String corpo = pedir(json("/api/jogadores", "{\"nome\":\"Novata\",\"senha\":\"senha-da-novata\"}"), 201);
        assertEquals(BancoDaFase7.MAIOR_ID_DE_JOGADOR + 1, ((Number) JsonPath.read(corpo, "$.id")).longValue());

        String token = entrar("Novata", "senha-da-novata");
        // uma derrota: não entra no ranking, que os outros testes conferem
        String partida = pedir(json("/api/partidas", "{\"resultado\":\"DERROTA\",\"dificuldade\":\"FACIL\","
                + "\"pontos\":10,\"passageiros\":1,\"movimentos\":9,\"tempoSegundos\":20}")
                .header("Authorization", "Bearer " + token), 201);
        assertEquals(BancoDaFase7.MAIOR_ID_DE_PARTIDA + 1, ((Number) JsonPath.read(partida, "$.id")).longValue());
    }

    @Test
    void umaSegundaSubidaNaoDuplicaNada() throws Exception {
        List<String> classicoAntes = ranking("");
        List<String> pilotagemAntes = ranking("?modo=PILOTAGEM");
        JdbcTemplate jdbc = new JdbcTemplate(servidor.getBean(DataSource.class));
        Integer jogadores = jdbc.queryForObject("select count(*) from jogadores", Integer.class);
        Integer partidas = jdbc.queryForObject("select count(*) from partidas", Integer.class);

        QuandoOServidorComecaAAtender segunda = new QuandoOServidorComecaAAtender();
        try (ConfigurableApplicationContext outro = subir(segunda)) {
            assertEquals(jogadores, segunda.jogadores);
            assertEquals(partidas, segunda.partidas);
        }
        assertEquals(jogadores, jdbc.queryForObject("select count(*) from jogadores", Integer.class));
        assertEquals(partidas, jdbc.queryForObject("select count(*) from partidas", Integer.class));
        assertEquals(classicoAntes, ranking(""));
        assertEquals(pilotagemAntes, ranking("?modo=PILOTAGEM"));
    }

    private static List<String> ranking(String parametros) throws Exception {
        List<Map<String, Object>> linhas = JsonPath.read(
                pedir(HttpRequest.newBuilder(endereco("/api/ranking" + parametros)), 200), "$");
        List<String> ranking = new ArrayList<>();
        for (Map<String, Object> l : linhas) {
            ranking.add(l.get("posicao") + " " + l.get("nome") + " " + l.get("pontos") + " " + l.get("dificuldade")
                    + " " + l.get("passageiros") + " " + l.get("tempoSegundos") + " "
                    + Instant.parse((String) l.get("registradaEm")));
        }
        return ranking;
    }

    private static String entrar(String nome, String senha) throws Exception {
        String corpo = pedir(json("/api/sessoes", "{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}"), 200);
        return JsonPath.read(corpo, "$.token");
    }

    private static HttpRequest.Builder json(String rota, String corpo) {
        return HttpRequest.newBuilder(endereco(rota)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo));
    }

    private static String pedir(HttpRequest.Builder pedido, int esperado) throws Exception {
        HttpResponse<String> resposta = HTTP.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(esperado, resposta.statusCode(), resposta.body());
        return resposta.body();
    }

    private static URI endereco(String rota) {
        return URI.create("http://localhost:" + servidor.getEnvironment().getProperty("local.server.port") + rota);
    }

    /** A data de uma partida da "Veterana", gravada pelo esquema-fase-7.sql com CURRENT_TIMESTAMP. */
    private static Instant registradaEmNaOrigem(long partida) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + origem + ";ACCESS_MODE_DATA=r", "sa", "");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("select registrada_em from partidas where id = " + partida)) {
            r.next();
            return r.getObject(1, java.time.OffsetDateTime.class).toInstant();
        }
    }
}
