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
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fase 8: o bilhete que o servidor dá no começo de uma partida ({@code POST /api/partidas/inicio}). Só uma vitória
 * que cita um bilhete válido rende moedas (as regras ficam no {@link ServicoDeBilhetes}). É a defesa contra farm de
 * moedas por {@code curl}, já que o servidor não confere os pontos (DA-16).
 *
 * <p>As colunas de enum são texto, não {@code ENUM} nativo do H2 (DA-40).
 */
@Entity
@Table(name = "bilhetes", indexes = @Index(name = "ix_bilhetes_jogador", columnList = "jogador_id, estado"))
public class BilheteDePartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** O que o jogo recebe e manda de volta ao registrar a partida: aleatório, não dá para adivinhar. */
    @Column(nullable = false, unique = true)
    private UUID codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Modo modo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Dificuldade dificuldade;

    @Column(name = "emitido_em", nullable = false)
    private Instant emitidoEm;

    @Column(name = "valido_ate", nullable = false)
    private Instant validoAte;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoDoBilhete estado = EstadoDoBilhete.ATIVO;

    /** Quando deixou de ser ativo (usado ou anulado). */
    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    /** A partida que usou o bilhete (para conferência), ou nenhuma, se ele foi anulado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    protected BilheteDePartida() {
        // exigido pelo JPA
    }

    public BilheteDePartida(UUID codigo, Jogador jogador, Modo modo, Dificuldade dificuldade, Instant emitidoEm,
                            Instant validoAte) {
        this.codigo = codigo;
        this.jogador = jogador;
        this.modo = modo;
        this.dificuldade = dificuldade;
        this.emitidoEm = emitidoEm;
        this.validoAte = validoAte;
    }

    void usar(Partida partida, Instant agora) {
        this.estado = EstadoDoBilhete.USADO;
        this.partida = partida;
        this.encerradoEm = agora;
    }

    void anular(Instant agora) {
        this.estado = EstadoDoBilhete.ANULADO;
        this.encerradoEm = agora;
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public Jogador getJogador() { return jogador; }
    public Modo getModo() { return modo; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public Instant getEmitidoEm() { return emitidoEm; }
    public Instant getValidoAte() { return validoAte; }
    public EstadoDoBilhete getEstado() { return estado; }
    public Instant getEncerradoEm() { return encerradoEm; }
    public Partida getPartida() { return partida; }
}
