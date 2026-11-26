package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 8: o bilhete de partida. Regras:
 * <ul>
 *   <li>um bilhete ativo por jogador: pedir outro anula o anterior;</li>
 *   <li>vale por {@link #VALIDADE} e é usado uma vez, qualquer que seja o resultado da partida que o cita;</li>
 *   <li>a vitória só rende moedas se o bilhete é do mesmo jogador, está ativo e dentro da validade, tem o mesmo modo
 *   e a mesma dificuldade da partida, e se passou tempo real suficiente desde que ele foi emitido: pelo menos o
 *   tempo da partida menos {@link #FOLGA}, e nunca menos que o mínimo do modo (5 s no Clássico, 45 s na
 *   Pilotagem e na Horda).</li>
 * </ul>
 * Um bilhete de outro jogador é ignorado e não é gasto: ninguém queima o bilhete de outro piloto.
 */
@Service
public class ServicoDeBilhetes {

    public static final Duration VALIDADE = Duration.ofHours(2);
    /** Quanto o tempo real pode ficar abaixo do tempo da partida (arredondamentos, a ida e a volta da rede). */
    public static final Duration FOLGA = Duration.ofSeconds(10);

    private final RepositorioDeBilhetes bilhetes;
    private final RepositorioDeJogadores jogadores;
    private final Clock relogio;
    private final ServicoDeHordas hordas;

    public ServicoDeBilhetes(RepositorioDeBilhetes bilhetes, RepositorioDeJogadores jogadores, Clock relogio,
                             ServicoDeHordas hordas) {
        this.bilhetes = bilhetes;
        this.jogadores = jogadores;
        this.relogio = relogio;
        this.hordas = hordas;
    }

    /** O tempo real mínimo de uma partida, mesmo curta: a vitória mais rápida no Fácil leva de 8 a 10 s. */
    public static Duration minimo(Modo modo) {
        return switch (modo) {
            case CLASSICO -> Duration.ofSeconds(5);
            case PILOTAGEM, HORDA -> Duration.ofSeconds(45);
        };
    }

    /** Emite um bilhete novo e anula o que estiver ativo. O jogador fica travado para não sobrarem dois ativos. */
    @Transactional
    public BilheteResposta emitir(long jogadorId, InicioDePartida pedido) {
        boolean horda = pedido.modo() == Modo.HORDA;
        Instant admitidaEm = horda ? hordas.conferirPartida(pedido.horda(), pedido.dificuldade()) : null;
        Jogador jogador = jogadores.travar(jogadorId).orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        Instant agora = horda ? admitidaEm : relogio.instant();
        bilhetes.findByJogadorIdAndEstado(jogadorId, EstadoDoBilhete.ATIVO).forEach(b -> b.anular(agora));
        BilheteDePartida novo = bilhetes.save(new BilheteDePartida(UUID.randomUUID(), jogador, pedido.modo(),
                pedido.dificuldade(), agora, agora.plus(VALIDADE), horda ? pedido.horda() : null));
        return new BilheteResposta(novo.getCodigo(), novo.getValidoAte());
    }

    /**
     * Gasta o bilhete citado pela partida (se ele for do jogador e estiver ativo) e diz se ele vale moedas. Quem chama
     * já travou o jogador, então duas partidas ao mesmo tempo não usam o mesmo bilhete.
     */
    @Transactional
    public boolean consumir(long jogadorId, String codigo, Partida partida) {
        var citado = ativoDoJogador(jogadorId, codigo).filter(b -> b.getModo() != Modo.HORDA);
        if (citado.isEmpty()) {
            return false;
        }
        BilheteDePartida bilhete = citado.get();
        Instant agora = relogio.instant();
        bilhete.usar(partida, agora);
        return valeMoedas(bilhete, partida, agora);
    }

    /** Na Horda, a validação vem antes da gravação. Quem chama já travou o jogador. */
    BilheteDePartida conferirParaHorda(long jogadorId, String codigo, Partida partida) {
        return ativoDoJogador(jogadorId, codigo)
                .filter(b -> valeMoedas(b, partida, partida.getRegistradaEm()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "a horda exige um bilhete válido"));
    }

    private Optional<BilheteDePartida> ativoDoJogador(long jogadorId, String codigo) {
        return ler(codigo).flatMap(bilhetes::findByCodigo)
                .filter(b -> b.getJogador().getId() == jogadorId && b.getEstado() == EstadoDoBilhete.ATIVO);
    }

    private static boolean valeMoedas(BilheteDePartida bilhete, Partida partida, Instant agora) {
        if (agora.isAfter(bilhete.getValidoAte()) || bilhete.getModo() != partida.getModo()
                || bilhete.getDificuldade() != partida.getDificuldade()
                || !Objects.equals(bilhete.getHordaNumero(), partida.getHordaNumero())) {
            return false;
        }
        Duration exigido = Duration.ofSeconds(partida.getTempoSegundos()).minus(FOLGA);
        if (exigido.compareTo(minimo(partida.getModo())) < 0) {
            exigido = minimo(partida.getModo());
        }
        return Duration.between(bilhete.getEmitidoEm(), agora).compareTo(exigido) >= 0;
    }

    /** Um código que não é UUID não é bilhete: a partida entra sem moedas, sem erro para o jogo. */
    private static Optional<UUID> ler(String codigo) {
        try {
            return Optional.of(UUID.fromString(codigo.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
