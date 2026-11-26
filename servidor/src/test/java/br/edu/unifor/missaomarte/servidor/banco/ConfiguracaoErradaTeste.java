package br.edu.unifor.missaomarte.servidor.banco;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.servidor.ServidorApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Revisão da etapa 1b: uma configuração errada não pode subir "com sucesso" no banco errado. Com o banco novo vazio,
 * cadastros novos ganhariam os ids 1, 2, 3, e o token antigo de um jogador (que só guarda o id) entraria na conta de
 * outra pessoa.
 */
class ConfiguracaoErradaTeste {

    @Test
    void comAUrlDoBancoVaziaOServidorNaoSobeNumBancoEmMemoria() {
        // como argumento de linha de comando, que ganha do application.properties (o properties() do builder perde)
        Exception erro = assertThrows(Exception.class, () -> new SpringApplicationBuilder(ServidorApplication.class)
                .run("--MISSAOMARTE_DB_URL=", "--server.port=0",
                        "--missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais")
                .close());
        StringBuilder causas = new StringBuilder();
        for (Throwable t = erro; t != null; t = t.getCause()) {
            causas.append(t.getMessage()).append(" | ");
        }
        assertTrue(causas.toString().contains("Failed to determine a suitable driver class"), causas.toString());
    }

    @Test
    void naImagemSemOBancoConfiguradoOH2ArquivoNaoEhUsado() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> GuardaDoBanco.verificar(Map.of("MISSAOMARTE_PERMITIR_H2", "false")));
        assertTrue(erro.getMessage().contains("MISSAOMARTE_DB_URL"), erro.getMessage());
        assertThrows(IllegalStateException.class, () -> GuardaDoBanco.verificar(
                Map.of("MISSAOMARTE_PERMITIR_H2", "false", "MISSAOMARTE_DB_URL", "  ")));
    }

    @Test
    void comOBancoConfiguradoOuForaDaImagemPodeSubir() {
        assertDoesNotThrow(() -> GuardaDoBanco.verificar(Map.of(
                "MISSAOMARTE_PERMITIR_H2", "false",
                "MISSAOMARTE_DB_URL", "jdbc:postgresql://host:5432/missaomarte")));
        assertDoesNotThrow(() -> GuardaDoBanco.verificar(Map.of()));
    }
}
