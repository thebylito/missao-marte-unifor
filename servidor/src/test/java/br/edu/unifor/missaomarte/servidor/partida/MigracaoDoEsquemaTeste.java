package br.edu.unifor.missaomarte.servidor.partida;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fase 8 (DA-40): o servidor sobe sobre um banco criado pela versão da Fase 7, com as colunas ENUM nativas do H2,
 * e as converte para texto sem perder nada. Assim um modo novo (a Horda) cabe na coluna, e o banco de produção
 * continua com as partidas de sempre.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MigracaoDoEsquemaTeste {

    private static final List<String> COLUNAS_DE_ENUM = List.of("MODO", "RESULTADO", "DIFICULDADE");

    /** Cria o banco da Fase 7 num arquivo novo, antes de o servidor subir, e aponta o servidor para ele. */
    @DynamicPropertySource
    static void bancoDaFase7(DynamicPropertyRegistry registro) throws Exception {
        Path pasta = Files.createTempDirectory("missaomarte-fase7-");
        String url = "jdbc:h2:file:" + pasta.resolve("missaomarte");
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             Statement s = c.createStatement()) {
            s.execute("RUNSCRIPT FROM 'classpath:esquema-fase-7.sql'");
        }
        registro.add("spring.datasource.url", () -> url);
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registro.add("missaomarte.jwt.segredo", () -> "segredo-so-para-os-testes-com-32-bytes-ou-mais");
    }

    @Autowired
    private DataSource banco;

    @Autowired
    private MockMvc mvc;

    @Test
    void asColunasDeEnumViramTexto() throws SQLException {
        for (String coluna : COLUNAS_DE_ENUM) {
            assertEquals("CHARACTER VARYING", coluna(coluna, "DATA_TYPE"), coluna);
        }
    }

    @Test
    void asPartidasAntigasContinuamIguais() throws SQLException {
        List<String> linhas = new ArrayList<>();
        try (Connection c = banco.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT MODO, RESULTADO, DIFICULDADE, PONTOS FROM PARTIDAS ORDER BY ID")) {
            while (r.next()) {
                linhas.add(r.getString(1) + "," + r.getString(2) + "," + r.getString(3) + "," + r.getInt(4));
            }
        }
        assertEquals(List.of("CLASSICO,VITORIA,MEDIO,50", "PILOTAGEM,VITORIA,DIFICIL,60"), linhas);
    }

    @Test
    void oDefaultEONotNullDoModoContinuam() throws SQLException {
        assertTrue(coluna("MODO", "COLUMN_DEFAULT").contains("'CLASSICO'"), coluna("MODO", "COLUMN_DEFAULT"));
        for (String coluna : COLUNAS_DE_ENUM) {
            assertEquals("NO", coluna(coluna, "IS_NULLABLE"), coluna);
        }
    }

    @Test
    void umModoNovoJaCabeNaColuna() throws SQLException {
        try (Connection c = banco.getConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                assertDoesNotThrow(() -> s.executeUpdate("INSERT INTO PARTIDAS(DIFICULDADE, MODO, MOVIMENTOS, "
                        + "PASSAGEIROS, PONTOS, REGISTRADA_EM, RESULTADO, TEMPO_SEGUNDOS, JOGADOR_ID) VALUES "
                        + "('MEDIO', 'HORDA', 1, 0, 0, CURRENT_TIMESTAMP, 'DERROTA', 1, 1)"));
            } finally {
                c.rollback();   // a linha de teste não fica no banco (o enum Modo ainda não conhece HORDA)
            }
        }
    }

    @Test
    void oRankingDaApiContinuaIgual() throws Exception {
        mvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Veterana"))
                .andExpect(jsonPath("$[0].pontos").value(50));
        mvc.perform(get("/api/ranking").param("modo", "PILOTAGEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Veterana"))
                .andExpect(jsonPath("$[0].pontos").value(60));
    }

    @Test
    void oScriptPodeRodarDeNovo() throws SQLException {
        try (Connection c = banco.getConnection(); Statement s = c.createStatement()) {
            assertDoesNotThrow(() -> s.execute("RUNSCRIPT FROM 'classpath:migracoes/enums-para-texto.sql'"));
        }
        asColunasDeEnumViramTexto();
    }

    private String coluna(String nome, String atributo) throws SQLException {
        try (Connection c = banco.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT " + atributo + " FROM INFORMATION_SCHEMA.COLUMNS "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PARTIDAS' AND COLUMN_NAME = '" + nome + "'")) {
            assertTrue(r.next(), "coluna " + nome);
            return r.getString(1);
        }
    }
}
