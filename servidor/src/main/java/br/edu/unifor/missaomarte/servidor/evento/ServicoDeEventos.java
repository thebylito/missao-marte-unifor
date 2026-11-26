package br.edu.unifor.missaomarte.servidor.evento;

import static br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo.AVISO_SEGUNDOS;
import static br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo.FOLGA_RESULTADO_SEGUNDOS;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.ranking.PlacarSemanal;
import br.edu.unifor.missaomarte.servidor.seguranca.Sementes;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeEventos {

    private final SecureRandom sorteioManual = new SecureRandom();
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
            agenda = AgendaDeEventos.daHorda(horda);
            numeroDaAgenda = numero;
            publicados.clear();
        }
        for (EventoAoVivo evento : agenda) {
            if (!agora.isBefore(evento.aviso()) && agora.isBefore(evento.inicio())
                    && !publicados.contains(evento.id())) {
                gravar(evento);
                publicados.add(evento.id());
            }
        }
    }

    /** O evento do painel dura o que os ajustes gravados na horda atual dizem (DA-48), como os automáticos. */
    public void solicitar(TipoDeEvento tipo, Instant agora) {
        long duracao = (long) hordas.atual(agora).descricao().ajustes().eventos().duracao(tipo);
        long indice = jdbc.queryForObject("select nextval('seq_evento_manual')", Long.class);
        Instant inicio = agora.truncatedTo(ChronoUnit.MICROS).plusSeconds(AVISO_SEGUNDOS);
        String vip = null;
        if (tipo == TipoDeEvento.PASSAGEIRO_VIP) {
            byte[] chave = new byte[32];
            sorteioManual.nextBytes(chave);
            vip = Sementes.emHex(chave);
        }
        gravar(EventoAoVivo.novo(ServicoDeHordas.numeroEm(agora), indice, tipo, sorteioManual.nextLong(), inicio, vip,
                duracao));
    }

    private void gravar(EventoAoVivo evento) {
        jdbc.update("""
                insert into eventos_ao_vivo (evento, tipo, semente, inicio, fim, semente_vip) values (?, ?, ?, ?, ?, ?)
                on conflict do nothing
                """, evento.id(), evento.tipo().name(), evento.semente(), utc(evento.inicio()), utc(evento.fim()), evento.sementeVip());
    }

    /**
     * Os eventos em aviso, em andamento ou ainda recebendo resultados. Um evento encurtado (DA-48) continua aqui até o
     * prazo do jogo antigo, que usa a duração padrão ({@link EventoAoVivo#fimDosResultados()}).
     */
    public List<EventoAoVivo> ativos(Instant agora) {
        return jdbc.query("""
                select e.evento, e.tipo, e.semente, e.inicio, e.fim, e.semente_vip, j.nome as vencedor
                from eventos_ao_vivo e left join jogadores j on j.id = e.vip_jogador_id
                where e.inicio <= ? and (e.fim >= ? or e.inicio >= ?)
                order by e.inicio, e.evento
                """, (r, i) -> new EventoAoVivo(r.getString("evento"), TipoDeEvento.valueOf(r.getString("tipo")),
                r.getLong("semente"), r.getObject("inicio", OffsetDateTime.class).toInstant(),
                r.getObject("fim", OffsetDateTime.class).toInstant(), r.getString("vencedor"), r.getString("semente_vip")),
                utc(agora.plusSeconds(AVISO_SEGUNDOS)), utc(agora.minusSeconds(FOLGA_RESULTADO_SEGUNDOS)),
                utc(agora.minusSeconds(FOLGA_RESULTADO_SEGUNDOS + DURACAO_PADRAO_MAXIMA)))
                .stream().filter(evento -> !agora.isAfter(evento.fimDosResultados())).toList();
    }

    /** A maior duração padrão de um evento (o VIP, 45 s). */
    private static final long DURACAO_PADRAO_MAXIMA = (long) java.util.Arrays.stream(TipoDeEvento.values())
            .mapToDouble(TipoDeEvento::duracaoSegundos).max().orElseThrow();

    /** O canal já conferiu que a sessão recebeu o início. A chave do extrato elimina reenvios e sessões duplicadas. */
    public void sobreviver(EventoAoVivo evento, long jogador, Instant agora) {
        if (evento.podeReceberResultado(agora)) {
            pontuar(evento, jogador, 3);
        }
    }

    /** O UPDATE arbitra o primeiro VIP mesmo durante um deploy com duas instâncias. */
    @Transactional
    public Optional<String> pegarVip(EventoAoVivo evento, long jogador, Instant agora) {
        if (!evento.podePegarVip(agora)) {
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
