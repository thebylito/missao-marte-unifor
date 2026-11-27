package br.edu.unifor.missaomarte.grafico.telas;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.Plataforma;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPartida;
import br.edu.unifor.missaomarte.grafico.online.*;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import br.edu.unifor.missaomarte.pilotagem.horda.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SelecaoDaHordaTeste {

    private static final Sessao PILOTO = new Sessao("Iara", "token");
    private static final HordaAtual HORDA = new HordaAtual(new DescricaoDaHorda(42, Dificuldade.DIFICIL,
            new DescricaoDeCaverna("marciana", "Caverna Marciana", 3840, 1440, List.of(), null,
                    List.of(), List.of(), true),
            List.of(), 1, List.of(), List.of(new Modificador(TipoDeModificador.VENTO, -12.5)), List.of()),
            70, 70, "20:00");

    private final Servidor servidor = new Servidor();
    private Input entradaAnterior;
    private InputProcessor entrada;
    private MissaoMarteGame jogo;
    @TempDir Path pasta;

    @BeforeEach void preparar() {
        entradaAnterior = Gdx.input;
        Gdx.input = new MockInput() {
            @Override public void setInputProcessor(InputProcessor nova) { entrada = nova; }
        };
        var ranking = new ServicoRanking(new RankingArquivoJson(pasta.resolve("ranking.json")), Relogio.DO_SISTEMA);
        var servicos = new ServicosDoJogo(new GeradorDeMissao(new Random(1)), ranking, Relogio.DO_SISTEMA, servidor);
        var plataforma = new Plataforma(true, Plataforma.padrao().servidor());
        jogo = new MissaoMarteGame(servicos, plataforma) {
            @Override public void trocarTela(Screen nova) {
                screen = nova;
                if (nova instanceof TelaMenu || nova instanceof TelaNovaPartida) nova.show();
            }
        };
    }

    @AfterEach void restaurar() {
        Gdx.input = entradaAnterior;
    }

    @ParameterizedTest
    @CsvSource({"0, HORDA", "1, PILOTAGEM", "2, CLASSICO", "3, HORDA"})
    void conectadoPercorreOsTresModos(int trocas, String esperado) {
        abrirNovaMissao(true, true);
        for (int i = 0; i < trocas; i++) entrada.keyDown(Keys.TAB);
        entrada.keyDown(Keys.ENTER);
        assertEquals(esperado, servidor.modoPedido);
    }

    @ParameterizedTest
    @CsvSource({"false, true", "true, false", "false, false"})
    void semContaOuSemHordaPercorreOsDoisModosLocais(boolean conectado, boolean publicada) {
        abrirNovaMissao(conectado, publicada);
        entrada.keyDown(Keys.ENTER);
        assertInstanceOf(TelaPilotagem.class, jogo.getScreen());
        abrirNovaMissao(conectado, publicada);
        entrada.keyDown(Keys.TAB);
        entrada.keyDown(Keys.ENTER);
        assertInstanceOf(TelaPartida.class, jogo.getScreen());
    }

    @Test void decolarPorToquePedeBilheteDaHordaSemIniciarPilotagemComum() {
        var nova = abrirNovaMissao(true, true);
        nova.acaoNoPonto(325, 96).orElseThrow().run();
        nova.acaoNoPonto(325, 96).orElseThrow().run();
        assertEquals(1, servidor.pedidosDaHorda);
        assertEquals(0, servidor.pedidosComuns);
        assertSame(nova, jogo.getScreen());
        servidor.bilhete.falha(Falha.semConexao());
        nova.acaoNoPonto(325, 96).orElseThrow().run();
        assertEquals(2, servidor.pedidosDaHorda);
    }

    @Test void tocarNoModoTrocaDaHordaParaPilotagem() {
        var nova = abrirNovaMissao(true, true);
        nova.acaoNoPonto(635, 477).orElseThrow().run();
        entrada.keyDown(Keys.ENTER);
        assertEquals("PILOTAGEM", servidor.modoPedido);
    }

    @Test void resumoMostraOsDadosPublicadosESubstituiOsControlesDeDificuldadeEMapa() {
        var nova = abrirNovaMissao(true, true);
        String resumo = String.join(" ", nova.resumoDaHorda());
        for (String texto : List.of("Horda 42", "Caverna Marciana", "Difícil", "Vento 12,5 para a esquerda",
                "Termina em")) {
            assertTrue(resumo.contains(texto), resumo);
        }
        assertTrue(nova.acaoNoPonto(595, 334).isEmpty());
        assertTrue(nova.acaoNoPonto(595, 264).isEmpty());
        for (var alvo : nova.alvosDeToque()) {
            assertTrue(alvo.area().largura() >= LayoutDaPartida.ALVO_MINIMO);
            assertTrue(alvo.area().altura() >= LayoutDaPartida.ALVO_MINIMO);
        }
        entrada.keyDown(Keys.TAB);
        assertTrue(nova.resumoDaHorda().isEmpty());
        assertTrue(nova.acaoNoPonto(595, 334).isPresent());
        entrada.keyDown(Keys.TAB);
        assertTrue(nova.acaoNoPonto(595, 264).isPresent());
        entrada.keyDown(Keys.TAB);
        assertTrue(nova.acaoNoPonto(595, 264).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void respostaQueChegaNaNovaMissaoRespeitaAEscolhaJaFeita(boolean trocouModo) {
        servidor.adiarConsulta = true;
        var nova = abrirNovaMissao(true, true);
        if (trocouModo) entrada.keyDown(Keys.TAB);
        servidor.consulta.sucesso(HORDA);
        nova.atualizar(0);
        entrada.keyDown(Keys.ENTER);
        assertEquals(trocouModo ? "CLASSICO" : "HORDA", servidor.modoPedido);
        assertEquals(1, servidor.consultas);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void faixaAbreAHordaPelaTeclaOuPorToqueNaBorda(boolean toque) {
        servidor.publicada = true;
        jogo.entrarComo(PILOTO);
        var menu = assertInstanceOf(TelaMenu.class, jogo.getScreen());
        if (toque) menu.acaoNoPonto(45, 12).orElseThrow().run();
        else entrada.keyDown(Keys.H);
        assertEquals(1, servidor.pedidosDaHorda);
        assertSame(menu, jogo.getScreen());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void encontroLocalAbrePorTeclaOuToqueSemBilhete(boolean toque) {
        jogo.jogarComoConvidado();
        var menu = assertInstanceOf(TelaMenu.class, jogo.getScreen());
        if (toque) menu.acaoNoPonto(80, 200).orElseThrow().run();
        else entrada.keyDown(Keys.NUM_8);
        assertInstanceOf(br.edu.unifor.missaomarte.grafico.historia.TelaEncontro.class, jogo.getScreen());
        assertEquals(0, servidor.pedidosDaHorda);
        assertEquals(0, servidor.pedidosComuns);
    }

    @Test void feedbackDoMenuFicaForaDoBotaoHistoria() {
        jogo.jogarComoConvidado();
        var menu=assertInstanceOf(TelaMenu.class,jogo.getScreen());
        var area=menu.areaDaMensagem();
        assertTrue(area.x()>=240,"feedback não pode ocupar a coluna dos botões de história");
    }

    private TelaNovaPartida abrirNovaMissao(boolean conectado, boolean publicada) {
        servidor.publicada = publicada;
        if (conectado) jogo.entrarComo(PILOTO);
        else jogo.jogarComoConvidado();
        entrada.keyDown(Keys.NUM_1);
        return assertInstanceOf(TelaNovaPartida.class, jogo.getScreen());
    }

    private static final class Servidor implements ServidorDoJogo {
        boolean publicada;
        boolean adiarConsulta;
        int consultas;
        Resposta<HordaAtual> consulta;
        String modoPedido;
        int pedidosDaHorda;
        int pedidosComuns;
        Resposta<Bilhete> bilhete;

        @Override public void hordaAtual(Resposta<HordaAtual> resposta) {
            consultas++;
            if (adiarConsulta) consulta = resposta;
            else if (publicada) resposta.sucesso(HORDA);
            else resposta.falha(Falha.semConexao());
        }

        @Override public void iniciarHorda(Sessao s, Dificuldade d, long numero, Resposta<Bilhete> resposta) {
            assertEquals(PILOTO, s);
            assertEquals(Dificuldade.DIFICIL, d);
            assertEquals(42, numero);
            modoPedido = "HORDA";
            pedidosDaHorda++;
            bilhete = resposta;
        }

        @Override public void iniciarPartida(Sessao s, Modo modo, Dificuldade d, Resposta<Bilhete> resposta) {
            modoPedido = modo.name();
            pedidosComuns++;
        }

        @Override public void entrar(String n, String s, Resposta<Sessao> r) { fail(); }
        @Override public void cadastrar(String n, String s, Resposta<Void> r) { fail(); }
        @Override public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { fail(); }
        @Override public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { fail(); }
        @Override public void ranking(Modo m, Resposta<List<PosicaoGlobal>> r) { fail(); }
    }
}
