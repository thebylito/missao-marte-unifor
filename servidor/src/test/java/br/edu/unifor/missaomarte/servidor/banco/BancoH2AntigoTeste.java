package br.edu.unifor.missaomarte.servidor.banco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.ServidorApplication;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Fase 8 (DA-41): sem {@code MISSAOMARTE_DB_URL}, o servidor usa o H2 de {@code MISSAOMARTE_DADOS}. Se ali estiver um
 * banco antigo, criado pelo Hibernate antes do Flyway (o {@code servidor/dados} de quem já rodava o servidor, ou o
 * {@code /dados} da produção num deploy sem as variáveis do PostgreSQL), a subida falha logo na conexão: o H2 em modo
 * PostgreSQL ({@code DATABASE_TO_LOWER}) não acha o schema {@code PUBLIC}, em maiúsculas, do banco antigo, e o Flyway
 * nem chega a rodar. O H2 regrava um pedaço do arquivo ao fechar, mas os dados ficam: o banco continua abrindo como na
 * Fase 7 e ainda pode ser importado. O banco de desenvolvimento antigo é apagado (ou importado, DA-41).
 */
class BancoH2AntigoTeste {

    @TempDir
    Path pasta;

    @Test
    void oServidorNaoSobeSobreUmH2AntigoEOsDadosFicam() throws Exception {
        Path banco = BancoDaFase7.criar(pasta);
        List<String> antes = tabelasEContagens(banco);

        Exception erro = assertThrows(Exception.class, () -> new SpringApplicationBuilder(ServidorApplication.class)
                .run("--server.port=0", "--MISSAOMARTE_DADOS=" + banco,
                        "--MISSAOMARTE_JWT_SEGREDO=segredo-so-para-os-testes-com-32-bytes-ou-mais").close());

        assertTrue(mensagens(erro).contains("Schema \"PUBLIC\" not found"), mensagens(erro));
        assertEquals(antes, tabelasEContagens(banco));
        DriverManagerDataSource destino = new DriverManagerDataSource("jdbc:h2:mem:antigo-" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1" + ApiTeste.MODO_POSTGRESQL, "sa", "");
        Flyway.configure().dataSource(destino).load().migrate();
        assertEquals(new ImportadorDoH2.Resultado(ImportadorDoH2.Situacao.IMPORTADO, BancoDaFase7.JOGADORES,
                BancoDaFase7.PARTIDAS), new ImportadorDoH2(destino).importar(banco));
    }

    /** Abre o arquivo como a versão da Fase 7 abria (sem opções), como numa volta para a imagem antiga. */
    private static List<String> tabelasEContagens(Path banco) throws Exception {
        List<String> tabelas = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + banco, "sa", "");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("select table_name from information_schema.tables "
                     + "where table_schema = 'PUBLIC' order by table_name")) {
            while (r.next()) {
                tabelas.add(r.getString(1));
            }
        }
        List<String> contagens = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + banco, "sa", "");
             Statement s = c.createStatement()) {
            for (String tabela : tabelas) {
                try (ResultSet r = s.executeQuery("select count(*) from \"" + tabela + "\"")) {
                    r.next();
                    contagens.add(tabela + "=" + r.getLong(1));
                }
            }
        }
        return contagens;
    }

    private static String mensagens(Throwable erro) {
        StringBuilder texto = new StringBuilder();
        for (Throwable t = erro; t != null; t = t.getCause()) {
            texto.append(t.getMessage()).append(" | ");
        }
        return texto.toString();
    }
}
