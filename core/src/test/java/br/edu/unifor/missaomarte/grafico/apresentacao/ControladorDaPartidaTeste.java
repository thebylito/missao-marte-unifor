package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.MovimentoInimigo;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import com.badlogic.gdx.Input.Keys;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Partidas inteiras dirigidas por códigos de tecla, como a janela faz, mas sem janela: o controlador
 * (Humble Object) concentra as decisões da tela de jogo.
 */
class ControladorDaPartidaTeste {

    private final Apoio.RepositorioEmMemoria repositorio = new Apoio.RepositorioEmMemoria();
    private final Apoio.RelogioParado relogio = new Apoio.RelogioParado();
    private final ServicoRanking ranking = new ServicoRanking(repositorio, relogio);

    /** Mapa de -2 a 2, inimigos parados e um professor logo à direita da plataforma. */
    private static Missao missaoComProfessorAoLado() {
        Missao m = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);
        m.adicionarPassageiro(new Professor("Dr. Silva", new Posicao(1, 0)));
        return m;
    }

    private ControladorDaPartida controlador(Missao m) {
        return new ControladorDaPartida(new Partida("Ana", Dificuldade.MEDIO, m, relogio), ranking);
    }

    private static List<Evento.Tipo> tipos(List<Evento> eventos) {
        return eventos.stream().map(Evento::tipo).collect(Collectors.toList());
    }

    @Test
    void vitoriaPeloTecladoGravaORankingEAvisaOFim() {
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());

        assertTrue(c.teclaPressionada(Keys.D));
        assertTrue(c.teclaPressionada(Keys.C));
        assertEquals(List.of(Evento.Tipo.PASSAGEIRO_EMBARCADO, Evento.Tipo.RETORNE_A_PLATAFORMA),
                tipos(c.getUltimosEventos()));
        assertTrue(c.getFim().isEmpty());

        relogio.millis = 42_000;
        assertTrue(c.teclaPressionada(Keys.LEFT));

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.VITORIA, fim.estado());
        assertEquals(List.of(Evento.Tipo.VITORIA), tipos(fim.eventosFinais()));
        assertEquals(28, fim.resultado().orElseThrow().pontos());   // 20 - 2 movimentos + 10 do professor
        assertTrue(fim.entrouNoTop());
        assertTrue(fim.erroAoSalvar().isEmpty());
        assertTrue(fim.liderAnterior().isEmpty());
        assertEquals(List.of(new RegistroRanking("Ana", 28, Dificuldade.MEDIO, 1, "2026-11-17 10:15:30", 42)),
                repositorio.salvos);
    }

    @Test
    void depoisDoFimAsTeclasSaoIgnoradas() {
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());
        c.teclaPressionada(Keys.Q);
        assertFalse(c.teclaPressionada(Keys.D));
        assertEquals(Posicao.ORIGEM, c.getPartida().getMissao().getNave().getPosicao());
    }

    @Test
    void teclaQueOJogoNaoUsaNaoGastaTurno() {
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());
        assertFalse(c.teclaPressionada(Keys.X));
        assertEquals(0, c.getPartida().getMovimentos());
        assertEquals(20, c.getPartida().getPontos());
        assertTrue(c.getUltimosEventos().isEmpty());
    }

    @Test
    void abandonarComQEncerraSemGravar() {
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());
        assertTrue(c.teclaPressionada(Keys.Q));
        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.ABORTADA, fim.estado());
        assertTrue(fim.resultado().isEmpty());
        assertFalse(fim.entrouNoTop());
        assertTrue(repositorio.salvos.isEmpty());
    }

    @Test
    void tresColisoesDestroemANaveEOFimEhGameOver() {
        Missao m = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);
        m.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        m.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        ControladorDaPartida c = controlador(m);

        c.teclaPressionada(Keys.D);
        assertEquals(List.of(new Evento(Evento.Tipo.COLISAO, null, 2)), c.getUltimosEventos());
        c.teclaPressionada(Keys.A);
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.A);
        c.teclaPressionada(Keys.D);

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.DERROTA, fim.estado());
        assertEquals(List.of(Evento.Tipo.NAVE_DESTRUIDA), tipos(fim.eventosFinais()));
        assertEquals("GAME OVER!", TextosDaInterface.tituloDoFim(fim));
        assertTrue(repositorio.salvos.isEmpty());
    }

    @Test
    void oRecordeEhLidoAntesDaPartida() {
        RegistroRanking bia = new RegistroRanking("Bia", 100, Dificuldade.DIFICIL, 5, "2026-11-01 09:00:00", 60);
        repositorio.salvos.add(bia);
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.C);
        c.teclaPressionada(Keys.A);

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(bia, fim.liderAnterior().orElseThrow());
        assertTrue(fim.entrouNoTop());
        assertTrue(TextosDaInterface.linhasDoFim(fim)
                .contains(" - Recorde atual a ser batido: 100 pontos (Piloto: Bia)"));
    }

    @Test
    void erroAoGravarORankingApareceNoFim() {
        repositorio.falharAoSalvar = true;
        ControladorDaPartida c = controlador(missaoComProfessorAoLado());
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.C);
        c.teclaPressionada(Keys.A);

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals("disco cheio", fim.erroAoSalvar().orElseThrow());
        assertTrue(TextosDaInterface.linhasDoFim(fim).contains("Não foi possível salvar o ranking: disco cheio"));
    }

    // ---- partida sorteada com semente fixa, como o console faz com -Dmissaomarte.semente ----

    @Test
    void comAMesmaSementeAJanelaMontaAMesmaMissaoDoConsole() {
        ServicosDoJogo servicos = new ServicosDoJogo(new GeradorDeMissao(new Random(7)), ranking, relogio);
        Missao daJanela = servicos.novaPartida("Ana", Dificuldade.DIFICIL, 5).getMissao();
        // o console faz: gerador.gerar(new Mapa(tamanho), dificuldade)
        Missao doConsole = new GeradorDeMissao(new Random(7)).gerar(new Mapa(5), Dificuldade.DIFICIL);

        assertEquals(QuadroDaPartida.de(doConsole), QuadroDaPartida.de(daJanela));
    }

    @Test
    void partidaSorteadaComSementeFixaJogadaAteAVitoria() {
        ServicosDoJogo servicos = new ServicosDoJogo(new GeradorDeMissao(new Random(3)), ranking, relogio);
        ControladorDaPartida c = new ControladorDaPartida(
                servicos.novaPartida("Piloto Automático", Dificuldade.FACIL, 3), ranking);

        int teclas = 0;
        while (c.getFim().isEmpty() && teclas++ < 200) {
            c.teclaPressionada(proximaTecla(c.getPartida().getMissao()));
        }

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.VITORIA, fim.estado(), () -> TextosDaInterface.linhasDoFim(fim).toString());
        assertEquals(4, fim.resultado().orElseThrow().passageirosResgatados());
        assertEquals(1, repositorio.salvos.size());
    }

    /**
     * Piloto simples: vai até o passageiro mais perto (ou à plataforma, no fim), anda primeiro no eixo x e
     * desvia de asteroides tentando o outro eixo. Embarca ao chegar.
     */
    private static int proximaTecla(Missao m) {
        Posicao nave = m.getNave().getPosicao();
        if (m.passageiroNaPosicaoDaNave().isPresent()) {
            return Keys.C;
        }
        Posicao alvo = m.getPassageiros().stream()
                .map(Passageiro::getPosicao)
                .min(Comparator.comparingInt(p -> Math.abs(p.x() - nave.x()) + Math.abs(p.y() - nave.y())))
                .orElse(m.getMapa().getPlataforma());
        int dx = Integer.signum(alvo.x() - nave.x());
        int dy = Integer.signum(alvo.y() - nave.y());
        int teclaX = dx > 0 ? Keys.D : Keys.A;
        int teclaY = dy > 0 ? Keys.S : Keys.W;   // no domínio, "w" diminui o y
        boolean xLivre = dx != 0 && !temAsteroide(m, nave.deslocar(dx, 0));
        boolean yLivre = dy != 0 && !temAsteroide(m, nave.deslocar(0, dy));
        if (xLivre) {
            return teclaX;
        }
        if (yLivre) {
            return teclaY;
        }
        // bloqueado: contorna pelo eixo que não está em uso
        return dx != 0 ? (nave.y() < m.getMapa().getMax() ? Keys.S : Keys.W)
                : (nave.x() < m.getMapa().getMax() ? Keys.D : Keys.A);
    }

    private static boolean temAsteroide(Missao m, Posicao p) {
        return m.getAsteroides().stream().anyMatch(a -> a.getPosicao().equals(p));
    }
}
