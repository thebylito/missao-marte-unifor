package br.edu.unifor.missaomarte.servidor.partida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDeBilhetes extends JpaRepository<BilheteDePartida, Long> {

    Optional<BilheteDePartida> findByCodigo(UUID codigo);

    List<BilheteDePartida> findByJogadorIdAndEstado(Long jogadorId, EstadoDoBilhete estado);
}
