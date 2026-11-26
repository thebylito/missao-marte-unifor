package br.edu.unifor.missaomarte.servidor.ranking;

import static br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas.FORTALEZA;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PlacarSemanal {

    private final JdbcTemplate jdbc;

    public PlacarSemanal(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static LocalDate semanaDe(Instant instante) {
        return ZonedDateTime.ofInstant(instante, FORTALEZA).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static Instant inicioDe(LocalDate segunda) {
        return segunda.atStartOfDay(FORTALEZA).toInstant();
    }

    public Placar placarDe(LocalDate semana) {
        List<Faixa> faixas = faixasDaSemana(semana);
        List<Posicao> posicoes = new ArrayList<>(faixas.size());
        for (Faixa faixa : faixas) {
            posicoes.add(new Posicao(posicoes.size() + 1, faixa.nome(), faixa.pontos(), faixa.hordas(), faixa.eventos()));
        }
        return new Placar(semana, posicoes);
    }

    public List<Faixa> faixasDaSemana(LocalDate semana) {
        return jdbc.query("""
                select j.id, j.nome, sum(c.pontos) as total, sum(c.hordas) as hordas, sum(c.eventos) as eventos
                from (
                    select t.jogador_id, t.pontos_semanais as pontos, 1 as hordas, 0 as eventos
                    from top_da_horda t join hordas h on h.numero = t.horda_numero where h.semana = ?
                    union all
                    select jogador_id, pontos, 0, 1 from pontos_de_evento where semana = ?
                ) c join jogadores j on j.id = c.jogador_id
                group by j.id, j.nome
                order by total desc, j.id
                """, (r, i) -> new Faixa(r.getLong("id"), r.getString("nome"), r.getLong("total"),
                r.getInt("hordas"), r.getInt("eventos")), semana, semana);
    }

    public record Faixa(long jogadorId, String nome, long pontos, int hordas, int eventos) {}
    public record Posicao(int posicao, String nome, long pontos, int hordas, int eventos) {}
    public record Placar(LocalDate semana, List<Posicao> posicoes) {}
}
