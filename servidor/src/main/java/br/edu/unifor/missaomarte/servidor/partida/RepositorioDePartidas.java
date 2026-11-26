package br.edu.unifor.missaomarte.servidor.partida;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioDePartidas extends JpaRepository<Partida, Long> {

    /** Histórico de um jogador, da mais recente para a mais antiga. */
    List<Partida> findByJogadorIdOrderByRegistradaEmDescIdDesc(Long jogadorId, Pageable limite);

    /**
     * Vitórias de um modo, da maior para a menor pontuação; no empate, a mais antiga primeiro. O jogador vem junto
     * (join fetch) para o ranking não fazer uma consulta extra por linha.
     */
    @Query("select p from Partida p join fetch p.jogador where p.resultado = :resultado and p.modo = :modo "
            + "order by p.pontos desc, p.registradaEm asc, p.id asc")
    List<Partida> melhoresPrimeiro(Resultado resultado, Modo modo);

    @Query("select p from Partida p join fetch p.jogador where p.resultado = :resultado and p.modo = :modo "
            + "and p.hordaNumero = :numero order by p.pontos desc, p.registradaEm asc, p.id asc")
    List<Partida> melhoresDaHorda(Resultado resultado, Modo modo, long numero);
}
