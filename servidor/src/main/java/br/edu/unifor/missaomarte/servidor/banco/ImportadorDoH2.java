package br.edu.unifor.missaomarte.servidor.banco;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fase 8 (DA-41): copia os jogadores e as partidas do banco H2 da Fase 7 (o arquivo do volume {@code /dados} em
 * produção) para o banco novo, o PostgreSQL. Roda uma vez, na virada.
 *
 * <ul>
 *   <li>Só copia se o banco novo estiver vazio: numa segunda subida, não faz nada (e diz no log).</li>
 *   <li>Os ids são os mesmos, e as sequências de identidade continuam do maior id copiado.</li>
 *   <li>O H2 é aberto só para leitura ({@code ACCESS_MODE_DATA=r}) e nunca é criado ({@code IFEXISTS=TRUE}).</li>
 *   <li>Tudo numa transação: se algo falha, o banco novo fica vazio e a subida do servidor falha junto, em vez de
 *       ele atender com o ranking pela metade.</li>
 *   <li>Sem a coluna {@code MODO} (banco das Fases 5 e 6), as partidas são do modo clássico. As colunas
 *       {@code ENUM} do H2 da 7.x são lidas como texto.</li>
 * </ul>
 *
 * <p>Usa só JDBC, com o SQL comum ao PostgreSQL e ao H2 em modo PostgreSQL. Quem chama é
 * {@link ConfiguracaoDoBanco}, logo depois das migrações do Flyway e antes de o servidor atender pedidos.
 */
public class ImportadorDoH2 {

    /** O que a importação fez. */
    public enum Situacao { IMPORTADO, DESTINO_COM_DADOS, ORIGEM_INEXISTENTE }

    /** A situação e quantas linhas foram copiadas. */
    public record Resultado(Situacao situacao, int jogadores, int partidas) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(ImportadorDoH2.class);
    private static final int LOTE = 500;

    private final DataSource destino;

    public ImportadorDoH2(DataSource destino) {
        this.destino = destino;
    }

    /**
     * @param origem o banco H2, sem a extensão {@code .mv.db} (o mesmo formato de {@code MISSAOMARTE_DADOS}, por
     *               exemplo {@code /dados/missaomarte})
     */
    public Resultado importar(Path origem) {
        Path banco = origem.toAbsolutePath().normalize();
        Path arquivo = Path.of(banco + ".mv.db");
        if (!Files.isRegularFile(arquivo)) {
            LOG.warn("Importação do H2: {} não existe; nada importado", arquivo);
            return new Resultado(Situacao.ORIGEM_INEXISTENTE, 0, 0);
        }
        try (Connection novo = destino.getConnection()) {
            long linhas = contar(novo, "jogadores") + contar(novo, "partidas");
            if (linhas > 0) {
                LOG.info("Importação do H2: o banco novo já tem dados ({} linhas em jogadores e partidas); "
                        + "nada importado de {}", linhas, arquivo);
                return new Resultado(Situacao.DESTINO_COM_DADOS, 0, 0);
            }
            try (Connection antigo = abrirSoParaLeitura(banco)) {
                return copiar(antigo, novo, arquivo);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Importação do H2 " + arquivo + " falhou; o banco novo ficou como "
                    + "estava", e);
        }
    }

    private static Connection abrirSoParaLeitura(Path banco) {
        try {
            return DriverManager.getConnection("jdbc:h2:file:" + banco + ";ACCESS_MODE_DATA=r;IFEXISTS=TRUE",
                    "sa", "");
        } catch (SQLException e) {
            throw new IllegalStateException("Importação do H2: não deu para abrir " + banco + ".mv.db só para "
                    + "leitura. Se o arquivo está travado, o servidor antigo ainda está rodando: o deploy da virada "
                    + "precisa ser stop-first", e);
        }
    }

