package br.edu.unifor.missaomarte.servidor.partida;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fase 8 (DA-41): num banco novo, o esquema é o da migração {@code V1} do Flyway, e não o que o Hibernate criaria.
 * As colunas de enum são texto sem lista fechada de valores (a intenção da DA-40): um modo novo (a Horda) cabe num
 * banco criado do zero. O mesmo esquema é conferido no PostgreSQL de verdade em {@code EsquemaNoPostgresTeste}.
 */
class EsquemaNovoTeste extends ApiTeste {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void oEsquemaVemDasMigracoesDoFlyway() {
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class));
    }

    @Test
    void aTabelaDePartidasNaoTemListaFechadaDeValores() {
        Integer restricoes = jdbc.queryForObject("select count(*) from information_schema.table_constraints "
                + "where table_schema = 'public' and table_name = 'partidas' and constraint_type = 'CHECK'",
                Integer.class);
        assertEquals(0, restricoes);
    }

    @Test
    void asColunasDeEnumSaoTextoDe20() {
        for (String coluna : List.of("modo", "resultado", "dificuldade")) {
            assertEquals("character varying", jdbc.queryForObject("select lower(data_type) from "
                    + "information_schema.columns where table_name = 'partidas' and column_name = ?", String.class,
                    coluna), coluna);
            assertEquals(20, jdbc.queryForObject("select character_maximum_length from information_schema.columns "
                    + "where table_name = 'partidas' and column_name = ?", Integer.class, coluna), coluna);
        }
    }

    @Test
    void umModoNovoCabeNumBancoNovo() {
        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (current_timestamp, 'Novata', 'x')");
        Long jogador = jdbc.queryForObject("select id from jogadores where nome = 'Novata'", Long.class);
        assertDoesNotThrow(() -> jdbc.update("insert into partidas(dificuldade, modo, movimentos, passageiros, "
                + "pontos, registrada_em, resultado, tempo_segundos, jogador_id) values "
                + "('MEDIO', 'HORDA', 1, 0, 0, current_timestamp, 'DERROTA', 1, ?)", jogador));
    }

    @Test
    void semOModoAPartidaEClassica() {
        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (current_timestamp, 'Antiga', 'x')");
        Long jogador = jdbc.queryForObject("select id from jogadores where nome = 'Antiga'", Long.class);
        jdbc.update("insert into partidas(dificuldade, movimentos, passageiros, pontos, registrada_em, resultado, "
                + "tempo_segundos, jogador_id) values ('FACIL', 1, 0, 0, current_timestamp, 'VITORIA', 1, ?)", jogador);
        assertEquals("CLASSICO", jdbc.queryForObject("select modo from partidas where jogador_id = ?", String.class,
                jogador));
    }
}
