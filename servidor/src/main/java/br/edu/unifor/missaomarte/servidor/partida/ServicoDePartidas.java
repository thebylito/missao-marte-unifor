package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeRanking;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registrar partidas e consultar o histórico. O jogador vem sempre do token, nunca do corpo do pedido. Fase 8: a
 * partida que cita um bilhete o gasta, e a vitória com um bilhete válido rende moedas.
 */
@Service
public class ServicoDePartidas {

    /** Tamanho máximo do histórico devolvido (as mais recentes). */
    public static final int LIMITE_DO_HISTORICO = 50;

    private final RepositorioDePartidas partidas;
    private final RepositorioDeJogadores jogadores;
    private final ServicoDeBilhetes bilhetes;
    private final ServicoDeMoedas moedas;
    private final Clock relogio;
    private final ServicoDeHordas hordas;
    private final ServicoDeRanking ranking;
    private final AjustesDeVoo ajustes;

    public ServicoDePartidas(RepositorioDePartidas partidas, RepositorioDeJogadores jogadores,
                             ServicoDeBilhetes bilhetes, ServicoDeMoedas moedas, Clock relogio,
                             ServicoDeHordas hordas, ServicoDeRanking ranking, AjustesDeVoo ajustes) {
        this.ajustes = ajustes;
        this.partidas = partidas;
        this.jogadores = jogadores;
        this.bilhetes = bilhetes;
        this.moedas = moedas;
        this.relogio = relogio;
        this.hordas = hordas;
        this.ranking = ranking;
    }

    /**
     * Grava a partida. Sem bilhete, como sempre (e sem moedas). Com bilhete, o jogador fica travado até o fim, para
     * duas partidas ao mesmo tempo não usarem o mesmo bilhete nem passarem juntas do teto do Clássico.
     */
    @Transactional
    public PartidaRegistrada registrar(long jogadorId, RegistroDePartida dados) {
        if (dados.modo() == Modo.HORDA) {
            return registrarNaHorda(jogadorId, dados);
        }
        if (comRegraAntiga(jogadorId, dados)) {
            throw ServicoDeBilhetes.jogoDesatualizado(
                    "Esta partida usou as regras antigas. Atualize o jogo pelo site para entrar no ranking.");
        }

        boolean citaBilhete = dados.bilhete() != null;
        var encontrado = citaBilhete ? jogadores.travar(jogadorId) : jogadores.findById(jogadorId);
        Jogador jogador = encontrado.orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        Partida partida = partidas.save(new Partida(jogador, dados, relogio.instant()));
        boolean valeMoedas = citaBilhete && bilhetes.consumir(jogadorId, dados.bilhete(), partida);
        int ganhas = valeMoedas ? moedas.creditarVitoria(partida) : 0;
        return PartidaRegistrada.de(partida, ganhas, moedas.saldo(jogadorId), null);
    }

    /**
     * DA-48: na Pilotagem Livre, com ajustes diferentes do padrão, o jogo que não os segue (o publicado antes deles)
     * jogou com a regra velha. Só vale se começou a partida quando os ajustes ainda eram o padrão (o bilhete diz).
     */
    private boolean comRegraAntiga(long jogadorId, RegistroDePartida dados) {
        return dados.modo() == Modo.PILOTAGEM && !Boolean.TRUE.equals(dados.comAjustes())
                && !ajustes.equals(AjustesDeVoo.PADRAO) && !bilhetes.comecouComOPadrao(jogadorId, dados.bilhete());
    }

    private PartidaRegistrada registrarNaHorda(long jogadorId, RegistroDePartida dados) {
        if (dados.bilhete() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a horda exige um bilhete válido");
        }
        if (dados.pontos() > 75) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a horda vale no máximo 75 pontos");
        }
        var estado = hordas.travarParaPartida(dados.horda());
        Jogador jogador = jogadores.travar(jogadorId).orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        BilheteDePartida bilhete = bilhetes.daHorda(jogadorId, dados).orElse(null);
        if (bilhete != null && bilhete.getEstado() == EstadoDoBilhete.USADO) {
            return repetirRegistro(bilhete, dados);
        }

        Instant admitidaEm = hordas.conferirPartida(dados.horda(), dados.dificuldade(), estado);
        // A resposta inicial e a relida do banco usam a mesma precisão da coluna registrada_em.
        Partida partida = new Partida(jogador, dados, admitidaEm.truncatedTo(ChronoUnit.MICROS));
        bilhetes.conferirParaHorda(bilhete, partida, admitidaEm);
        partida = partidas.save(partida);
        bilhete.usar(partida, partida.getRegistradaEm());
        var resposta = respostaDaHorda(partida, moedas.creditarVitoria(partida));
        bilhete.guardarResposta(resposta);
        return resposta;
    }

    private PartidaRegistrada repetirRegistro(BilheteDePartida bilhete, RegistroDePartida dados) {
        Partida partida = bilhete.getPartida();
        if (partida == null || partida.getModo() != Modo.HORDA || !Objects.equals(partida.getHordaNumero(), dados.horda())
                || partida.getResultado() != dados.resultado() || partida.getDificuldade() != dados.dificuldade()
                || partida.getPontos() != dados.pontos() || partida.getPassageiros() != dados.passageiros()
                || partida.getMovimentos() != dados.movimentos() || partida.getTempoSegundos() != dados.tempoSegundos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "o bilhete já foi usado em outra partida");
        }
        var guardada = bilhete.respostaGuardada();
        if (guardada != null) {
            return guardada;
        }
        // Bilhetes anteriores à V5 têm o crédito no extrato, mas não guardavam posição e saldo da resposta.
        var resposta = respostaDaHorda(partida, moedas.ganhasNaHorda(partida));
        bilhete.guardarResposta(resposta);
        return resposta;
    }

    private PartidaRegistrada respostaDaHorda(Partida partida, int ganhas) {
        long jogadorId = partida.getJogador().getId();
        Integer colocacao = ranking.melhores(Modo.HORDA, partida.getHordaNumero()).stream()
                .filter(v -> v.partida().getJogador().getId() == jogadorId)
                .map(ServicoDeRanking.VitoriaNoRanking::posicao).findFirst().orElse(null);
        return PartidaRegistrada.de(partida, ganhas, moedas.saldo(jogadorId), colocacao);
    }

    @Transactional(readOnly = true)
    public List<PartidaResposta> historico(long jogadorId) {
        return partidas.findByJogadorIdOrderByRegistradaEmDescIdDesc(jogadorId, PageRequest.of(0, LIMITE_DO_HISTORICO))
                .stream().map(PartidaResposta::de).toList();
    }
}
