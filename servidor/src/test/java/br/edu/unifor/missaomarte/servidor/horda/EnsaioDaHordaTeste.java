package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "missaomarte.hordas.ensaio=poco-polar")
class EnsaioDaHordaTeste extends ApiTeste {

    @Autowired JdbcTemplate jdbc;

    @Test
    void usaCavernaNaoLiberadaSemLerNemGravarAHorda() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T21:10:00Z");
        jdbc.update("insert into hordas (numero, inicio, fim, semente, caverna, dificuldade, descricao) "
                + "values (3957, current_timestamp, current_timestamp, 0, 'marciana', 'DIFICIL', '{}')");
        mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(3957))
                .andExpect(jsonPath("$.caverna.id").value("poco-polar"))
                .andExpect(jsonPath("$.descricao.caverna.liberada").value(false));
        assertThat(jdbc.queryForList("select caverna from hordas", String.class)).containsExactly("marciana");
    }
}
