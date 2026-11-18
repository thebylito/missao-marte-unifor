package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import java.util.List;
import java.util.Optional;

/**
 * Como a partida terminou, para a tela de fim mostrar.
 *
 * @param estado         VITORIA, DERROTA ou ABORTADA
 * @param eventosFinais  eventos do último turno (por exemplo, NAVE_DESTRUIDA ou VITORIA)
 * @param resultado      números da partida; presente só na vitória
 * @param liderAnterior  o primeiro do ranking antes desta partida (para o "recorde a ser batido")
 * @param entrouNoTop    a vitória entrou no Top 5
 * @param erroAoSalvar   motivo, se o ranking não pôde ser gravado
 */
public record FimDePartida(
        Partida.Estado estado,
        List<Evento> eventosFinais,
        Optional<ResultadoPartida> resultado,
        Optional<RegistroRanking> liderAnterior,
        boolean entrouNoTop,
        Optional<String> erroAoSalvar) {

    public FimDePartida {
        eventosFinais = List.copyOf(eventosFinais);
    }
}
