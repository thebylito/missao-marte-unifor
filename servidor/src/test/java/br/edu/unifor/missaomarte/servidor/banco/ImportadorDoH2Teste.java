package br.edu.unifor.missaomarte.servidor.banco;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.banco.ImportadorDoH2.Situacao;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Fase 8 (DA-41): a cópia do banco H2 da Fase 7 para o banco novo, sem Spring e sem Docker. O destino é um H2 em
 * memória em modo PostgreSQL, com o esquema da V1 do Flyway; o PostgreSQL de verdade, com o servidor inteiro, fica
 * em {@code ImportacaoDoH2NoPostgresTeste}.
 */
class ImportadorDoH2Teste {

    private static final String JOGADORES = "select id, criado_em, nome, senha_hash from jogadores order by id";
    private static final String PARTIDAS = "select id, jogador_id, resultado, dificuldade, modo, pontos, passageiros, "
            + "movimentos, tempo_segundos, registrada_em from partidas order by id";

    @TempDir
    Path pasta;

    private DriverManagerDataSource destino;
    private JdbcTemplate jdbc;

    @BeforeEach
    void bancoNovo() {
        destino = new DriverManagerDataSource("jdbc:h2:mem:importacao-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"
                + ApiTeste.MODO_POSTGRESQL, "sa", "");
        Flyway.configure().dataSource(destino).load().migrate();
        jdbc = new JdbcTemplate(destino);
    }

    @Test
    void copiaJogadoresEPartidasComTodasAsColunasEOsMesmosIds() throws Exception {
        Path origem = BancoDaFase7.criar(pasta);

        new ImportadorDoH2(destino).importar(origem);

        assertEquals(linhasDaOrigem(origem, JOGADORES), linhas(destino.getConnection(), JOGADORES));
        assertEquals(linhasDaOrigem(origem, PARTIDAS), linhas(destino.getConnection(), PARTIDAS));
    }

    @Test
    void devolveQuantasLinhasCopiou() throws Exception {
        ImportadorDoH2.Resultado resultado = new ImportadorDoH2(destino).importar(BancoDaFase7.criar(pasta));

        assertEquals(new ImportadorDoH2.Resultado(Situacao.IMPORTADO, BancoDaFase7.JOGADORES, BancoDaFase7.PARTIDAS),
                resultado);
    }

    @Test
    void osProximosIdsContinuamDoMaior() throws Exception {
        new ImportadorDoH2(destino).importar(BancoDaFase7.criar(pasta));

        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (current_timestamp, 'Novata', 'x')");
        Long novata = jdbc.queryForObject("select id from jogadores where nome = 'Novata'", Long.class);
        jdbc.update("insert into partidas(dificuldade, movimentos, passageiros, pontos, registrada_em, resultado, "
                + "tempo_segundos, jogador_id) values ('FACIL', 1, 0, 0, current_timestamp, 'VITORIA', 1, ?)", novata);

        assertEquals(BancoDaFase7.MAIOR_ID_DE_JOGADOR + 1, novata);
        assertEquals(BancoDaFase7.MAIOR_ID_DE_PARTIDA + 1,
                jdbc.queryForObject("select id from partidas where jogador_id = ?", Long.class, novata));
    }

