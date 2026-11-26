package br.edu.unifor.missaomarte.servidor.partida;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fase 8 (DA-40): num banco criado do zero pelo Hibernate (o volume novo de quem sobe o compose pela primeira vez,
 * o primeiro deploy de outra pessoa), as colunas de enum também aceitam valores novos. Sem o dialeto
 * {@code H2SemCheckDeEnum}, o Hibernate 7.4 grava {@code check (modo in ('CLASSICO','PILOTAGEM'))} na criação da
 * tabela, e o {@code ddl-auto=update} nunca mais muda essa lista: a Horda seria recusada.
 */
class EsquemaNovoTeste extends ApiTeste {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aTabelaDePartidasNaoTemListaFechadaDeValores() {
        Integer restricoes = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PARTIDAS' AND CONSTRAINT_TYPE = 'CHECK'",
                Integer.class);
        assertEquals(0, restricoes);
    }

    @Test
    void umModoNovoCabeNumBancoNovo() {
        jdbc.update("INSERT INTO JOGADORES(CRIADO_EM, NOME, SENHA_HASH) VALUES (CURRENT_TIMESTAMP, 'Novata', 'x')");
        Long jogador = jdbc.queryForObject("SELECT ID FROM JOGADORES WHERE NOME = 'Novata'", Long.class);
        assertDoesNotThrow(() -> jdbc.update("INSERT INTO PARTIDAS(DIFICULDADE, MODO, MOVIMENTOS, PASSAGEIROS, "
                + "PONTOS, REGISTRADA_EM, RESULTADO, TEMPO_SEGUNDOS, JOGADOR_ID) VALUES "
                + "('MEDIO', 'HORDA', 1, 0, 0, CURRENT_TIMESTAMP, 'DERROTA', 1, ?)", jogador));
    }
}
