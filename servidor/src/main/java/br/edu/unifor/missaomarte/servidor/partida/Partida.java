package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma partida concluída, sempre de um jogador (o dono do token que a enviou).
 *
 * <p>Fase 8: o esquema é o da migração {@code db/migration/V1__esquema_inicial.sql} (DA-41), e o Hibernate só o
 * confere ({@code ddl-auto=validate}). As colunas de enum são texto (DA-40): {@code @JdbcTypeCode(SqlTypes.VARCHAR)}
 * faz o Hibernate esperar {@code varchar(20)}, como na migração, em vez de um {@code ENUM} nativo do H2.
 */
@Entity
@Table(name = "partidas", indexes = {
        @Index(name = "ix_partidas_jogador", columnList = "jogador_id, registrada_em"),
        @Index(name = "ix_partidas_ranking", columnList = "resultado, pontos")
})
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Resultado resultado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Dificuldade dificuldade;

    @Column(nullable = false)
    private int pontos;

    @Column(nullable = false)
    private int passageiros;

    @Column(nullable = false)
    private int movimentos;

    @Column(name = "tempo_segundos", nullable = false)
    private long tempoSegundos;

    @Column(name = "registrada_em", nullable = false)
    private Instant registradaEm;

    /**
     * Fase 7. O valor padrão no banco ({@code default 'CLASSICO'}, na migração V1) faz as partidas já gravadas (Fases
     * 5 e 6) contarem como clássicas.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'CLASSICO'")
    @Column(nullable = false, length = 20)
    private Modo modo = Modo.CLASSICO;

    @Column(name = "horda_numero")
    private Long hordaNumero;

    protected Partida() {
        // exigido pelo JPA
    }

    public Partida(Jogador jogador, RegistroDePartida dados, Instant registradaEm) {
        this.jogador = jogador;
        this.resultado = dados.resultado();
        this.dificuldade = dados.dificuldade();
        this.pontos = dados.pontos();
        this.passageiros = dados.passageiros();
        this.movimentos = dados.movimentos();
        this.tempoSegundos = dados.tempoSegundos();
        this.registradaEm = registradaEm;
        this.modo = dados.modo();
        this.hordaNumero = modo == Modo.HORDA ? dados.horda() : null;
    }

    public Long getId() { return id; }
    public Jogador getJogador() { return jogador; }
    public Resultado getResultado() { return resultado; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public int getPontos() { return pontos; }
    public int getPassageiros() { return passageiros; }
    public int getMovimentos() { return movimentos; }
    public long getTempoSegundos() { return tempoSegundos; }
    public Instant getRegistradaEm() { return registradaEm; }
    public Modo getModo() { return modo; }
    public Long getHordaNumero() { return hordaNumero; }
}
