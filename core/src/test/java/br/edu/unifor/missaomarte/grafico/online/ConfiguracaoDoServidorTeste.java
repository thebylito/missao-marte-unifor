package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Endereço do servidor configurável no jogo (issue #10/#11: sem "-D" no celular, sem segredo no app). */
class ConfiguracaoDoServidorTeste {

    private final Map<String, String> guardado = new HashMap<>();
    private final ConfiguracaoDoServidor.Preferencias preferencias = new ConfiguracaoDoServidor.Preferencias() {
        @Override public String ler(String chave) { return guardado.get(chave); }
        @Override public void gravar(String chave, String valor) { guardado.put(chave, valor); }
    };

    @Test
    void semNadaGravadoUsaOPadraoDaPlataforma() {
        assertEquals("http://10.0.2.2:8080", new ConfiguracaoDoServidor(preferencias, "http://10.0.2.2:8080", null).endereco());
    }

    @Test
    void enderecoGravadoVenceOPadraoESobreviveAUmaNovaConfiguracao() {
        ConfiguracaoDoServidor c = new ConfiguracaoDoServidor(preferencias, "http://10.0.2.2:8080", null);
        assertEquals(Optional.empty(), c.alterar("  http://192.168.0.10:8080/  "));
        assertEquals("http://192.168.0.10:8080", c.endereco());
        assertEquals("http://192.168.0.10:8080",
                new ConfiguracaoDoServidor(preferencias, "http://10.0.2.2:8080", null).endereco(), "ficou guardado");
    }

    @Test
    void enderecoVazioVoltaAoPadrao() {
        ConfiguracaoDoServidor c = new ConfiguracaoDoServidor(preferencias, "http://localhost:8080", null);
        c.alterar("https://ranking.exemplo.com");
        assertEquals(Optional.empty(), c.alterar("   "));
        assertEquals("http://localhost:8080", c.endereco());
    }

    @Test
    void enderecoDaLinhaDeComandoTemPrioridadeENaoPodeSerTrocado() {
        guardado.put(ConfiguracaoDoServidor.CHAVE, "http://gravado:8080");
        ConfiguracaoDoServidor c = new ConfiguracaoDoServidor(preferencias, "http://localhost:8080", "http://forcado:9090");
        assertEquals("http://forcado:9090", c.endereco());
        assertTrue(c.isForcado());
        assertTrue(c.alterar("http://outro:8080").isPresent());
        assertEquals("http://gravado:8080", guardado.get(ConfiguracaoDoServidor.CHAVE));
    }

    @Test
    void enderecosInvalidosNaoSaoGravadosEExplicamOProblema() {
        ConfiguracaoDoServidor c = new ConfiguracaoDoServidor(preferencias, "http://localhost:8080", null);
        assertEquals(Optional.of("Use http:// ou https:// no começo do endereço."), c.alterar("192.168.0.10:8080"));
        assertEquals(Optional.of("Use http:// ou https:// no começo do endereço."), c.alterar("ftp://servidor"));
        assertTrue(c.alterar("http://").isPresent());
        assertEquals(Optional.of("A porta deve ficar entre 1 e 65535."), c.alterar("http://servidor:99999"));
        assertTrue(c.alterar("http://usuario:senha@servidor:8080").isPresent(), "nada de credencial no endereço");
        assertTrue(c.alterar("http://servidor:8080?x=1").isPresent());
        assertTrue(c.alterar("http://ser vidor").isPresent());
        assertEquals("http://localhost:8080", c.endereco(), "nenhum inválido foi gravado");
    }

    @Test
    void enderecosValidos() {
        for (String e : new String[] {"http://localhost:8080", "http://10.0.2.2:8080", "https://ranking.exemplo.com",
                "http://192.168.0.10", "https://exemplo.com/api-missao"}) {
            assertEquals(Optional.empty(), ConfiguracaoDoServidor.problema(e), e);
        }
    }
}
