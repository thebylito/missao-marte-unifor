package br.edu.unifor.missaomarte.servidor.moedas;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** O extrato de moedas. */
public interface RepositorioDeMoedas extends JpaRepository<MovimentoDeMoedas, Long> {

    /** A soma de todos os movimentos do jogador (0 sem nenhum). */
    @Query("select coalesce(sum(m.valor), 0) from MovimentoDeMoedas m where m.jogador.id = :jogadorId")
    long saldo(long jogadorId);

    /** A soma dos movimentos de uma origem registrados em [desde, ate): usada no teto diário do Clássico. */
    @Query("select coalesce(sum(m.valor), 0) from MovimentoDeMoedas m where m.jogador.id = :jogadorId "
            + "and m.origem = :origem and m.registradoEm >= :desde and m.registradoEm < :ate")
    long somaNoPeriodo(long jogadorId, Origem origem, Instant desde, Instant ate);

    boolean existsByJogadorIdAndOrigemAndReferencia(Long jogadorId, Origem origem, String referencia);
}
