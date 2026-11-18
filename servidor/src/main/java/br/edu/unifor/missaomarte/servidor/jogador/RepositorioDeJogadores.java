package br.edu.unifor.missaomarte.servidor.jogador;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de jogadores; o Spring Data gera a implementação a partir do nome dos métodos. */
public interface RepositorioDeJogadores extends JpaRepository<Jogador, Long> {

    Optional<Jogador> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);
}
