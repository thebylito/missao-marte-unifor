package br.edu.unifor.missaomarte.servidor.jogador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

/**
 * Entidade JPA de um jogador. Fica dentro do servidor: a API nunca a devolve (usa {@link JogadorResposta}), então o
 * hash da senha não tem como vazar numa resposta.
 */
@Entity
@Table(name = "jogadores")
public class Jogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Único sem diferenciar maiúsculas: a checagem fica no serviço, e o índice único garante no banco. */
    @Column(nullable = false, unique = true, length = 20)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /**
     * Fase 8: o id da skin equipada (catálogo da loja). O valor padrão no banco faz os jogadores que já existiam
     * ficarem com a nave de sempre ao aplicar a migração V2.
     */
    @ColumnDefault("'padrao'")
    @Column(nullable = false, length = 32)
    private String skin = "padrao";

    protected Jogador() {
        // exigido pelo JPA
    }

    public Jogador(String nome, String senhaHash, Instant criadoEm) {
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
    public Instant getCriadoEm() { return criadoEm; }
    public String getSkin() { return skin; }

    /** Troca a skin equipada; quem chama já conferiu que o jogador tem essa skin. */
    public void equipar(String skin) {
        this.skin = skin;
    }
}
