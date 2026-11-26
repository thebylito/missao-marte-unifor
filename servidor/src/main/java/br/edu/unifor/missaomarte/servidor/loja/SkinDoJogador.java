package br.edu.unifor.missaomarte.servidor.loja;

import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Fase 8: uma skin comprada por um jogador. A {@code padrao} não aparece aqui: ela é de todo jogador. A restrição
 * única (jogador, skin) garante no banco que ninguém tem a mesma skin duas vezes.
 */
@Entity
@Table(name = "skins_do_jogador",
        uniqueConstraints = @UniqueConstraint(name = "uk_skins_do_jogador", columnNames = {"jogador_id", "skin"}))
public class SkinDoJogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    /** O id da skin no catálogo. */
    @Column(nullable = false, length = 32)
    private String skin;

    @Column(name = "comprada_em", nullable = false)
    private Instant compradaEm;

    protected SkinDoJogador() {
        // exigido pelo JPA
    }

    public SkinDoJogador(Jogador jogador, String skin, Instant compradaEm) {
        this.jogador = jogador;
        this.skin = skin;
        this.compradaEm = compradaEm;
    }

    public Long getId() { return id; }
    public Jogador getJogador() { return jogador; }
    public String getSkin() { return skin; }
    public Instant getCompradaEm() { return compradaEm; }
}
