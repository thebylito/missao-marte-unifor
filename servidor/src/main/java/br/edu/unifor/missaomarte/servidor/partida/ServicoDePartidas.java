package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ServicoDePartidas(RepositorioDePartidas partidas, RepositorioDeJogadores jogadores,
                             ServicoDeBilhetes bilhetes, ServicoDeMoedas moedas, Clock relogio) {
        this.partidas = partidas;
        this.jogadores = jogadores;
        this.bilhetes = bilhetes;
        this.moedas = moedas;
        this.relogio = relogio;
    }

    /**
     * Grava a partida. Sem bilhete, como sempre (e sem moedas). Com bilhete, o jogador fica travado até o fim, para
     * duas partidas ao mesmo tempo não usarem o mesmo bilhete nem passarem juntas do teto do Clássico.
     */
    @Transactional
    public PartidaRegistrada registrar(long jogadorId, RegistroDePartida dados) {
        boolean citaBilhete = dados.bilhete() != null;
        Optional<Jogador> encontrado = citaBilhete ? jogadores.travar(jogadorId) : jogadores.findById(jogadorId);
        Jogador jogador = encontrado.orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        Partida partida = partidas.save(new Partida(jogador, dados, relogio.instant()));
        boolean valeMoedas = citaBilhete && bilhetes.consumir(jogadorId, dados.bilhete(), partida);
        int ganhas = valeMoedas ? moedas.creditarVitoria(partida) : 0;
        return PartidaRegistrada.de(partida, ganhas, moedas.saldo(jogadorId));
    }

    @Transactional(readOnly = true)
    public List<PartidaResposta> historico(long jogadorId) {
        return partidas.findByJogadorIdOrderByRegistradaEmDescIdDesc(jogadorId, PageRequest.of(0, LIMITE_DO_HISTORICO))
                .stream().map(PartidaResposta::de).toList();
    }
}
