package br.edu.unifor.missaomarte.servidor.evento;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
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
}
