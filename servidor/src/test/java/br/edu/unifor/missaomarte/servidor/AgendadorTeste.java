package br.edu.unifor.missaomarte.servidor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeFechamentos;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AgendadorTeste {

    @Test
    void falhaNosEventosNaoImpedeOFechamentoNoMesmoCiclo() {
        Instant agora = Instant.parse("2026-11-26T21:00:00Z");
        var canal = mock(CanalDeSombras.class);
        var fechamentos = mock(ServicoDeFechamentos.class);
        doThrow(new IllegalStateException("banco indisponível")).when(canal).verificarEventos(agora);
        var agendador = new Agendador(Clock.fixed(agora, ZoneOffset.UTC), fechamentos, canal);

        assertDoesNotThrow(agendador::verificar);
        verify(fechamentos).verificar(agora);
    }

    @Test
    void falhaNaRecuperacaoNaoEscapaEOProximoCicloTentaDeNovo() {
        var agendador = mock(Agendador.class);
        doThrow(new IllegalStateException("falha na subida")).doNothing().when(agendador).verificar();
        var configuracao = new ConfiguracaoDoAgendamento(agendador);

        assertDoesNotThrow(configuracao::recuperar);
        assertDoesNotThrow(configuracao::agendar);
        verify(agendador, times(2)).verificar();
    }
}
