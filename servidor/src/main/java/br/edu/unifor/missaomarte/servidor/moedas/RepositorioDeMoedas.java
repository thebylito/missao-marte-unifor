package br.edu.unifor.missaomarte.servidor.moedas;

import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into moedas (jogador_id, origem, referencia, valor, registrado_em)
            values (:jogadorId, :origem, :referencia, :valor, :registradoEm) on conflict do nothing
            """, nativeQuery = true)
    int inserirCredito(long jogadorId, String origem, String referencia, int valor, OffsetDateTime registradoEm);
}
