package br.edu.unifor.missaomarte.aplicacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Evento.Tipo;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;

/** Regras de um turno, com missão montada à mão, inimigos previsíveis e relógio fixo. */
class PartidaTeste {

    private final RelogioFixo relogio = new RelogioFixo();
    private int passosDosInimigos;

    /** Missão de -2 a 2 sem nada; os inimigos contam quantas vezes andaram (e ficam parados). */
    private Missao missao(int capacidade) {
        return new Missao(new Mapa(2), new Nave(capacidade), (atual, mapa) -> {
            passosDosInimigos++;
            return atual;
        });
    }

    /** Um inimigo parado num canto, só para contar os turnos em que os inimigos andam. */
    private static Missao comInimigoNoCanto(Missao m) {
        m.adicionarInimigo(new Inimigo(new Posicao(-2, 2)));
        return m;
    }

    private Partida partida(Missao m, Dificuldade d) {
        return new Partida("Teste", d, m, relogio);
    }

    private static List<Tipo> tipos(List<Evento> eventos) {
        return eventos.stream().map(Evento::tipo).collect(Collectors.toList());
    }

    @Test
    void comecaComOsPontosDaDificuldade() {
        for (Dificuldade d : Dificuldade.values()) {
            assertEquals(d.getPontosIniciais(), partida(missao(5), d).getPontos(), d.name());
        }
    }

    @Test
    void cadaMovimentoCustaUmPontoMesmoParadoNaBorda() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.DIREITA);
        p.executar(Comando.DIREITA);
        p.executar(Comando.DIREITA); // já está em x = 2: não sai do lugar, mas paga
        assertEquals(new Posicao(2, 0), m.getNave().getPosicao());
        assertEquals(17, p.getPontos());
        assertEquals(3, p.getMovimentos());
        assertEquals(3, passosDosInimigos, "inimigos andam a cada movimento");
    }

    @Test
    void comandoInvalidoNaoGastaOTurno() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        assertEquals(List.of(Tipo.COMANDO_INVALIDO), tipos(p.executar(Comando.INVALIDO)));
        assertEquals(20, p.getPontos());
        assertEquals(0, p.getMovimentos());
        assertEquals(0, passosDosInimigos, "inimigos não andam");
    }

    @Test
    void embarcarSomaAPontuacaoDoTipoEGastaOTurnoSemCustarPonto() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        List<Evento> eventos = p.executar(Comando.EMBARCAR);
        assertEquals(new Evento(Tipo.PASSAGEIRO_EMBARCADO, "Ast. Maria", 20), eventos.get(0));
        assertEquals(40, p.getPontos());
        assertEquals(0, p.getMovimentos());
        assertEquals(1, passosDosInimigos);
        assertEquals(1, m.getPassageiros().size());
    }

    @Test
    void embarcarSemPassageiroAvisaEAindaAssimOsInimigosAndam() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        assertEquals(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO), tipos(p.executar(Comando.EMBARCAR)));
        assertEquals(1, passosDosInimigos);
    }

    @Test
    void naveCheiaRecusaOEmbarque() {
        Missao m = missao(1);
        m.adicionarPassageiro(new Professor("A", Posicao.ORIGEM));
        m.adicionarPassageiro(new Engenheiro("B", Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.EMBARCAR);
        assertEquals(List.of(Tipo.NAVE_CHEIA), tipos(p.executar(Comando.EMBARCAR)));
        assertEquals(30, p.getPontos());
    }

    @Test
    void colisaoTiraUmaVidaEAvisaQuantasRestam() {
        Missao m = missao(5);
        m.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        assertEquals(List.of(new Evento(Tipo.COLISAO, null, 2)), p.executar(Comando.DIREITA));
        assertEquals(new Posicao(1, 0), m.getNave().getPosicao(), "a nave fica na célula do asteroide");
        assertTrue(p.emAndamento(), "ainda tem vidas");
    }

    @Test
    void ficarSobreOAsteroideTiraOutraVida() {
        // comportamento herdado da Fase 0 (documentado em docs/fase-00-comportamento-referencia.md)
        Missao m = missao(5);
        m.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.DIREITA);
        assertEquals(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO, Tipo.COLISAO), tipos(p.executar(Comando.EMBARCAR)));
        assertEquals(1, m.getNave().getVidas());
    }

    @Test
    void semVidasAPartidaTerminaEmDerrota() {
        Missao m = missao(5);
        m.adicionarInimigo(new Inimigo(Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.EMBARCAR);
        p.executar(Comando.EMBARCAR);
        assertEquals(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO, Tipo.NAVE_DESTRUIDA), tipos(p.executar(Comando.EMBARCAR)));
        assertEquals(Partida.Estado.DERROTA, p.getEstado());
        assertThrows(IllegalStateException.class, () -> p.executar(Comando.CIMA));
    }

    @Test
    void pontosZeradosEncerramAPartida() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        for (int i = 0; i < 19; i++) {
            p.executar(i % 2 == 0 ? Comando.CIMA : Comando.BAIXO);
        }
        assertTrue(p.emAndamento(), "1 ponto ainda é jogo");
        assertEquals(List.of(Tipo.PONTOS_ESGOTADOS), tipos(p.executar(Comando.CIMA)));
        assertEquals(Partida.Estado.DERROTA, p.getEstado());
    }

    @Test
    void naveDestruidaEPontosZeradosNoMesmoTurnoMostramAsDuasMensagens() {
        Missao m = missao(5);
        m.adicionarInimigo(new Inimigo(new Posicao(1, -1))); // 19 movimentos alternados terminam em (0,-1)
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        m.getNave().perderVida();
        m.getNave().perderVida();
        for (int i = 0; i < 19; i++) {
            p.executar(i % 2 == 0 ? Comando.CIMA : Comando.BAIXO);
        }
        assertEquals(List.of(Tipo.NAVE_DESTRUIDA, Tipo.PONTOS_ESGOTADOS), tipos(p.executar(Comando.DIREITA)));
    }

    @Test
    void vitoriaSoComTodosEmbarcadosENaPlataforma() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Engenheiro("Eng. Rosa", new Posicao(1, 0)));
        Partida p = partida(m, Dificuldade.FACIL);
        p.executar(Comando.DIREITA);
        assertEquals(List.of(new Evento(Tipo.PASSAGEIRO_EMBARCADO, "Eng. Rosa", 15), Evento.de(Tipo.RETORNE_A_PLATAFORMA)),
                p.executar(Comando.EMBARCAR));
        relogio.millis = 12_999;
        assertEquals(List.of(Evento.de(Tipo.VITORIA)), p.executar(Comando.ESQUERDA));
        assertEquals(Partida.Estado.VITORIA, p.getEstado());
        assertEquals(new ResultadoPartida("Teste", Dificuldade.FACIL, 43, 2, 12, 1), p.getResultado());
    }

    @Test
    void resultadoSoExisteDepoisDaVitoria() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        assertThrows(IllegalStateException.class, p::getResultado);
    }

    @Test
    void abortarEncerraSemGastarTurno() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        assertEquals(List.of(Tipo.ABORTADA), tipos(p.executar(Comando.ABORTAR)));
        assertEquals(Partida.Estado.ABORTADA, p.getEstado());
        assertFalse(p.emAndamento(), "terminou");
        assertEquals(0, passosDosInimigos);
    }
}
