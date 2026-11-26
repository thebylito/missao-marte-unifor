package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.online.*;
import br.edu.unifor.missaomarte.pilotagem.horda.*;
import com.badlogic.gdx.Input.Keys;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuERankingDaHordaTeste {

    private static final class Servidor implements ServidorDoJogo {
        int consultas;
        Resposta<HordaAtual> horda;
        Resposta<List<PosicaoGlobal>> classificacao;
        Resposta<RankingSemanal> semana;

        public void hordaAtual(Resposta<HordaAtual> resposta) { consultas++; horda = resposta; }
        public void rankingDaHorda(Long numero, Resposta<List<PosicaoGlobal>> resposta) {
            assertNull(numero, "o servidor escolhe a horda que está valendo");
            classificacao = resposta;
        }
        public void rankingSemanal(Resposta<RankingSemanal> resposta) { semana = resposta; }
        public void entrar(String n, String s, Resposta<Sessao> r) { fail(); }
        public void cadastrar(String n, String s, Resposta<Void> r) { fail(); }
        public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { fail(); }
        public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { fail(); }
        public void ranking(Modo modo, Resposta<List<PosicaoGlobal>> r) { fail(); }
    }

    private final Servidor servidor = new Servidor();
    private long agora;
    private boolean temSessao;

    private ControladorDoMenu menu(boolean conectado) {
        temSessao = conectado;
        return new ControladorDoMenu(null, servidor, () -> temSessao, () -> agora);
    }

    private static HordaAtual horda(long numero, long segundos) {
        var caverna = new DescricaoDeCaverna("marciana", "Caverna Marciana", 3840, 1440,
                List.of(), null, List.of(), List.of(), true);
        var descricao = new DescricaoDaHorda(numero, Dificuldade.DIFICIL, caverna, List.of(), 1, List.of(),
                List.of(new Modificador(TipoDeModificador.VENTO, -12.5)), List.of());
        return new HordaAtual(descricao, segundos, segundos, "20:00");
    }

    @Test void conectadoRecebeFaixaComOsDadosPublicadosEAguardaUmUnicoBilhete() {
        var menu = menu(true);
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        servidor.horda.sucesso(horda(42, 70));
        String faixa = String.join(" ", menu.faixaDaHorda());
        for (String esperado : List.of("42", "Caverna Marciana", "Difícil", "Vento", "esquerda", "20:00")) {
            assertTrue(faixa.contains(esperado), faixa);
        }
        assertEquals("HORDA", menu.teclaPressionada(Keys.H).name());
        assertEquals(42, menu.hordaAtual().numero());
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        menu.falhaAoIniciarHorda("Bilhete recusado");
        assertEquals("Bilhete recusado", menu.getMensagem());
        assertEquals("HORDA", menu.teclaPressionada(Keys.H).name());
    }

    @Test void convidadoVeAHordaEOAvisoSemIniciarPartida() {
        var menu = menu(false);
        servidor.horda.sucesso(horda(42, 100));
        assertTrue(String.join(" ", menu.faixaDaHorda()).contains("Caverna Marciana"));
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        assertTrue(menu.getMensagem().contains("uma conta"));
    }

    @Test void perderSessaoComMenuAbertoImpedeSolicitarBilhete() {
        var menu = menu(true);
        servidor.horda.sucesso(horda(42, 4320));
        assertTrue(String.join(" ", menu.faixaDaHorda()).contains("Termina em 1h 12min"));
        temSessao = false;
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        assertTrue(menu.getMensagem().contains("uma conta"));
    }

    @Test void quedaDaConexaoMantemOMenuUtilEPermiteRecuperarAConsulta() {
        var menu = menu(true);
        servidor.horda.falha(Falha.semConexao());
        assertTrue(String.join(" ", menu.faixaDaHorda()).contains("indisponível"));
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        assertEquals("NOVA_PARTIDA", menu.teclaPressionada(Keys.NUM_1).name());
        agora = 60_000;
        menu.atualizarHorda();
        assertEquals(2, servidor.consultas);
        servidor.horda.sucesso(horda(43, 100));
        assertEquals("HORDA", menu.teclaPressionada(Keys.H).name());
    }

    @Test void contaAPartirDaRespostaEConsultaAoZerarSemRepetirPedidoPendente() {
        var menu = menu(true);
        agora = 7_000;
        servidor.horda.sucesso(horda(42, 70));
        agora += 4_000;
        menu.atualizarHorda();
        assertTrue(String.join(" ", menu.faixaDaHorda()).contains("1min 6s"));
        agora += 56_000;
        menu.atualizarHorda();
        assertEquals(2, servidor.consultas);
        servidor.horda.sucesso(horda(42, 10));
        agora += 10_000;
        menu.atualizarHorda();
        menu.atualizarHorda();
        assertEquals(3, servidor.consultas);
        assertEquals("NENHUMA", menu.teclaPressionada(Keys.H).name());
        servidor.horda.sucesso(horda(43, 7200));
        assertEquals("HORDA", menu.teclaPressionada(Keys.H).name());
        assertEquals(43, menu.hordaAtual().numero());
    }

    @Test void rankingPercorreOsTresModosEConsultaAsDuasClassificacoesPublicas() {
        var ranking = new ControladorDoRanking(Ranking.vazio(), servidor);
        ranking.teclaPressionada(Keys.DOWN);
        ranking.teclaPressionada(Keys.DOWN);
        assertEquals("ESTA_HORDA", ranking.getAba().name());
        servidor.classificacao.sucesso(List.of(new PosicaoGlobal(1, "Ana", 75, "DIFICIL", 3, 120, "hoje")));
        assertTrue(ranking.linhas().get(0).contains("Ana - 75 pts"));
        ranking.teclaPressionada(Keys.RIGHT);
        assertEquals("SEMANA", ranking.getAba().name());
        servidor.semana.sucesso(new RankingSemanal("2026-11-23",
                List.of(new RankingSemanal.Posicao(1, "Bia", 42, 5, 3))));
        String linhas = String.join(" ", ranking.linhas());
        for (String esperado : List.of("Bia", "42 pts", "5", "3")) assertTrue(linhas.contains(esperado), linhas);
        ranking.teclaPressionada(Keys.DOWN);
        assertEquals(Modo.CLASSICO, ranking.getModo());
        assertEquals("LOCAL", ranking.getAba().name());
    }

    @Test void falhaNoPlacarDaSemanaNaoImpedeVoltarAoTopDaHorda() {
        var ranking = new ControladorDoRanking(Ranking.vazio(), servidor);
        ranking.teclaPressionada(Keys.H);
        assertEquals("ESTA_HORDA", ranking.getAba().name());
        servidor.classificacao.sucesso(List.of());
        ranking.teclaPressionada(Keys.TAB);
        servidor.semana.falha(Falha.semConexao());
        assertTrue(ranking.linhas().get(0).contains("indisponível"));
        ranking.teclaPressionada(Keys.TAB);
        assertTrue(ranking.linhas().get(0).contains("Nenhuma"));
    }
}
