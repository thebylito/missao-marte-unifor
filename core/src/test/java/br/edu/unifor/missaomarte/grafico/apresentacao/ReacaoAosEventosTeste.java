package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Evento.Tipo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos.Impacto;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos.Reacao;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Evento da Partida → som e efeito. Nenhuma posição ou vida é olhada aqui: só os eventos. */
class ReacaoAosEventosTeste {

    private static Reacao reacao(Comando c, Tipo... tipos) {
        return ReacaoAosEventos.para(c, List.of(tipos).stream().map(Evento::de).toList());
    }

    @Test
    void movimentoSemNovidadeSoTocaOPropulsor() {
        assertEquals(new Reacao(List.of(Som.MOVER), Impacto.NENHUM), reacao(Comando.DIREITA));
    }

    @Test
    void embarqueTocaOSomDeEmbarqueSemMovimento() {
        assertEquals(new Reacao(List.of(Som.EMBARQUE), Impacto.NENHUM),
                reacao(Comando.EMBARCAR, Tipo.PASSAGEIRO_EMBARCADO));
    }

    @Test
    void acoesSemEfeitoTocamOZumbido() {
        assertEquals(List.of(Som.INVALIDO), reacao(Comando.EMBARCAR, Tipo.SEM_PASSAGEIRO_NA_POSICAO).sons());
        assertEquals(List.of(Som.INVALIDO), reacao(Comando.EMBARCAR, Tipo.NAVE_CHEIA).sons());
    }

    @Test
    void colisaoEhImpactoLeveENaveDestruidaEhFatal() {
        assertEquals(new Reacao(List.of(Som.MOVER, Som.COLISAO), Impacto.LEVE), reacao(Comando.CIMA, Tipo.COLISAO));
        assertEquals(new Reacao(List.of(Som.MOVER, Som.EXPLOSAO), Impacto.FATAL),
                reacao(Comando.CIMA, Tipo.NAVE_DESTRUIDA));
    }

    @Test
    void todosABordoTocaOAlerta() {
        assertEquals(List.of(Som.EMBARQUE, Som.ALERTA),
                reacao(Comando.EMBARCAR, Tipo.PASSAGEIRO_EMBARCADO, Tipo.RETORNE_A_PLATAFORMA).sons());
    }

    @Test
    void fimDaPartidaNaoTocaNadaNoTurnoPorqueATelaDeFimToca() {
        assertEquals(List.of(Som.MOVER), reacao(Comando.ESQUERDA, Tipo.VITORIA).sons());
        assertEquals(List.of(Som.MOVER), reacao(Comando.ESQUERDA, Tipo.PONTOS_ESGOTADOS).sons());
        assertEquals(List.of(), reacao(Comando.ABORTAR, Tipo.ABORTADA).sons());
    }

    @Test
    void somDaTelaDeFim() {
        assertEquals(Optional.of(Som.VITORIA), ReacaoAosEventos.somDoFim(Partida.Estado.VITORIA));
        assertEquals(Optional.of(Som.DERROTA), ReacaoAosEventos.somDoFim(Partida.Estado.DERROTA));
        assertEquals(Optional.empty(), ReacaoAosEventos.somDoFim(Partida.Estado.ABORTADA));
    }

    @Test
    void cadaSomApareceUmaVezPorTurno() {
        assertEquals(List.of(Som.MOVER, Som.COLISAO),
                reacao(Comando.BAIXO, Tipo.COLISAO, Tipo.COLISAO).sons());
    }
}
