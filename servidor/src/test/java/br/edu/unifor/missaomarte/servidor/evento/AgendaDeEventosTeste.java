package br.edu.unifor.missaomarte.servidor.evento;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.seguranca.Sementes;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgendaDeEventosTeste {

    @Test
    void sorteiaDeDoisATresEventosEstaveisDentroDaHordaEComVinteMinutosEntreEles() {
        Instant inicio = Instant.parse("2026-11-26T21:00:00Z");
        var tipos = EnumSet.noneOf(TipoDeEvento.class);
        for (long semente = 0; semente < 100; semente++) {
            var agenda = AgendaDeEventos.daHorda(123, inicio, semente);
            assertThat(agenda).isEqualTo(AgendaDeEventos.daHorda(123, inicio, semente));
            assertThat(agenda.size()).isBetween(2, 3);
            assertThat(agenda.get(0).inicio()).isAfterOrEqualTo(inicio.plusSeconds(300));
            for (int i = 0; i < agenda.size(); i++) {
                var evento = agenda.get(i);
                assertThat(evento.id()).isEqualTo("e-123-" + (i + 1));
                assertThat(evento.fim()).isBeforeOrEqualTo(inicio.plusSeconds(7200));
                assertThat(evento.aviso()).isEqualTo(evento.inicio().minusSeconds(15));
                tipos.add(evento.tipo());
                if (i > 0) {
                    assertThat(Duration.between(agenda.get(i - 1).inicio(), evento.inicio()))
                            .isGreaterThanOrEqualTo(Duration.ofMinutes(20));
                }
            }
        }
        assertThat(tipos).containsExactlyInAnyOrder(TipoDeEvento.values());
        assertThat(AgendaDeEventos.daHorda(123, inicio, 0)).isNotEqualTo(AgendaDeEventos.daHorda(123, inicio, 1));
    }

    /** Fase 8 (DA-48): os eventos automáticos duram o que os ajustes gravados na horda dizem. */
    @Test
    void cadaEventoDuraOQueOsAjustesDaHordaDizemEAcabaDentroDela() {
        var longos = new AjustesDosEventos(150, 250, 90, 90, 90, 90);
        var tipos = EnumSet.noneOf(TipoDeEvento.class);
        for (int i = 0; i < 40; i++) {
            Horda horda = horda(i, longos);
            for (EventoAoVivo evento : AgendaDeEventos.daHorda(horda)) {
                assertThat(Duration.between(evento.inicio(), evento.fim())).isEqualTo(Duration.ofSeconds(90));
                assertThat(evento.fim()).isBeforeOrEqualTo(horda.fim());
                tipos.add(evento.tipo());
            }
        }
        assertThat(tipos).containsExactlyInAnyOrder(TipoDeEvento.values());
    }

    /** As hordas gravadas antes dos ajustes (padrão) mantêm os mesmos horários e durações. */
    @Test
    void comOsAjustesPadraoAAgendaEAMesmaDeAntes() {
        for (int i = 0; i < 40; i++) {
            Horda horda = horda(i, AjustesDosEventos.PADRAO);
            for (EventoAoVivo evento : AgendaDeEventos.daHorda(horda)) {
                assertThat(evento.duracao()).isEqualTo((long) evento.tipo().duracaoSegundos());
            }
            Horda antiga = new Horda(horda.numero(), horda.inicio(), horda.fim(), horda.semente(), null,
                    horda.descricao());
            assertThat(AgendaDeEventos.daHorda(antiga))
                    .isEqualTo(AgendaDeEventos.daHorda(horda.numero(), horda.inicio(), horda.semente()));
        }
    }

    private static Horda horda(int i, AjustesDosEventos eventos) {
        Instant inicio = Instant.parse("2026-11-26T21:00:00Z").plus(Duration.ofHours(2L * i));
        var ajustes = new AjustesDeVoo(AjustesDeVoo.PADRAO.facil(), AjustesDeVoo.PADRAO.medio(),
                AjustesDeVoo.PADRAO.dificil(), eventos);
        var descricao = new DescricaoDaHorda(200 + i, Dificuldade.MEDIO, null, List.of(), 1.0, List.of(), List.of(),
                List.of(), ajustes);
        String agenda = Sementes.emHex(Sementes.derivar(new byte[32], "agenda:" + i));
        return new Horda(200 + i, inicio, inicio.plus(Duration.ofHours(2)), 1000L + i, agenda, descricao);
    }
}
