package br.edu.unifor.missaomarte.servidor.ranking;

import static br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas.TOLERANCIA;

import br.edu.unifor.missaomarte.servidor.moedas.Origem;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import br.edu.unifor.missaomarte.servidor.partida.Modo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicoDeFechamentos {

    private static final int[] PONTOS_HORDA = {10, 8, 6, 5, 4, 3, 2, 1, 1, 1};
    private static final int[] MOEDAS_HORDA = {20, 15, 10, 5, 5, 5, 5, 5, 5, 5};
    private static final int[] MOEDAS_SEMANA = {50, 30, 20};

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final ServicoDeRanking ranking;
    private final PlacarSemanal placar;
    private final ServicoDeMoedas moedas;

    public ServicoDeFechamentos(JdbcTemplate jdbc, PlatformTransactionManager gerenciador,
                                 ServicoDeRanking ranking, PlacarSemanal placar, ServicoDeMoedas moedas) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenciador);
        this.ranking = ranking;
        this.placar = placar;
        this.moedas = moedas;
    }

    public void verificar(Instant agora) {
        List<Long> pendentes = jdbc.queryForList("""
                select numero from hordas where fechada_em is null and fim <= ? order by fim
                """, Long.class, utc(agora.minus(TOLERANCIA)));
        for (long numero : pendentes) {
            transacao.executeWithoutResult(status -> fecharHorda(numero, agora));
        }

        LocalDate ultimaVencida = PlacarSemanal.semanaDe(agora).minusWeeks(1);
        if (agora.isBefore(PlacarSemanal.inicioDe(ultimaVencida.plusWeeks(1)).plus(TOLERANCIA))) {
            ultimaVencida = ultimaVencida.minusWeeks(1);
        }
        List<LocalDate> semanas = jdbc.queryForList("""
                select atividade.semana from (
                    select semana from hordas where semana <= ?
                    union
                    select semana from pontos_de_evento where semana <= ?
                    union
                    select cast(? as date)
                ) atividade
                where not exists (select 1 from semanas_fechadas f where f.semana = atividade.semana)
                order by atividade.semana
                """, LocalDate.class, ultimaVencida, ultimaVencida, ultimaVencida);
        for (LocalDate semana : semanas) {
            transacao.executeWithoutResult(status -> fecharSemana(semana, agora));
        }
    }

    private void fecharHorda(long numero, Instant agora) {
        List<OffsetDateTime> fins = jdbc.queryForList("""
                select fim from hordas where numero = ? and fechada_em is null for update
                """, OffsetDateTime.class, numero);
        if (fins.isEmpty()) {
            return; // outra instância já fechou
        }
        for (var vitoria : ranking.melhores(Modo.HORDA, numero)) {
            int posicao = vitoria.posicao();
            long jogador = vitoria.partida().getJogador().getId();
            jdbc.update("""
                    insert into top_da_horda (horda_numero, posicao, jogador_id, partida_id, pontos_semanais)
                    values (?, ?, ?, ?, ?)
                    """, numero, posicao, jogador, vitoria.partida().getId(), PONTOS_HORDA[posicao - 1]);
            moedas.creditar(jogador, Origem.HORDA, "horda:" + numero, MOEDAS_HORDA[posicao - 1]);
        }
        // A horda de domingo às 22h acaba na virada, mas é a última da semana de domingo.
        LocalDate semana = PlacarSemanal.semanaDe(fins.get(0).toInstant().minusNanos(1));
        jdbc.update("update hordas set fechada_em = ?, semana = ? where numero = ?", utc(agora), semana, numero);
    }

    private void fecharSemana(LocalDate semana, Instant agora) {
        Integer faltam = jdbc.queryForObject("""
                select count(*) from hordas where fechada_em is null and fim > ? and fim <= ?
                """, Integer.class, utc(PlacarSemanal.inicioDe(semana)), utc(PlacarSemanal.inicioDe(semana.plusWeeks(1))));
        if (faltam != 0 || jdbc.update("""
                insert into semanas_fechadas (semana, fechada_em) values (?, ?) on conflict do nothing
                """, semana, utc(agora)) == 0) {
            return;
        }
        var faixas = placar.faixasDaSemana(semana);
        for (int i = 0; i < Math.min(faixas.size(), MOEDAS_SEMANA.length); i++) {
            moedas.creditar(faixas.get(i).jogadorId(), Origem.SEMANA, "semana:" + semana, MOEDAS_SEMANA[i]);
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }
}
