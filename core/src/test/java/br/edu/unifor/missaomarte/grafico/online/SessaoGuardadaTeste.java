package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A sessão sobrevive ao Android matar o jogo em segundo plano (visto no celular: exit-info LOW_MEMORY): fica nas
 * preferências do app e volta na próxima abertura, enquanto o token valer e o servidor for o mesmo.
 */
class SessaoGuardadaTeste {

    private static final String SERVIDOR = "https://missao-marte.thebylito.dev";
    private static final Instant AGORA = Instant.parse("2026-11-25T22:00:00Z");

    private final Map<String, String> guardado = new HashMap<>();
    private final ConfiguracaoDoServidor.Preferencias preferencias = new ConfiguracaoDoServidor.Preferencias() {
        @Override public String ler(String chave) { return guardado.get(chave); }
        @Override public void gravar(String chave, String valor) { guardado.put(chave, valor); }
    };

    private SessaoGuardada aberta(Instant agora) {
        return new SessaoGuardada(preferencias, Clock.fixed(agora, ZoneOffset.UTC));
    }

    private final Sessao ana = new Sessao("Ana", "tok-da-ana", AGORA.plusSeconds(30L * 24 * 3600));

    @Test
    void semNadaGuardadoNaoHaSessao() {
        assertEquals(Optional.empty(), aberta(AGORA).recuperar(SERVIDOR));
    }

    @Test
    void sessaoGuardadaVoltaNumaNovaAberturaDoJogo() {
        aberta(AGORA).guardar(ana, SERVIDOR);
        // o processo morreu: outra instância, as mesmas preferências
        assertEquals(Optional.of(ana), aberta(AGORA.plusSeconds(3600)).recuperar(SERVIDOR));
    }

    @Test
    void sessaoDeOutroServidorNaoServeEEhApagada() {
        aberta(AGORA).guardar(ana, SERVIDOR);
        assertEquals(Optional.empty(), aberta(AGORA).recuperar("http://192.168.0.10:8080"));
        assertEquals(Optional.empty(), aberta(AGORA).recuperar(SERVIDOR), "o token do outro servidor foi apagado");
    }

    @Test
    void sessaoVencidaNaoServeEEhApagada() {
        aberta(AGORA).guardar(ana, SERVIDOR);
        assertEquals(Optional.empty(), aberta(ana.expiraEm()).recuperar(SERVIDOR));
        assertTrue(guardado.values().stream().allMatch(String::isEmpty), "nada do token ficou nas preferências");
    }

    @Test
    void esquecerApagaTudo() {
        SessaoGuardada s = aberta(AGORA);
        s.guardar(ana, SERVIDOR);
        s.esquecer();
        assertEquals(Optional.empty(), aberta(AGORA).recuperar(SERVIDOR));
        assertFalse(guardado.containsValue("tok-da-ana"));
    }

    @Test
    void semValidadeConhecidaValeAteOServidorRecusar() {
        aberta(AGORA).guardar(new Sessao("Bia", "tok-da-bia"), SERVIDOR);
        Sessao bia = aberta(AGORA.plusSeconds(90L * 24 * 3600)).recuperar(SERVIDOR).orElseThrow();
        assertEquals("tok-da-bia", bia.token());
        assertNull(bia.expiraEm());
    }

    @Test
    void dadosIncompletosOuEstragadosNaoViramSessao() {
        aberta(AGORA).guardar(ana, SERVIDOR);
        guardado.put(SessaoGuardada.TOKEN, "");
        assertEquals(Optional.empty(), aberta(AGORA).recuperar(SERVIDOR));

        aberta(AGORA).guardar(ana, SERVIDOR);
        guardado.put(SessaoGuardada.EXPIRA_EM, "amanhã");
        assertEquals(Optional.empty(), aberta(AGORA).recuperar(SERVIDOR));
    }

    @Test
    void emMemoriaNaoGravaNadaForaDoJogo() {
        SessaoGuardada s = SessaoGuardada.emMemoria();
        s.guardar(ana, SERVIDOR);
        assertEquals(Optional.of(ana), s.recuperar(SERVIDOR));
        assertTrue(guardado.isEmpty());
    }
}
