package br.edu.unifor.missaomarte.servidor.evento;

import static br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo.AVISO_SEGUNDOS;
import static br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo.FOLGA_RESULTADO_SEGUNDOS;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.ranking.PlacarSemanal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeEventos {

    private final JdbcTemplate jdbc;
    private final ServicoDeHordas hordas;

    private Long numeroDaAgenda;
    private List<EventoAoVivo> agenda = List.of();
    private final Set<String> publicados = new HashSet<>();

    public ServicoDeEventos(JdbcTemplate jdbc, ServicoDeHordas hordas) {
        this.jdbc = jdbc;
        this.hordas = hordas;
    }

    /** Chamado quando esta instância tem pilotos conectados. Qualquer instância pode publicar o mesmo evento. */
    public synchronized void publicarAutomaticos(Instant agora) {
        long numero = ServicoDeHordas.numeroEm(agora);
        if (numeroDaAgenda == null || numeroDaAgenda != numero) {
            Horda horda = hordas.atual(agora);
            agenda = AgendaDeEventos.daHorda(horda.numero(), horda.inicio(), horda.semente());
            numeroDaAgenda = numero;
            publicados.clear();
        }
        for (EventoAoVivo evento : agenda) {
            if (!agora.isBefore(evento.aviso()) && agora.isBefore(evento.aviso().plusSeconds(1))
                    && !publicados.contains(evento.id())) {
                gravar(evento);
                publicados.add(evento.id());
            }
        }
    }

    public void solicitar(TipoDeEvento tipo, Instant agora) {
        long indice = jdbc.queryForObject("select nextval('seq_evento_manual')", Long.class);
        Instant inicio = agora.truncatedTo(ChronoUnit.MICROS).plusSeconds(AVISO_SEGUNDOS);
        gravar(EventoAoVivo.novo(ServicoDeHordas.numeroEm(agora), indice, tipo,
                ThreadLocalRandom.current().nextLong(), inicio));
    }

    private void gravar(EventoAoVivo evento) {
        jdbc.update("""
                insert into eventos_ao_vivo (evento, tipo, semente, inicio, fim) values (?, ?, ?, ?, ?)
                on conflict do nothing
                """, evento.id(), evento.tipo().name(), evento.semente(), utc(evento.inicio()), utc(evento.fim()));
    }

    public List<EventoAoVivo> ativos(Instant agora) {
        return jdbc.query("""
                select e.evento, e.tipo, e.semente, e.inicio, e.fim, j.nome as vencedor
                from eventos_ao_vivo e left join jogadores j on j.id = e.vip_jogador_id
                where e.inicio <= ? and e.fim >= ?
                order by e.inicio, e.evento
                """, (r, i) -> new EventoAoVivo(r.getString("evento"), TipoDeEvento.valueOf(r.getString("tipo")),
                r.getLong("semente"), r.getObject("inicio", OffsetDateTime.class).toInstant(),
                r.getObject("fim", OffsetDateTime.class).toInstant(), r.getString("vencedor")),
                utc(agora.plusSeconds(AVISO_SEGUNDOS)), utc(agora.minusSeconds(FOLGA_RESULTADO_SEGUNDOS)));
    }

    /** O canal já conferiu que a sessão recebeu o início. A chave do extrato elimina reenvios e sessões duplicadas. */
    public void sobreviver(EventoAoVivo evento, long jogador, Instant agora) {
        if (evento.tipo().perigo() && !agora.isBefore(evento.fim())
                && agora.isBefore(evento.fim().plusSeconds(FOLGA_RESULTADO_SEGUNDOS))) {
            pontuar(evento, jogador, 3);
        }
    }

    /** O UPDATE arbitra o primeiro VIP mesmo durante um deploy com duas instâncias. */
    @Transactional
    public Optional<String> pegarVip(EventoAoVivo evento, long jogador, Instant agora) {
        if (evento.tipo() != TipoDeEvento.PASSAGEIRO_VIP || agora.isBefore(evento.inicio())
                || !agora.isBefore(evento.fim())) {
            return Optional.empty();
        }
        if (jdbc.update("""
                update eventos_ao_vivo set vip_jogador_id = ? where evento = ? and vip_jogador_id is null
                """, jogador, evento.id()) == 0) {
            return Optional.empty();
        }
        pontuar(evento, jogador, 5);
        return Optional.of(jdbc.queryForObject("select nome from jogadores where id = ?", String.class, jogador));
    }

    private void pontuar(EventoAoVivo evento, long jogador, int pontos) {
        jdbc.update("""
                insert into pontos_de_evento (jogador_id, evento, tipo, pontos, semana) values (?, ?, ?, ?, ?)
                on conflict do nothing
                """, jogador, evento.id(), evento.tipo().name(), pontos, PlacarSemanal.semanaDe(evento.inicio()));
    }

    private static OffsetDateTime utc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
