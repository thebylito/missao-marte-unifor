package br.edu.unifor.missaomarte.servidor.moedas;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma linha do extrato de moedas (Fase 8): crédito (valor positivo) ou compra (negativo). O saldo é a soma das
 * linhas do jogador, então nunca há um "saldo" gravado que possa divergir do extrato.
 *
 * <p>A restrição única (jogador, origem, referência) garante no banco que o mesmo crédito nunca entra duas vezes,
 * mesmo que o serviço seja chamado de novo. A origem é texto, não {@code ENUM} nativo do H2 (DA-40).
 */
@Entity
@Table(name = "moedas",
        uniqueConstraints = @UniqueConstraint(name = "uk_moedas_credito",
                columnNames = {"jogador_id", "origem", "referencia"}),
        indexes = @Index(name = "ix_moedas_jogador", columnList = "jogador_id, origem, registrado_em"))
public class MovimentoDeMoedas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Origem origem;

    /** O que gerou o movimento, por exemplo {@code partida:123} ou {@code skin:guara}. */
    @Column(nullable = false, length = 64)
    private String referencia;

    @Column(nullable = false)
    private int valor;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    protected MovimentoDeMoedas() {
        // exigido pelo JPA
    }

    public MovimentoDeMoedas(Jogador jogador, Origem origem, String referencia, int valor, Instant registradoEm) {
        this.jogador = jogador;
        this.origem = origem;
        this.referencia = referencia;
        this.valor = valor;
        this.registradoEm = registradoEm;
    }

    public Long getId() { return id; }
    public Jogador getJogador() { return jogador; }
    public Origem getOrigem() { return origem; }
    public String getReferencia() { return referencia; }
    public int getValor() { return valor; }
    public Instant getRegistradoEm() { return registradoEm; }
}
