package br.edu.unifor.missaomarte.servidor.banco;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigracaoDaHordaTeste {

    @Test
    void v5PreservaHordasDaV4EAceitaAInsercaoDaVersaoAnterior() {
        var dados = new DriverManagerDataSource(
                "jdbc:h2:mem:expansao-da-horda;DB_CLOSE_DELAY=-1" + ApiTeste.MODO_POSTGRESQL, "sa", "");
        var jdbc = new JdbcTemplate(dados);
        Flyway.configure().dataSource(dados).target("4").load().migrate();

        String insercao = """
                insert into hordas (numero, inicio, fim, semente, caverna, dificuldade, descricao)
                values (?, current_timestamp, current_timestamp, 42, 'marciana', 'MEDIO', '{}')
                """;
        jdbc.update(insercao, 1L);
        Flyway.configure().dataSource(dados).load().migrate();
        jdbc.update(insercao, 2L);

        assertThat(jdbc.queryForList("select semente_agenda from hordas order by numero", String.class))
                .containsExactly(null, null);
        assertThat(jdbc.queryForList("select semente from hordas order by numero", Long.class))
                .containsExactly(42L, 42L);
    }
}
