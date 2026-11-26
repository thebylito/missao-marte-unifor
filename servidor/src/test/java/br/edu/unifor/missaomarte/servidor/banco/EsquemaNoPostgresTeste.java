package br.edu.unifor.missaomarte.servidor.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.Agendador;
import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo;
import br.edu.unifor.missaomarte.servidor.evento.ServicoDeEventos;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fase 8 (DA-41): o servidor inteiro sobre um PostgreSQL de verdade (o mesmo 18 da publicação), num contêiner. A
 * sequência do Flyway roda, o Hibernate confere o esquema ({@code ddl-auto=validate}) e a API funciona. Sem Docker, os
 * testes são pulados; os outros testes da API usam o H2 em modo PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
class EsquemaNoPostgresTeste extends ApiTeste {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicoDeHordas hordas;

    @Autowired
    private Agendador agendador;

    @Autowired
    private ServicoDeEventos eventos;

    @Test
    void oBancoEOPostgresComAsMigracoesDoFlyway() {
        assertEquals("PostgreSQL", jdbc.queryForObject("select split_part(version(), ' ', 1)", String.class));
        assertEquals(List.of("1", "2", "3", "4", "5"), jdbc.queryForList(
                "select version from flyway_schema_history where version is not null and success order by installed_rank",
                String.class));
    }

