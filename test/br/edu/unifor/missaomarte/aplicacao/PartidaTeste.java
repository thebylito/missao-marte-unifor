package br.edu.unifor.missaomarte.aplicacao;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.lanca;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

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
import br.edu.unifor.missaomarte.teste.Teste;
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

    @Teste
    void comecaComOsPontosDaDificuldade() {
        for (Dificuldade d : Dificuldade.values()) {
            igual(d.getPontosIniciais(), partida(missao(5), d).getPontos(), d.name());
        }
    }

    @Teste
    void cadaMovimentoCustaUmPontoMesmoParadoNaBorda() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.DIREITA);
        p.executar(Comando.DIREITA);
        p.executar(Comando.DIREITA); // já está em x = 2: não sai do lugar, mas paga
        igual(new Posicao(2, 0), m.getNave().getPosicao());
        igual(17, p.getPontos());
        igual(3, p.getMovimentos());
        igual(3, passosDosInimigos, "inimigos andam a cada movimento");
    }

    @Teste
    void comandoInvalidoNaoGastaOTurno() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        igual(List.of(Tipo.COMANDO_INVALIDO), tipos(p.executar(Comando.INVALIDO)));
        igual(20, p.getPontos());
        igual(0, p.getMovimentos());
        igual(0, passosDosInimigos, "inimigos não andam");
    }

    @Teste
    void embarcarSomaAPontuacaoDoTipoEGastaOTurnoSemCustarPonto() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        List<Evento> eventos = p.executar(Comando.EMBARCAR);
        igual(new Evento(Tipo.PASSAGEIRO_EMBARCADO, "Ast. Maria", 20), eventos.get(0));
        igual(40, p.getPontos());
        igual(0, p.getMovimentos());
        igual(1, passosDosInimigos);
        igual(1, m.getPassageiros().size());
    }

    @Teste
    void embarcarSemPassageiroAvisaEAindaAssimOsInimigosAndam() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        igual(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO), tipos(p.executar(Comando.EMBARCAR)));
        igual(1, passosDosInimigos);
    }

    @Teste
    void naveCheiaRecusaOEmbarque() {
        Missao m = missao(1);
        m.adicionarPassageiro(new Professor("A", Posicao.ORIGEM));
        m.adicionarPassageiro(new Engenheiro("B", Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.EMBARCAR);
        igual(List.of(Tipo.NAVE_CHEIA), tipos(p.executar(Comando.EMBARCAR)));
        igual(30, p.getPontos());
    }

    @Teste
    void colisaoTiraUmaVidaEAvisaQuantasRestam() {
        Missao m = missao(5);
        m.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        igual(List.of(new Evento(Tipo.COLISAO, null, 2)), p.executar(Comando.DIREITA));
        igual(new Posicao(1, 0), m.getNave().getPosicao(), "a nave fica na célula do asteroide");
        verdadeiro(p.emAndamento(), "ainda tem vidas");
    }

    @Teste
    void ficarSobreOAsteroideTiraOutraVida() {
        // comportamento herdado da Fase 0 (documentado em docs/fase-00-comportamento-referencia.md)
        Missao m = missao(5);
        m.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.DIREITA);
        igual(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO, Tipo.COLISAO), tipos(p.executar(Comando.EMBARCAR)));
        igual(1, m.getNave().getVidas());
    }

    @Teste
    void semVidasAPartidaTerminaEmDerrota() {
        Missao m = missao(5);
        m.adicionarInimigo(new Inimigo(Posicao.ORIGEM));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        p.executar(Comando.EMBARCAR);
        p.executar(Comando.EMBARCAR);
        igual(List.of(Tipo.SEM_PASSAGEIRO_NA_POSICAO, Tipo.NAVE_DESTRUIDA), tipos(p.executar(Comando.EMBARCAR)));
        igual(Partida.Estado.DERROTA, p.getEstado());
        lanca(IllegalStateException.class, () -> p.executar(Comando.CIMA));
    }

    @Teste
    void pontosZeradosEncerramAPartida() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        for (int i = 0; i < 19; i++) {
            p.executar(i % 2 == 0 ? Comando.CIMA : Comando.BAIXO);
        }
        verdadeiro(p.emAndamento(), "1 ponto ainda é jogo");
        igual(List.of(Tipo.PONTOS_ESGOTADOS), tipos(p.executar(Comando.CIMA)));
        igual(Partida.Estado.DERROTA, p.getEstado());
    }

    @Teste
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
        igual(List.of(Tipo.NAVE_DESTRUIDA, Tipo.PONTOS_ESGOTADOS), tipos(p.executar(Comando.DIREITA)));
    }

    @Teste
    void vitoriaSoComTodosEmbarcadosENaPlataforma() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Engenheiro("Eng. Rosa", new Posicao(1, 0)));
        Partida p = partida(m, Dificuldade.FACIL);
        p.executar(Comando.DIREITA);
        igual(List.of(new Evento(Tipo.PASSAGEIRO_EMBARCADO, "Eng. Rosa", 15), Evento.de(Tipo.RETORNE_A_PLATAFORMA)),
                p.executar(Comando.EMBARCAR));
        relogio.millis = 12_999;
        igual(List.of(Evento.de(Tipo.VITORIA)), p.executar(Comando.ESQUERDA));
        igual(Partida.Estado.VITORIA, p.getEstado());
        igual(new ResultadoPartida("Teste", Dificuldade.FACIL, 43, 2, 12, 1), p.getResultado());
    }

    @Teste
    void resultadoSoExisteDepoisDaVitoria() {
        Missao m = missao(5);
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        lanca(IllegalStateException.class, p::getResultado);
    }

    @Teste
    void abortarEncerraSemGastarTurno() {
        Missao m = comInimigoNoCanto(missao(5));
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Partida p = partida(m, Dificuldade.MEDIO);
        igual(List.of(Tipo.ABORTADA), tipos(p.executar(Comando.ABORTAR)));
        igual(Partida.Estado.ABORTADA, p.getEstado());
        falso(p.emAndamento(), "terminou");
        igual(0, passosDosInimigos);
    }
}
