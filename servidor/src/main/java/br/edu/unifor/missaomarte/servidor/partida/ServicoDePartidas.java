package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeRanking;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    public ServicoDePartidas(RepositorioDePartidas partidas, RepositorioDeJogadores jogadores,
                             ServicoDeBilhetes bilhetes, ServicoDeMoedas moedas, Clock relogio,
                             ServicoDeHordas hordas, ServicoDeRanking ranking) {
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
        boolean horda = dados.modo() == Modo.HORDA;
        boolean citaBilhete = dados.bilhete() != null;
        Instant admitidaEm = horda ? admitirNaHorda(dados) : null;
        Optional<Jogador> encontrado = citaBilhete ? jogadores.travar(jogadorId) : jogadores.findById(jogadorId);
        Jogador jogador = encontrado.orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        Partida partida = new Partida(jogador, dados, horda ? admitidaEm : relogio.instant());
        BilheteDePartida bilheteDaHorda = horda ? bilhetes.conferirParaHorda(jogadorId, dados.bilhete(), partida) : null;
        partida = partidas.save(partida);
        boolean valeMoedas;
        if (bilheteDaHorda != null) {
            bilheteDaHorda.usar(partida, partida.getRegistradaEm());
            valeMoedas = true;
        } else {
            valeMoedas = citaBilhete && bilhetes.consumir(jogadorId, dados.bilhete(), partida);
        }
        int ganhas = valeMoedas ? moedas.creditarVitoria(partida) : 0;
        Integer colocacao = horda ? ranking.melhores(Modo.HORDA, dados.horda()).stream()
                .filter(v -> v.partida().getJogador().getId() == jogadorId)
                .map(ServicoDeRanking.VitoriaNoRanking::posicao).findFirst().orElse(null) : null;
        return PartidaRegistrada.de(partida, ganhas, moedas.saldo(jogadorId), colocacao);
    }

    private Instant admitirNaHorda(RegistroDePartida dados) {
        if (dados.bilhete() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a horda exige um bilhete válido");
        }
        if (dados.pontos() > 75) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a horda vale no máximo 75 pontos");
        }
        return hordas.conferirPartida(dados.horda(), dados.dificuldade());
    }

    @Transactional(readOnly = true)
    public List<PartidaResposta> historico(long jogadorId) {
        return partidas.findByJogadorIdOrderByRegistradaEmDescIdDesc(jogadorId, PageRequest.of(0, LIMITE_DO_HISTORICO))
                .stream().map(PartidaResposta::de).toList();
    }
}
