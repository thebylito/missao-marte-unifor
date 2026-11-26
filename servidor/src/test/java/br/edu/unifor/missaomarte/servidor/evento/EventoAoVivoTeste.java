package br.edu.unifor.missaomarte.servidor.evento;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Fase 8 (DA-48): o resultado de um perigo vale de quando ele acaba até 15 s depois. O jogo publicado antes dos
 * ajustes usa a duração padrão; num evento mais longo, ele manda o resultado mais cedo, e ainda vale.
 */
class EventoAoVivoTeste {

    private static final Instant INICIO = Instant.parse("2026-11-26T21:10:00Z");

    @Test
    void comADuracaoPadraoOResultadoValeDoFimAteQuinzeSegundosDepois() {
        EventoAoVivo evento = EventoAoVivo.novo(1, 1, TipoDeEvento.CHUVA_DE_METEOROS, 7, INICIO);

        assertThat(evento.duracao()).isEqualTo(20);
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(19))).isFalse();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(20))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(34))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(35))).isFalse();
    }

    @Test
    void numEventoMaisLongoOResultadoDoJogoAntigoAindaVale() {
        EventoAoVivo evento = EventoAoVivo.novo(1, 1, TipoDeEvento.CHUVA_DE_METEOROS, 7, INICIO, null, 40);

        assertThat(evento.fim()).isEqualTo(INICIO.plusSeconds(40));
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(19))).isFalse();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(20))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(54))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(55))).isFalse();
    }

    /**
     * Num evento mais curto, o resultado vale do fim dele até 15 s depois da duração padrão: o jogo antigo encerra o
     * perigo só na duração padrão (revisão do PR #33).
     */
    @Test
    void numEventoMaisCurtoOResultadoValeDoFimDeleAteOPrazoDoJogoAntigo() {
        EventoAoVivo evento = EventoAoVivo.novo(1, 1, TipoDeEvento.TEMPESTADE, 7, INICIO, null, 12);

        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(11))).isFalse();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(12))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(39))).isTrue();
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(40))).isFalse();
        assertThat(evento.fimDosResultados()).isEqualTo(INICIO.plusSeconds(40));
    }

    @Test
    void comADuracaoPadraoOPrazoDosResultadosEODeAntes() {
        EventoAoVivo evento = EventoAoVivo.novo(1, 1, TipoDeEvento.PANE_DE_GRAVIDADE, 7, INICIO);

        assertThat(evento.fimDosResultados()).isEqualTo(evento.fim().plusSeconds(15));
    }

    @Test
    void oVipNaoTemResultado() {
        EventoAoVivo evento = EventoAoVivo.novo(1, 1, TipoDeEvento.PASSAGEIRO_VIP, 7, INICIO, "00", 60);

        assertThat(evento.fim()).isEqualTo(INICIO.plusSeconds(60));
        assertThat(evento.podeReceberResultado(INICIO.plusSeconds(60))).isFalse();
        assertThat(evento.podePegarVip(INICIO.plusSeconds(59))).isTrue();
        assertThat(evento.podePegarVip(INICIO.plusSeconds(60))).isFalse();
    }
}
