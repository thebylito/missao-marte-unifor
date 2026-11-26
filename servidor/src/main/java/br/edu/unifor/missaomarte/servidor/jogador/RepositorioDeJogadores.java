package br.edu.unifor.missaomarte.servidor.jogador;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Consultas de jogadores; o Spring Data gera a implementação a partir do nome dos métodos. */
public interface RepositorioDeJogadores extends JpaRepository<Jogador, Long> {

    Optional<Jogador> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    /**
     * Fase 8: o jogador com a linha travada ({@code select ... for update}) até o fim da transação. Tudo que mexe
     * nas moedas ou no bilhete de um jogador passa por aqui, então duas operações dele nunca correm ao mesmo tempo
     * (duas compras com saldo para uma só, duas partidas com o mesmo bilhete).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jogador j where j.id = :id")
    Optional<Jogador> travar(long id);

    /** Fase 8: só a skin equipada do jogador (o canal das sombras lê na conexão). */
    @Query("select j.skin from Jogador j where j.id = :id")
    Optional<String> skinDe(long id);
}
