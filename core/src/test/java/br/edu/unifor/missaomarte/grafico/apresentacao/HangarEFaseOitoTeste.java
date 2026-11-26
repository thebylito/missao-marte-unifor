package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.grafico.online.Bilhete;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Hangar;
import br.edu.unifor.missaomarte.grafico.online.PartidaDoHistorico;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import br.edu.unifor.missaomarte.grafico.online.RegistroDePartida;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.grafico.online.SkinDaLoja;
import br.edu.unifor.missaomarte.grafico.recursos.CatalogoDeNaves;
import com.badlogic.gdx.Input.Keys;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class HangarEFaseOitoTeste {
    private static final Sessao ANA = new Sessao("Ana", "token");
    private static final String UUID = "12345678-1234-1234-1234-123456789abc";
    private static final PartidaParaEnviar VITORIA = new PartidaParaEnviar("VITORIA", "MEDIO", 100, 3, 5, 60);

    private final ServidorFalso servidor = new ServidorFalso();
    private final List<Hangar> atualizacoes = new ArrayList<>();
    private final List<String> equipadas = new ArrayList<>();

    @ParameterizedTest
    @EnumSource(Modo.class)
    void pedeBilhetePorModoEDificuldadeEEnviaAoTerminar(Modo modo) {
        InicioDaPartida inicio = InicioDaPartida.pedir(Optional.of(ANA), servidor, modo, Dificuldade.MEDIO);
        assertEquals(modo, servidor.modo);
        assertEquals(Dificuldade.MEDIO, servidor.dificuldade);
        EnvioDoResultado envio = inicio.enviar(VITORIA, registro -> { }, falha -> { });
        assertEquals(EnvioDoResultado.Estado.ENVIANDO, envio.getEstado());
        assertNull(servidor.enviada);
        servidor.inicio.sucesso(new Bilhete(UUID, Instant.parse("2026-11-26T18:00:00Z")));
        assertEquals(UUID, servidor.enviada.bilhete());
    }

    @Test
    void falhaNoBilheteAindaPermiteEnviarAmesmaPartida() {
        InicioDaPartida inicio = InicioDaPartida.pedir(Optional.of(ANA), servidor, Modo.CLASSICO, Dificuldade.MEDIO);
        servidor.inicio.falha(Falha.semConexao());
        inicio.enviar(VITORIA, registro -> { }, falha -> { });
        assertEquals(VITORIA, servidor.enviada);
        assertNull(servidor.enviada.bilhete());
    }

    @Test
    void fimMostraRecompensaESaldoVindosDoServidor() {
        InicioDaPartida inicio = InicioDaPartida.pedir(Optional.of(ANA), servidor, Modo.CLASSICO, Dificuldade.MEDIO);
        servidor.inicio.sucesso(new Bilhete(UUID, null));
        List<RegistroDePartida> recebidos = new ArrayList<>();
        EnvioDoResultado envio = inicio.enviar(VITORIA, recebidos::add, falha -> { });
        RegistroDePartida registro = new RegistroDePartida(3, 63);
        servidor.registro.sucesso(registro);
        assertEquals(Optional.of("+3 moedas  |  Saldo: 63"), envio.textoDasMoedas());
        assertEquals(List.of(registro), recebidos);
    }

    @Test
    void convidadoNaoPedeBilheteNemEnviaOuVeRecompensa() {
        InicioDaPartida inicio = InicioDaPartida.pedir(Optional.empty(), servidor, Modo.CLASSICO, Dificuldade.MEDIO);
        EnvioDoResultado envio = inicio.enviar(VITORIA, registro -> fail(), falha -> fail());
        assertNull(servidor.inicio);
        assertNull(servidor.enviada);
        assertTrue(envio.textoDasMoedas().isEmpty());
    }

    @Test
    void saldoDoMenuSoApareceComConta() {
        assertEquals(Optional.of("Moedas: 42"), ControladorDoMenu.textoDoSaldo(Optional.of(ANA), 42));
        assertTrue(ControladorDoMenu.textoDoSaldo(Optional.empty(), 42).isEmpty());
        assertEquals(Optional.of("Moedas: —"), ControladorDoMenu.textoDoSaldo(Optional.of(ANA), null));
    }

    @Test
    void menuPreservaTeclasAntigasEAbreHangarComSete() {
        ControladorDoMenu menu = new ControladorDoMenu(null);
        var esperadas = List.of(ControladorDoMenu.Acao.NOVA_PARTIDA, ControladorDoMenu.Acao.RANKING,
                ControladorDoMenu.Acao.NENHUMA, ControladorDoMenu.Acao.SAIR, ControladorDoMenu.Acao.HISTORICO,
                ControladorDoMenu.Acao.TROCAR_PILOTO, ControladorDoMenu.Acao.HANGAR);
        for (int i = 0; i < esperadas.size(); i++) {
            assertEquals(esperadas.get(i), menu.teclaPressionada(Keys.NUM_1 + i));
            if (i == 2) {
                assertTrue(menu.isConfirmandoApagar());
                menu.teclaPressionada(Keys.N);
            }
        }
        assertEquals(ControladorDoMenu.Acao.HANGAR, menu.teclaPressionada(Keys.NUMPAD_7));
    }

    @Test
    void agrupaLinhasOrdenaNiveisEFiltraArteAusente() {
        var controle = carregar();
        assertEquals(List.of("padrao", "azul", "azul-2", "verde"),
                controle.itens().stream().map(SkinDaLoja::id).toList());
    }

    @Test
    void compraNivelDoisDiretoEAtualizaSaldoEPosseSemEquipar() {
        var controle = carregar();
        controle.acionar("azul-2");
        assertEquals("azul-2", servidor.skinSolicitada);
        controle.acionar("azul");
        assertEquals("azul-2", servidor.skinSolicitada);
        servidor.compra.sucesso(new Hangar(10, List.of("padrao", "azul-2"), null));
        assertEquals(new Hangar(10, List.of("padrao", "azul-2"), "padrao"), controle.hangar());
        assertEquals(new Hangar(10, List.of("padrao", "azul-2"), null), atualizacoes.get(1));
    }

    @ParameterizedTest
    @CsvSource({"CONFLITO,SALDO_INSUFICIENTE,Moedas insuficientes",
            "CONFLITO,JA_POSSUI,Você já tem esta nave", "SEM_CONEXAO,,Sem conexão"})
    void apresentaErroDaCompraSemAlterarSaldo(Falha.Motivo motivo, String codigo, String mensagem) {
        var controle = carregar();
        controle.acionar("azul");
        servidor.compra.falha(new Falha(motivo, "detail do servidor", codigo));
        assertEquals(mensagem, controle.mensagem());
        assertEquals(80, controle.hangar().saldo());
        assertEquals(List.of("padrao"), controle.hangar().skins());
    }

    @Test
    void equipaConfirmadaPeloServidor() {
        var controle = carregar();
        controle.acionar("verde");
        servidor.compra.sucesso(new Hangar(50, List.of("padrao", "verde"), null));
        controle.acionar("verde");
        assertEquals("verde", servidor.skinSolicitada);
        assertEquals("padrao", controle.hangar().equipada());
        servidor.equipar.sucesso("verde");
        assertEquals("verde", controle.hangar().equipada());
        assertEquals(List.of("verde"), equipadas);
    }

    @Test
    void podeReequiparPadraoQuandoSkinDoServidorNaoExisteNestaInstalacao() {
        var controle = carregarComPerfil(new Hangar(80, List.of("padrao", "outra"), "outra"));
        assertEquals(ControladorDoHangar.Estado.POSSUIDA, controle.estado("padrao"));
        controle.acionar("padrao");
        assertEquals("padrao", servidor.skinSolicitada);
        assertNotNull(servidor.equipar);
    }

    @Test
    void convidadoNaoConsultaLojaNemHangar() {
        var controle = new ControladorDoHangar(servidor, Optional.empty(), CatalogoDeNaves.ler("[]"),
                atualizacoes::add, equipadas::add, falha -> fail());
        controle.carregar();
        assertNull(servidor.perfil);
        assertNull(servidor.loja);
        assertTrue(controle.itens().isEmpty());
    }

    private ControladorDoHangar carregar() {
        return carregarComPerfil(new Hangar(80, List.of("padrao"), "padrao"));
    }

    private ControladorDoHangar carregarComPerfil(Hangar perfil) {
        String json = """
                [{"id":"azul-2","nome":"Azul II","descricao":""},{"id":"verde","nome":"Verde","descricao":""},
                 {"id":"azul","nome":"Azul","descricao":""}]
                """;
        var controle = new ControladorDoHangar(servidor, Optional.of(ANA), CatalogoDeNaves.ler(json),
                atualizacoes::add, equipadas::add, falha -> fail());
        controle.carregar();
        servidor.perfil.sucesso(perfil);
        servidor.loja.sucesso(List.of(new SkinDaLoja("padrao", "Padrão", "padrao", 1, 0),
                new SkinDaLoja("azul-2", "Azul II", "Azul", 2, 70),
                new SkinDaLoja("verde", "Verde", "Verde", 1, 30),
                new SkinDaLoja("azul", "Azul", "Azul", 1, 30),
                new SkinDaLoja("outra", "Outra", "Verde", 2, 30)));
        return controle;
    }

    private static final class ServidorFalso implements ServidorDoJogo {
        Resposta<Bilhete> inicio;
        Resposta<RegistroDePartida> registro;
        Resposta<Hangar> perfil;
        Resposta<Hangar> compra;
        Resposta<String> equipar;
        Resposta<List<SkinDaLoja>> loja;
        Modo modo;
        Dificuldade dificuldade;
        String skinSolicitada;
        PartidaParaEnviar enviada;

        @Override
        public void iniciarPartida(Sessao s, Modo modo, Dificuldade dificuldade, Resposta<Bilhete> resposta) {
            assertEquals(ANA, s);
            this.modo = modo;
            this.dificuldade = dificuldade;
            inicio = resposta;
        }

        @Override
        public void registrarPartidaComMoedas(Sessao s, PartidaParaEnviar partida, Resposta<RegistroDePartida> r) {
            assertEquals(ANA, s);
            enviada = partida;
            registro = r;
        }

        @Override public void loja(Resposta<List<SkinDaLoja>> r) { loja = r; }
        @Override public void hangar(Sessao s, Resposta<Hangar> r) { perfil = r; }

        @Override
        public void comprar(Sessao s, String skin, Resposta<Hangar> r) {
            skinSolicitada = skin;
            compra = r;
        }

        @Override
        public void equipar(Sessao s, String skin, Resposta<String> r) {
            skinSolicitada = skin;
            equipar = r;
        }

        @Override public void cadastrar(String n, String s, Resposta<Void> r) { fail(); }
        @Override public void entrar(String n, String s, Resposta<Sessao> r) { fail(); }
        @Override public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { fail(); }
        @Override public void ranking(Modo m, Resposta<List<PosicaoGlobal>> r) { fail(); }
        @Override public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { fail(); }
    }
}