    @Test
    void asColunasDeEnumSaoTextoSemListaFechada() {
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from pg_constraint where conrelid = 'partidas'::regclass and contype = 'c'",
                Integer.class));
        for (String coluna : new String[] {"modo", "resultado", "dificuldade"}) {
            assertEquals("character varying(20)", jdbc.queryForObject("select format_type(atttypid, atttypmod) "
                    + "from pg_attribute where attrelid = 'partidas'::regclass and attname = ?", String.class, coluna),
                    coluna);
        }
        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (now(), 'Novata', 'x')");
        Long jogador = jdbc.queryForObject("select id from jogadores where nome = 'Novata'", Long.class);
        assertDoesNotThrow(() -> jdbc.update("insert into partidas(dificuldade, modo, movimentos, passageiros, "
                + "pontos, registrada_em, resultado, tempo_segundos, jogador_id) values "
                + "('MEDIO', 'HORDA', 1, 0, 0, now(), 'DERROTA', 1, ?)", jogador));
    }

    @Test
    void semOModoAPartidaEClassica() {
        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (now(), 'Antiga', 'x')");
        Long jogador = jdbc.queryForObject("select id from jogadores where nome = 'Antiga'", Long.class);
        jdbc.update("insert into partidas(dificuldade, movimentos, passageiros, pontos, registrada_em, resultado, "
                + "tempo_segundos, jogador_id) values ('FACIL', 1, 0, 0, now(), 'VITORIA', 1, ?)", jogador);
        assertEquals("CLASSICO", jdbc.queryForObject("select modo from partidas where jogador_id = ?", String.class,
                jogador));
    }

    @Test
    void oNomeContinuaUnicoSemDiferenciarMaiusculas() throws Exception {
        cadastrar("Ana", "senha-123").andExpect(status().isCreated());
        cadastrar("ANA", "senha-456").andExpect(status().isConflict());
    }

    @Test
    void rankingEHistoricoFuncionamNoPostgres() throws Exception {
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        registrar(bia, partida("VITORIA", "FACIL", 50)).andExpect(status().isCreated());
        relogio.avancar(Duration.ofMinutes(5));
        registrar(ana, partida("VITORIA", "FACIL", 50)).andExpect(status().isCreated());
        registrar(ana, partida("DERROTA", "DIFICIL", 90)).andExpect(status().isCreated());
        registrar(ana, "{\"resultado\":\"VITORIA\",\"dificuldade\":\"DIFICIL\",\"pontos\":70,\"passageiros\":3,"
                + "\"movimentos\":5,\"tempoSegundos\":130,\"modo\":\"PILOTAGEM\"}").andExpect(status().isCreated());

        mvc.perform(get("/api/ranking"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Bia"))
                .andExpect(jsonPath("$[1].nome").value("Ana"));
        mvc.perform(get("/api/ranking").param("modo", "PILOTAGEM"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].pontos").value(70));
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].modo").value("PILOTAGEM"))
                .andExpect(jsonPath("$[2].pontos").value(50));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publicaRegistraEFechaNoPostgresComTransacoesConcorrentes() throws Exception {
        relogio.agora = Instant.parse("2026-11-30T01:05:00Z");
        var publicadas = emParalelo(() -> hordas.atual(relogio.instant()), () -> hordas.atual(relogio.instant()));
        var horda = publicadas.get(0);
        assertThat(publicadas.get(1)).isEqualTo(horda);
        assertThat(jdbc.queryForObject("select count(*) from hordas where numero = ?", Integer.class, horda.numero()))
                .isEqualTo(1);

        String token = novoJogador("HordaNoPostgres");
        String dificuldade = horda.descricao().dificuldade().name();
        String emissao = mvc.perform(post("/api/partidas/inicio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"modo":"HORDA","dificuldade":"%s","horda":%d}
                                """.formatted(dificuldade, horda.numero())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bilhete = JsonPath.read(emissao, "$.bilhete");
        relogio.avancar(Duration.ofSeconds(45).plusNanos(123));
        String partida = """
                {"modo":"HORDA","dificuldade":"%s","horda":%d,"bilhete":"%s","resultado":"VITORIA",
                 "pontos":75,"passageiros":3,"movimentos":0,"tempoSegundos":45}
                """.formatted(dificuldade, horda.numero(), bilhete);
        Callable<String> registrarHorda = () -> registrar(token, partida).andExpect(status().isCreated())
                .andExpect(jsonPath("$.moedasGanhas").value(5)).andExpect(jsonPath("$.colocacao").value(1))
                .andReturn().getResponse().getContentAsString();
        var registros = emParalelo(registrarHorda, registrarHorda);
        assertThat(registros.get(0)).isEqualTo(registros.get(1));
        assertThat(jdbc.queryForObject("select count(*) from partidas where horda_numero = ?", Integer.class,
                horda.numero())).isEqualTo(1);

        relogio.agora = Instant.parse("2026-11-30T03:30:00Z");
        Callable<Boolean> fechar = () -> {
            agendador.verificar();
            return true;
        };
        emParalelo(fechar, fechar);
        assertThat(registrarHorda.call()).isEqualTo(registros.get(0));
        assertThat(jdbc.queryForList("select posicao from top_da_horda where horda_numero = ?",
                Integer.class, horda.numero())).containsExactly(1);
        assertThat(jdbc.queryForList("select valor from moedas m join jogadores j on j.id = m.jogador_id "
                + "where j.nome = 'HordaNoPostgres' order by m.valor", Integer.class)).containsExactly(5, 20, 50);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void arbitraUmVipComPedidosConcorrentesNoPostgres() throws Exception {
        Instant inicio = Instant.parse("2026-11-18T17:00:00Z");
        var evento = EventoAoVivo.novo(ServicoDeHordas.numeroEm(inicio), 99, TipoDeEvento.PASSAGEIRO_VIP, 0, inicio);
        jdbc.update("""
                insert into eventos_ao_vivo (evento, tipo, semente, inicio, fim)
                values (?, ?, ?, ?, ?)
                """, evento.id(), evento.tipo().name(), evento.semente(), inicio.atOffset(ZoneOffset.UTC),
                evento.fim().atOffset(ZoneOffset.UTC));
        long primeiro = jogadorSemSenha("PretendenteA");
        long segundo = jogadorSemSenha("PretendenteB");
        Instant podePegar = inicio.plusSeconds(3);
        var resultados = emParalelo(() -> eventos.pegarVip(evento, primeiro, podePegar),
                () -> eventos.pegarVip(evento, segundo, podePegar));

        assertThat(resultados.stream().flatMap(java.util.Optional::stream).toList()).hasSize(1);
        assertThat(jdbc.queryForList("select pontos from pontos_de_evento where evento = ?", Integer.class, evento.id()))
                .containsExactly(5);
    }

    private long jogadorSemSenha(String nome) {
        jdbc.update("insert into jogadores (criado_em, nome, senha_hash) values (now(), ?, 'x')", nome);
        return jdbc.queryForObject("select id from jogadores where nome = ?", Long.class, nome);
    }

    private static <T> List<T> emParalelo(Callable<T> a, Callable<T> b) throws Exception {
        var barreira = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> {
                barreira.await(5, TimeUnit.SECONDS);
                return a.call();
            });
            var segunda = executor.submit(() -> {
                barreira.await(5, TimeUnit.SECONDS);
                return b.call();
            });
            return List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
