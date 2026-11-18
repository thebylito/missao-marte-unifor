package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registrar partidas e consultar o histórico. O jogador vem sempre do token, nunca do corpo do pedido. */
@Service
public class ServicoDePartidas {

    /** Tamanho máximo do histórico devolvido (as mais recentes). */
    public static final int LIMITE_DO_HISTORICO = 50;

    private final RepositorioDePartidas partidas;
    private final RepositorioDeJogadores jogadores;
    private final Clock relogio;

    public ServicoDePartidas(RepositorioDePartidas partidas, RepositorioDeJogadores jogadores, Clock relogio) {
        this.partidas = partidas;
        this.jogadores = jogadores;
        this.relogio = relogio;
    }

    @Transactional
    public PartidaResposta registrar(long jogadorId, RegistroDePartida dados) {
        Jogador jogador = jogadores.findById(jogadorId)
                .orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        return PartidaResposta.de(partidas.save(new Partida(jogador, dados, relogio.instant())));
    }

    @Transactional(readOnly = true)
    public List<PartidaResposta> historico(long jogadorId) {
        return partidas.findByJogadorIdOrderByRegistradaEmDescIdDesc(jogadorId, PageRequest.of(0, LIMITE_DO_HISTORICO))
                .stream().map(PartidaResposta::de).toList();
    }
}
