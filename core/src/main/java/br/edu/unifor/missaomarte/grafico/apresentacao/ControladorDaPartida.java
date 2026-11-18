package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Ranking;
import java.util.List;
import java.util.Optional;

/**
 * Liga o teclado da janela à {@link Partida} (padrão Humble Object): toda decisão da tela de jogo está
 * aqui, em uma classe que não usa OpenGL e pode ser testada sem abrir janela. A tela só repassa as teclas
 * e desenha o que este controlador expõe.
 *
 * <p>As regras continuam na Partida. Este controlador faz o mesmo papel do laço de turnos do
 * {@code JogoConsole}: traduz a entrada em {@link Comando}, guarda os eventos para mostrar e, na vitória,
 * chama o {@link ServicoRanking}.
 */
public final class ControladorDaPartida {

    private final Partida partida;
    private final ServicoRanking servicoRanking;
    private final Ranking rankingAntes;
    private List<Evento> ultimosEventos = List.of();
    private FimDePartida fim;

    public ControladorDaPartida(Partida partida, ServicoRanking servicoRanking) {
        this.partida = partida;
        this.servicoRanking = servicoRanking;
        // como no console: o ranking é lido antes da partida e serve para o recorde e para o Top 5
        this.rankingAntes = servicoRanking.carregar();
    }

    /**
     * Uma tecla pressionada. Devolve true se ela virou um turno (ou o abandono da partida); teclas que o jogo
     * não usa e teclas depois do fim são ignoradas.
     */
    public boolean teclaPressionada(int tecla) {
        if (fim != null) {
            return false;
        }
        Optional<Comando> comando = MapaDeTeclas.comandoPara(tecla);
        if (comando.isEmpty()) {
            return false;
        }
        ultimosEventos = partida.executar(comando.get());
        if (!partida.emAndamento()) {
            fim = concluir();
        }
        return true;
    }

    private FimDePartida concluir() {
        if (partida.getEstado() != Partida.Estado.VITORIA) {
            return new FimDePartida(partida.getEstado(), ultimosEventos, Optional.empty(),
                    rankingAntes.getLider(), false, Optional.empty());
        }
        ResultadoPartida resultado = partida.getResultado();
        ServicoRanking.Registro registro = servicoRanking.registrarVitoria(rankingAntes, resultado);
        return new FimDePartida(Partida.Estado.VITORIA, ultimosEventos, Optional.of(resultado),
                rankingAntes.getLider(), registro.entrou(), Optional.ofNullable(registro.erroAoSalvar()));
    }

    public Partida getPartida() {
        return partida;
    }

    /** Eventos do último turno, na ordem em que aconteceram. */
    public List<Evento> getUltimosEventos() {
        return ultimosEventos;
    }

    /** Presente quando a partida terminou: a tela deve ir para o fim de partida. */
    public Optional<FimDePartida> getFim() {
        return Optional.ofNullable(fim);
    }
}