    @Test
    void semAColunaModoAsPartidasSaoClassicas() throws Exception {
        new ImportadorDoH2(destino).importar(BancoDaFase7.criarSemModo(pasta));

        assertEquals(List.of("CLASSICO"), jdbc.queryForList("select distinct modo from partidas", String.class));
        assertEquals(BancoDaFase7.PARTIDAS, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test
    void comDadosNoDestinoNaoFazNada() throws Exception {
        jdbc.update("insert into jogadores(criado_em, nome, senha_hash) values (current_timestamp, 'Nova', 'x')");

        ImportadorDoH2.Resultado resultado = new ImportadorDoH2(destino).importar(BancoDaFase7.criar(pasta));

        assertEquals(Situacao.DESTINO_COM_DADOS, resultado.situacao());
        assertEquals(List.of("Nova"), jdbc.queryForList("select nome from jogadores", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test
    void rodarDeNovoNaoDuplicaNada() throws Exception {
        Path origem = BancoDaFase7.criar(pasta);
        new ImportadorDoH2(destino).importar(origem);

        ImportadorDoH2.Resultado segunda = new ImportadorDoH2(destino).importar(origem);

        assertEquals(new ImportadorDoH2.Resultado(Situacao.DESTINO_COM_DADOS, 0, 0), segunda);
        assertEquals(BancoDaFase7.JOGADORES, jdbc.queryForObject("select count(*) from jogadores", Integer.class));
        assertEquals(BancoDaFase7.PARTIDAS, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test
    void semOArquivoNaoFazNadaNemCriaUmBanco() throws Exception {
        Path inexistente = pasta.resolve("nao-existe").toAbsolutePath();

        ImportadorDoH2.Resultado resultado = new ImportadorDoH2(destino).importar(inexistente);

        assertEquals(Situacao.ORIGEM_INEXISTENTE, resultado.situacao());
        try (Stream<Path> arquivos = Files.list(pasta)) {
            assertEquals(0, arquivos.count());
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from jogadores", Integer.class));
    }

    @Test
    void oArquivoDeOrigemNaoMuda() throws Exception {
        Path origem = BancoDaFase7.criar(pasta);
        Path arquivo = Path.of(origem + ".mv.db");
        byte[] antes = resumo(arquivo);
        List<Path> arquivosAntes = arquivos();

        new ImportadorDoH2(destino).importar(origem);

        assertArrayEquals(antes, resumo(arquivo));
        assertEquals(arquivosAntes, arquivos());
    }

    @Test
    void umCaminhoRelativoTambemServe() throws Exception {
        Path origem = BancoDaFase7.criar(pasta);
        Path relativo = Path.of("").toAbsolutePath().relativize(origem);
        assertFalse(relativo.isAbsolute());

        assertEquals(Situacao.IMPORTADO, new ImportadorDoH2(destino).importar(relativo).situacao());
    }

    @Test
    void asColunasEnumDaOrigemViramTexto() throws Exception {
        new ImportadorDoH2(destino).importar(BancoDaFase7.criar(pasta));

        assertTrue(jdbc.queryForList("select resultado from partidas", String.class).contains("ABORTADA"));
        assertEquals(3, jdbc.queryForObject("select count(*) from partidas where modo = 'PILOTAGEM'", Integer.class));
    }

    private List<String> linhasDaOrigem(Path origem, String consulta) throws SQLException {
        return linhas(DriverManager.getConnection("jdbc:h2:file:" + origem + ";ACCESS_MODE_DATA=r", "sa", ""), consulta);
    }

    /** As linhas como texto; as datas viram o instante (o fuso da conexão não importa). Fecha a conexão. */
    private static List<String> linhas(Connection conexao, String consulta) throws SQLException {
        List<String> linhas = new ArrayList<>();
        try (Connection c = conexao; Statement s = c.createStatement(); ResultSet r = s.executeQuery(consulta)) {
            ResultSetMetaData meta = r.getMetaData();
            while (r.next()) {
                StringBuilder linha = new StringBuilder();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String nome = meta.getColumnLabel(i).toLowerCase();
                    Object valor = nome.endsWith("_em") ? r.getObject(i, OffsetDateTime.class).toInstant()
                            : r.getString(i);
                    linha.append(nome).append('=').append(valor).append(' ');
                }
                linhas.add(linha.toString());
            }
        }
        return linhas;
    }

    private static byte[] resumo(Path arquivo) throws IOException, NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(arquivo));
    }

    private List<Path> arquivos() throws IOException {
        try (Stream<Path> lista = Files.list(pasta)) {
            return lista.sorted().toList();
        }
    }
}
