package br.edu.unifor.missaomarte.servidor.moedas;

import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.partida.Dificuldade;
import br.edu.unifor.missaomarte.servidor.partida.Partida;
import br.edu.unifor.missaomarte.servidor.partida.Resultado;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saldo e movimentos de moedas (Fase 8): crédito único por origem e referência e saldo como soma dos movimentos.
 *
 * <p>Quanto vale uma vitória (só com bilhete válido, conferido antes, em {@code ServicoDeBilhetes}):
 * <ul>
 *   <li>Clássico: {@value #VITORIA_NO_CLASSICO}, até {@value #TETO_DIARIO_DO_CLASSICO} por dia de Fortaleza (a
 *   vitória no Fácil leva de 8 a 10 s);</li>
 *   <li>Pilotagem Livre: {@value #VITORIA_NO_LIVRE}, mais {@value #BONUS_DO_DIFICIL_NO_LIVRE} no Difícil.</li>
 * </ul>
 * Derrota e abortada não dão nada. A Horda e os fechamentos entram nas etapas 4 e 6.
 */
@Service
public class ServicoDeMoedas {

    public static final int VITORIA_NO_CLASSICO = 1;
    public static final int TETO_DIARIO_DO_CLASSICO = 20;
    public static final int VITORIA_NO_LIVRE = 3;
    public static final int BONUS_DO_DIFICIL_NO_LIVRE = 1;
    /** O dia do teto é o de Fortaleza (UTC-3, sem horário de verão), o mesmo relógio das hordas. */
    public static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");

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

    /**
     * Desconta {@code valor} moedas (o movimento fica negativo). Não confere o saldo: quem chama trava o jogador e
     * confere antes (a compra da loja).
     */
    @Transactional
    public void debitar(long jogadorId, Origem origem, String referencia, int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("o débito não pode ser negativo: " + valor);
        }
        lancar(jogadorId, origem, referencia, -valor);
    }

    /**
     * Credita a vitória de uma partida cujo bilhete já foi conferido. Devolve as moedas ganhas: 0 na derrota, na
     * abortada e no Clássico depois do teto do dia. A referência é {@code partida:<id>}.
     */
    @Transactional
    public int creditarVitoria(Partida partida) {
        if (partida.getResultado() != Resultado.VITORIA) {
            return 0;
        }
        long jogadorId = partida.getJogador().getId();
        String referencia = "partida:" + partida.getId();
        return switch (partida.getModo()) {
            case CLASSICO -> {
                int valor = Math.min(VITORIA_NO_CLASSICO, TETO_DIARIO_DO_CLASSICO - ganhasHojeNoClassico(jogadorId));
                yield valor > 0 ? creditar(jogadorId, Origem.VITORIA_CLASSICO, referencia, valor) : 0;
            }
            case PILOTAGEM -> creditar(jogadorId, Origem.VITORIA_LIVRE, referencia, VITORIA_NO_LIVRE
                    + (partida.getDificuldade() == Dificuldade.DIFICIL ? BONUS_DO_DIFICIL_NO_LIVRE : 0));
        };
    }

    /** As moedas do Clássico já ganhas hoje, no dia civil de Fortaleza (não no fuso do relógio do servidor). */
    private int ganhasHojeNoClassico(long jogadorId) {
        LocalDate hoje = ZonedDateTime.ofInstant(relogio.instant(), FORTALEZA).toLocalDate();
        Instant inicio = hoje.atStartOfDay(FORTALEZA).toInstant();
        Instant fim = hoje.plusDays(1).atStartOfDay(FORTALEZA).toInstant();
        return Math.toIntExact(extrato.somaNoPeriodo(jogadorId, Origem.VITORIA_CLASSICO, inicio, fim));
    }

    private void lancar(long jogadorId, Origem origem, String referencia, int valor) {
        extrato.save(new MovimentoDeMoedas(jogadores.getReferenceById(jogadorId), origem, referencia, valor,
                relogio.instant()));
    }
}
