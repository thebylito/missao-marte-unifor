package br.edu.unifor.missaomarte.servidor.loja;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDeSkinsDoJogador extends JpaRepository<SkinDoJogador, Long> {

    /** As skins compradas, na ordem da compra. */
    List<SkinDoJogador> findByJogadorIdOrderByCompradaEmAscIdAsc(Long jogadorId);

    boolean existsByJogadorIdAndSkin(Long jogadorId, String skin);
}
