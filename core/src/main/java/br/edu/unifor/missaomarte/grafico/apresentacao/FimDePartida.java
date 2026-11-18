package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
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
 * @param paraOServidor  resumo da partida no formato da API (Fase 5), enviado se houver um jogador conectado
 * @param modo           clássico ou pilotagem (Fase 7)
 * @param voo            na pilotagem, os números do voo (bônus de combustível e pousos)
 */
public record FimDePartida(
        Partida.Estado estado,
        List<Evento> eventosFinais,
        Optional<ResultadoPartida> resultado,
        Optional<RegistroRanking> liderAnterior,
        boolean entrouNoTop,
        Optional<String> erroAoSalvar,
        PartidaParaEnviar paraOServidor,
        Modo modo,
        Optional<ResumoDoVoo> voo) {

    /** Os números que só a pilotagem tem: os pontos dos resgates, o bônus de combustível e os pousos. */
    public record ResumoDoVoo(int pontosDosResgates, int bonus, int pousos) {
    }

    public FimDePartida {
        eventosFinais = List.copyOf(eventosFinais);
    }

    /** O fim de uma partida clássica (o formato das Fases 3 a 6). */
    public FimDePartida(Partida.Estado estado, List<Evento> eventosFinais, Optional<ResultadoPartida> resultado,
                        Optional<RegistroRanking> liderAnterior, boolean entrouNoTop, Optional<String> erroAoSalvar,
                        PartidaParaEnviar paraOServidor) {
        this(estado, eventosFinais, resultado, liderAnterior, entrouNoTop, erroAoSalvar, paraOServidor, Modo.CLASSICO,
                Optional.empty());
    }
}
