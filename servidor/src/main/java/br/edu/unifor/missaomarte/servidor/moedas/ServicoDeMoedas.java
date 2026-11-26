package br.edu.unifor.missaomarte.servidor.moedas;

import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saldo e movimentos de moedas (Fase 8). Quem chama decide quanto vale cada coisa (a vitória, a compra); este
 * serviço garante o extrato: crédito único por origem e referência e saldo como soma dos movimentos.
 */
@Service
public class ServicoDeMoedas {

    private final RepositorioDeMoedas extrato;
    private final RepositorioDeJogadores jogadores;
    private final Clock relogio;

    public ServicoDeMoedas(RepositorioDeMoedas extrato, RepositorioDeJogadores jogadores, Clock relogio) {
        this.extrato = extrato;
        this.jogadores = jogadores;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public int saldo(long jogadorId) {
        return Math.toIntExact(extrato.saldo(jogadorId));
    }

    /**
     * Credita {@code valor} moedas, uma vez só: se o extrato já tem esta origem com esta referência, não faz nada.
     * Devolve quanto foi creditado (0 quando o crédito já existia).
     */
    @Transactional
    public int creditar(long jogadorId, Origem origem, String referencia, int valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("o crédito precisa ser positivo: " + valor);
        }
        if (extrato.existsByJogadorIdAndOrigemAndReferencia(jogadorId, origem, referencia)) {
            return 0;
        }
        lancar(jogadorId, origem, referencia, valor);
        return valor;
    }

    private void lancar(long jogadorId, Origem origem, String referencia, int valor) {
        extrato.save(new MovimentoDeMoedas(jogadores.getReferenceById(jogadorId), origem, referencia, valor,
                relogio.instant()));
    }
}
