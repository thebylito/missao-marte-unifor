package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.ServidorApplication;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

class RecuperacaoDosFechamentosTeste {

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(Instant.parse("2026-12-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void recuperaHordaESemanaAtrasadasAoSubir() {
        String url = "jdbc:h2:mem:recuperacao-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1" + ApiTeste.MODO_POSTGRESQL;
        try (var anterior = subir(url, false)) {
            Instant inicio = Instant.parse("2026-11-30T01:00:00Z");
            Horda horda = anterior.getBean(ServicoDeHordas.class).atual(inicio);
            JdbcTemplate jdbc = anterior.getBean(JdbcTemplate.class);
            jdbc.update("insert into jogadores (id, nome, criado_em, senha_hash) values (7, 'Ana', current_timestamp, 'x')");
            jdbc.update("""
                    insert into partidas (jogador_id, resultado, dificuldade, modo, pontos, passageiros, movimentos,
                        tempo_segundos, registrada_em, horda_numero)
                    values (7, 'VITORIA', ?, 'HORDA', 75, 3, 0, 60, ?, ?)
                    """, horda.descricao().dificuldade().name(), inicio.plusSeconds(60).atOffset(ZoneOffset.UTC),
                    horda.numero());
            assertThat(jdbc.queryForObject("select count(*) from moedas", Integer.class)).isZero();
        }
        try (var nova = subir(url, true)) {
            JdbcTemplate jdbc = nova.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForList("select referencia from moedas order by valor", String.class))
                    .containsExactly("horda:" + ServicoDeHordas.numeroEm(Instant.parse("2026-11-30T01:00:00Z")),
                            "semana:2026-11-23");
        }
    }

    private static ConfigurableApplicationContext subir(String url, boolean agendador) {
        return new SpringApplicationBuilder(ServidorApplication.class, RelogioFixo.class)
                .run("--spring.datasource.url=" + url, "--server.port=0",
                        "--missaomarte.agendador.habilitado=" + agendador);
    }
}
