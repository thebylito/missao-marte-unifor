package br.edu.unifor.missaomarte.servidor.banco;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Um arquivo H2 como o de produção na Fase 7: o esquema exato que o Hibernate criou ({@code esquema-fase-7.sql}, com
 * as colunas ENUM nativas e a "Veterana") e mais dados, com buracos nos ids, como num banco de verdade.
 *
 * <p>Ranking esperado. Clássico: Comandante 80, Cadete 50 (fez antes), Veterana 50. Pilotagem: Comandante 60 (fez
 * antes), Veterana 60. Maior id de jogador: 7; maior id de partida: 20.
 */
final class BancoDaFase7 {

    static final String COMANDANTE = "Comandante";
    static final String SENHA_DA_COMANDANTE = "senha-da-comandante";
    static final long MAIOR_ID_DE_JOGADOR = 7;
    static final long MAIOR_ID_DE_PARTIDA = 20;
    static final int JOGADORES = 3;
    static final int PARTIDAS = 7;

    private BancoDaFase7() {
    }

    /** Cria o banco em {@code pasta/missaomarte.mv.db} e devolve o caminho sem a extensão, como o H2 espera. */
    static Path criar(Path pasta) throws SQLException {
        Path banco = pasta.resolve("missaomarte").toAbsolutePath();
        String hash = new BCryptPasswordEncoder().encode(SENHA_DA_COMANDANTE);
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + banco, "sa", "");
             Statement s = c.createStatement()) {
            s.execute("RUNSCRIPT FROM 'classpath:esquema-fase-7.sql'");   // Veterana (id 1) e as partidas 1 e 2
            s.execute("INSERT INTO JOGADORES(ID, CRIADO_EM, NOME, SENHA_HASH) VALUES "
                    + "(3, TIMESTAMP WITH TIME ZONE '2026-11-10 08:00:00+00', 'Cadete', 'x'), "
                    + "(7, TIMESTAMP WITH TIME ZONE '2026-11-20 10:00:00.123456+00', '" + COMANDANTE + "', '" + hash
                    + "')");
            s.execute("INSERT INTO PARTIDAS(ID, DIFICULDADE, MODO, MOVIMENTOS, PASSAGEIROS, PONTOS, REGISTRADA_EM, "
                    + "RESULTADO, TEMPO_SEGUNDOS, JOGADOR_ID) VALUES "
                    + "(5, 'DIFICIL', 'CLASSICO', 30, 4, 50, TIMESTAMP WITH TIME ZONE '2026-11-10 09:00:00+00', "
                    + "'VITORIA', 75, 3), "
                    + "(11, 'FACIL', 'CLASSICO', 18, 4, 80, TIMESTAMP WITH TIME ZONE '2026-11-21 12:00:00.5+00', "
                    + "'VITORIA', 33, 7), "
                    + "(12, 'MEDIO', 'PILOTAGEM', 0, 3, 60, TIMESTAMP WITH TIME ZONE '2026-11-21 12:30:00+00', "
                    + "'VITORIA', 95, 7), "
                    + "(15, 'DIFICIL', 'CLASSICO', 40, 1, 999, TIMESTAMP WITH TIME ZONE '2026-11-22 18:00:00+00', "
                    + "'DERROTA', 200, 7), "
                    + "(20, 'MEDIO', 'PILOTAGEM', 0, 0, 0, TIMESTAMP WITH TIME ZONE '2026-11-23 07:45:00+00', "
                    + "'ABORTADA', 12, 7)");
        }
        return banco;
    }

    /** O banco das Fases 5 e 6, de antes da coluna MODO (todas as partidas são do modo clássico). */
    static Path criarSemModo(Path pasta) throws SQLException {
        Path banco = criar(pasta);
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + banco, "sa", "");
             Statement s = c.createStatement()) {
            s.execute("ALTER TABLE PARTIDAS DROP COLUMN MODO");
        }
        return banco;
    }
}
