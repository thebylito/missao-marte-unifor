package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.Agendador;
import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FechamentosTeste extends ApiTeste {

    @Autowired JdbcTemplate jdbc;
    @Autowired ServicoDeHordas hordas;
    @Autowired Agendador agendador;

    @Test
    void fechaTop10PremiaUmaVezESomaHordasEEventosNaSemana() throws Exception {
        relogio.agora = Instant.parse("2026-11-30T01:10:00Z"); // domingo, 22:10 em Fortaleza
        Horda ultima = hordas.atual(relogio.instant());
        Horda penultima = hordas.atual(relogio.instant().minusSeconds(7200));
        for (int i = 1; i <= 11; i++) {
            vitoria(ultima, jogador("Piloto" + i), 76 - i);
        }
        long primeiro = jdbc.queryForObject("select id from jogadores where nome = 'Piloto1'", Long.class);
        long segundo = jdbc.queryForObject("select id from jogadores where nome = 'Piloto2'", Long.class);
        vitoria(ultima, primeiro, 70); // só a melhor vitória
        vitoria(penultima, segundo, 60);
        pontuarEvento(primeiro);

        relogio.agora = Instant.parse("2026-11-30T03:29:59Z");
        agendador.verificar();
        assertThat(jdbc.queryForList("select horda_numero from top_da_horda", Long.class))
                .containsExactly(penultima.numero());
        assertThat(jdbc.queryForObject("select count(*) from semanas_fechadas where semana = '2026-11-23'", Integer.class))
                .isZero();

        relogio.agora = Instant.parse("2026-11-30T03:30:00Z");
        agendador.verificar();
        agendador.verificar();
        assertThat(jdbc.queryForList("select posicao from top_da_horda where horda_numero = ? order by posicao",
                Integer.class, ultima.numero())).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(jdbc.queryForList("select m.valor from moedas m join top_da_horda t on t.jogador_id = m.jogador_id "
                + "where t.horda_numero = ? and m.origem = 'HORDA' and m.referencia = ? order by t.posicao",
                Integer.class, ultima.numero(), "horda:" + ultima.numero()))
                .containsExactly(20, 15, 10, 5, 5, 5, 5, 5, 5, 5);
        assertThat(jdbc.queryForList("select valor from moedas where origem = 'SEMANA' "
                + "and referencia = 'semana:2026-11-23' order by valor desc", Integer.class)).containsExactly(50, 30, 20);
        assertThat(jdbc.queryForObject("select valor from moedas where jogador_id = ? and origem = 'SEMANA'",
                Integer.class, segundo)).isEqualTo(50);
        assertThat(jdbc.queryForObject("select count(*) from semanas_fechadas where semana = '2026-11-23'", Integer.class))
                .isEqualTo(1);
        mvc.perform(get("/api/ranking/semanal")).andExpect(status().isOk())
                .andExpect(jsonPath("$.semana").value("2026-11-30"))
                .andExpect(jsonPath("$.posicoes").isEmpty());

        relogio.agora = Instant.parse("2026-11-30T02:59:59Z");
        mvc.perform(get("/api/ranking/semanal")).andExpect(status().isOk())
                .andExpect(jsonPath("$.semana").value("2026-11-23"))
                .andExpect(jsonPath("$.posicoes[0].nome").value("Piloto2"))
                .andExpect(jsonPath("$.posicoes[0].pontos").value(18))
                .andExpect(jsonPath("$.posicoes[0].hordas").value(2))
                .andExpect(jsonPath("$.posicoes[0].eventos").value(0))
                .andExpect(jsonPath("$.posicoes[1].nome").value("Piloto1"))
                .andExpect(jsonPath("$.posicoes[1].pontos").value(13))
                .andExpect(jsonPath("$.posicoes[1].eventos").value(1));
    }

    private long jogador(String nome) {
        jdbc.update("insert into jogadores (nome, criado_em, senha_hash) values (?, current_timestamp, 'x')", nome);
        return jdbc.queryForObject("select id from jogadores where nome = ?", Long.class, nome);
    }

    private void vitoria(Horda horda, long jogador, int pontos) {
        jdbc.update("""
                insert into partidas (jogador_id, resultado, dificuldade, modo, pontos, passageiros, movimentos,
                    tempo_segundos, registrada_em, horda_numero)
                values (?, 'VITORIA', ?, 'HORDA', ?, 3, 0, 60, ?, ?)
                """, jogador, horda.descricao().dificuldade().name(), pontos,
                horda.inicio().plusSeconds(120).atOffset(ZoneOffset.UTC), horda.numero());
    }

    private void pontuarEvento(long jogador) {
        OffsetDateTime inicio = OffsetDateTime.parse("2026-11-29T21:00:00Z");
        jdbc.update("""
                insert into eventos_ao_vivo (evento, tipo, semente, inicio, fim)
                values ('e-1-1', 'TEMPESTADE', 1, ?, ?)
                """, inicio, inicio.plusSeconds(25));
        jdbc.update("""
                insert into pontos_de_evento (jogador_id, evento, tipo, pontos, semana)
                values (?, 'e-1-1', 'TEMPESTADE', 3, date '2026-11-23')
                """, jogador);
    }
}
