package br.edu.unifor.missaomarte.servidor.banco;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fase 8 (DA-41): o servidor inteiro sobre um PostgreSQL de verdade (o mesmo 17 da publicação), num contêiner. A
 * migração V1 roda, o Hibernate confere o esquema ({@code ddl-auto=validate}) e a API funciona. Sem Docker, os
 * testes são pulados; os outros testes da API usam o H2 em modo PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
class EsquemaNoPostgresTeste extends ApiTeste {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void oBancoEOPostgresComAMigracaoV1() {
        assertEquals("PostgreSQL", jdbc.queryForObject("select split_part(version(), ' ', 1)", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class));
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
}
