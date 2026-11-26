package br.edu.unifor.missaomarte.servidor.moedas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fase 8 (CT-14): o extrato de moedas. O saldo é a soma dos movimentos, e o mesmo crédito (jogador, origem e
 * referência) nunca entra duas vezes: o serviço confere antes e a restrição única do banco garante.
 */
class ExtratoDeMoedasTeste extends ApiTeste {

    @Autowired
    private ServicoDeMoedas moedas;

    @Autowired
    private RepositorioDeMoedas extrato;

    @Autowired
    private RepositorioDeJogadores jogadores;

    @Autowired
    private DataSource banco;

    @Autowired
    private JdbcTemplate jdbc;

    private long idDe(String token) throws Exception {
        String corpo = mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    @Test
    void semMovimentosOSaldoEZero() throws Exception {                              // CT-14
        long ana = idDe(novoJogador("Ana"));
        assertEquals(0, moedas.saldo(ana));
    }

    @Test
    void oSaldoEASomaDosMovimentosDoProprioJogador() throws Exception {             // CT-14
        long ana = idDe(novoJogador("Ana"));
        long bia = idDe(novoJogador("Bia"));
        assertEquals(3, moedas.creditar(ana, Origem.VITORIA_LIVRE, "partida:1", 3));
        assertEquals(1, moedas.creditar(ana, Origem.VITORIA_CLASSICO, "partida:2", 1));
        assertEquals(50, moedas.creditar(bia, Origem.SEMANA, "semana:2026-W48", 50));

        assertEquals(4, moedas.saldo(ana));
        assertEquals(50, moedas.saldo(bia));
    }

    @Test
    void oMesmoCreditoNaoEntraDuasVezes() throws Exception {                        // CT-14
        long ana = idDe(novoJogador("Ana"));
        assertEquals(3, moedas.creditar(ana, Origem.VITORIA_LIVRE, "partida:7", 3));
        assertEquals(0, moedas.creditar(ana, Origem.VITORIA_LIVRE, "partida:7", 3));   // repetido: nada
        assertEquals(3, moedas.saldo(ana));

        // a mesma referência com outra origem é outro crédito
        assertEquals(1, moedas.creditar(ana, Origem.VITORIA_CLASSICO, "partida:7", 1));
        assertEquals(4, moedas.saldo(ana));
    }

    @Test
    void oBancoRecusaOCreditoRepetidoMesmoSemPassarPeloServico() throws Exception {   // CT-14
        long ana = idDe(novoJogador("Ana"));
        moedas.creditar(ana, Origem.HORDA, "horda:12", 20);

        MovimentoDeMoedas repetido = new MovimentoDeMoedas(jogadores.getReferenceById(ana), Origem.HORDA, "horda:12",
                20, relogio.instant());
        assertThrows(DataIntegrityViolationException.class, () -> extrato.saveAndFlush(repetido));
    }

    @Test
    void creditoPrecisaSerPositivo() throws Exception {
        long ana = idDe(novoJogador("Ana"));
        assertThrows(IllegalArgumentException.class, () -> moedas.creditar(ana, Origem.SEMANA, "semana:1", 0));
        assertThrows(IllegalArgumentException.class, () -> moedas.creditar(ana, Origem.SEMANA, "semana:1", -5));
    }

    @Test
    void aOrigemEhTextoNoBanco() throws SQLException {                              // DA-40
        try (Connection c = banco.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'MOEDAS' AND COLUMN_NAME = 'ORIGEM'")) {
            assertTrue(r.next(), "coluna MOEDAS.ORIGEM");
            assertEquals("CHARACTER VARYING", r.getString(1));
            assertEquals(20, r.getInt(2));
        }
    }

    @Test
    void oExtratoNaoTemListaFechadaDeOrigens() throws Exception {                   // DA-40
        Integer restricoes = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'MOEDAS' AND CONSTRAINT_TYPE = 'CHECK'",
                Integer.class);
        assertEquals(0, restricoes);

        // uma origem que o enum ainda não conhece cabe na coluna (quem valida é o Java)
        long ana = idDe(novoJogador("Ana"));
        assertEquals(1, jdbc.update("INSERT INTO MOEDAS(JOGADOR_ID, ORIGEM, REFERENCIA, VALOR, REGISTRADO_EM) "
                + "VALUES (?, 'EVENTO_FUTURO', 'teste:1', 1, CURRENT_TIMESTAMP)", ana));
    }
}
