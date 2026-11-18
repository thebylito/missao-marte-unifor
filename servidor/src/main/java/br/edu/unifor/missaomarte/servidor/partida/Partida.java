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

/** Uma partida concluída, sempre de um jogador (o dono do token que a enviou). */
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
    @Column(nullable = false, length = 10)
    private Resultado resultado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
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
}