    private static Resultado copiar(Connection antigo, Connection novo, Path arquivo) throws SQLException {
        boolean autoCommit = novo.getAutoCommit();
        novo.setAutoCommit(false);
        try {
            int jogadores = copiarJogadores(antigo, novo);
            int partidas = copiarPartidas(antigo, novo);
            long proximoJogador = continuarDoMaior(novo, "jogadores");
            long proximaPartida = continuarDoMaior(novo, "partidas");
            novo.commit();
            LOG.info("Importação do H2 {}: {} jogadores e {} partidas copiados, com os mesmos ids; próximos ids: "
                    + "jogador {}, partida {}", arquivo, jogadores, partidas, proximoJogador, proximaPartida);
            return new Resultado(Situacao.IMPORTADO, jogadores, partidas);
        } catch (SQLException | RuntimeException e) {
            novo.rollback();
            throw e;
        } finally {
            novo.setAutoCommit(autoCommit);
        }
    }

    private static int copiarJogadores(Connection antigo, Connection novo) throws SQLException {
        try (Statement leitura = antigo.createStatement();
             ResultSet r = leitura.executeQuery("select id, criado_em, nome, senha_hash from jogadores order by id");
             PreparedStatement escrita = novo.prepareStatement(
                     "insert into jogadores (id, criado_em, nome, senha_hash) values (?, ?, ?, ?)")) {
            int linhas = 0;
            while (r.next()) {
                escrita.setLong(1, r.getLong("id"));
                escrita.setObject(2, r.getObject("criado_em", OffsetDateTime.class));
                escrita.setString(3, r.getString("nome"));
                escrita.setString(4, r.getString("senha_hash"));
                linhas = emLote(escrita, linhas);
            }
            escrita.executeBatch();
            return linhas;
        }
    }

    private static int copiarPartidas(Connection antigo, Connection novo) throws SQLException {
        String modo = temColuna(antigo, "partidas", "modo") ? "coalesce(modo, 'CLASSICO')" : "'CLASSICO'";
        try (Statement leitura = antigo.createStatement();
             ResultSet r = leitura.executeQuery("select id, jogador_id, resultado, dificuldade, " + modo
                     + " as modo, pontos, passageiros, movimentos, tempo_segundos, registrada_em "
                     + "from partidas order by id");
             PreparedStatement escrita = novo.prepareStatement("insert into partidas (id, jogador_id, resultado, "
                     + "dificuldade, modo, pontos, passageiros, movimentos, tempo_segundos, registrada_em) "
                     + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int linhas = 0;
            while (r.next()) {
                escrita.setLong(1, r.getLong("id"));
                escrita.setLong(2, r.getLong("jogador_id"));
                escrita.setString(3, r.getString("resultado"));
                escrita.setString(4, r.getString("dificuldade"));
                escrita.setString(5, r.getString("modo"));
                escrita.setInt(6, r.getInt("pontos"));
                escrita.setInt(7, r.getInt("passageiros"));
                escrita.setInt(8, r.getInt("movimentos"));
                escrita.setLong(9, r.getLong("tempo_segundos"));
                escrita.setObject(10, r.getObject("registrada_em", OffsetDateTime.class));
                linhas = emLote(escrita, linhas);
            }
            escrita.executeBatch();
            return linhas;
        }
    }

    private static int emLote(PreparedStatement escrita, int linhas) throws SQLException {
        escrita.addBatch();
        if ((linhas + 1) % LOTE == 0) {
            escrita.executeBatch();
        }
        return linhas + 1;
    }

    /** A próxima linha sem id recebe o maior id copiado + 1. Funciona no PostgreSQL e no H2 (colunas de identidade). */
    private static long continuarDoMaior(Connection novo, String tabela) throws SQLException {
        long proximo;
        try (Statement s = novo.createStatement();
             ResultSet r = s.executeQuery("select coalesce(max(id), 0) + 1 from " + tabela)) {
            r.next();
            proximo = r.getLong(1);
        }
        try (Statement s = novo.createStatement()) {
            s.execute("alter table " + tabela + " alter column id restart with " + proximo);
        }
        return proximo;
    }

    private static long contar(Connection c, String tabela) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("select count(*) from " + tabela)) {
            r.next();
            return r.getLong(1);
        }
    }

    /** Pelos metadados da consulta, sem depender de maiúsculas: o H2 da 7.x guarda os nomes em maiúsculas. */
    private static boolean temColuna(Connection c, String tabela, String coluna) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("select * from " + tabela + " where 1 = 0")) {
            ResultSetMetaData meta = r.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (meta.getColumnName(i).equalsIgnoreCase(coluna)) {
                    return true;
                }
            }
            return false;
        }
    }
}
