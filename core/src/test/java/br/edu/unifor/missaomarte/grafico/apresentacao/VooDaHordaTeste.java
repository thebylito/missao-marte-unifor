package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.grafico.online.*;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.EventoAplicado;
import br.edu.unifor.missaomarte.pilotagem.horda.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class VooDaHordaTeste {
    private static final Sessao PILOTO = new Sessao("Iara", "token");
    private final Servidor servidor = new Servidor();
    private ControladorDaPilotagem controle;
    private Falha erro;

    private void pedir() {
        var caverna = new DescricaoDeCaverna("ensaio", "Ensaio", 2200, 1000, List.of(),
                new PontoDePouso(100, 120), List.of(new PontoDePouso(500, 120),
                new PontoDePouso(900, 120), new PontoDePouso(1600, 120)), List.of(), true);
        var horda = new DescricaoDaHorda(53, Dificuldade.DIFICIL, caverna, List.of(
                new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 2), new PassageiroNaHorda(TipoPassageiro.PROFESSOR, 0),
                new PassageiroNaHorda(TipoPassageiro.ENGENHEIRO, 1)), 1, List.of(),
                List.of(new Modificador(TipoDeModificador.VENTO, 16)),
                List.of(new PerigoDoRoteiro(TipoDeEvento.PANE_DE_GRAVIDADE, 0.025, 971)));
        InicioDaPartida.pedirHorda(PILOTO, servidor, horda, Resposta.de(c -> controle = c, f -> erro = f));
    }

    @Test void aguardaOBilheteEFicaNoMenuSeOServidorRecusar() {
        pedir();
        assertNull(controle);
        servidor.bilhete.falha(Falha.semConexao());
        assertNull(controle);
        assertEquals(Falha.Motivo.SEM_CONEXAO, erro.motivo());
    }

    @ParameterizedTest @NullAndEmptySource void respostaSemBilheteImpedeOVoo(String vazio) {
        pedir();
        servidor.bilhete.sucesso(new Bilhete(vazio, null));
        assertNull(controle);
        assertNotNull(erro);
    }

    @Test void aplicaCavernaPassageirosVentoERoteiroAntesDoPrimeiroPasso() {
        pedir();
        servidor.bilhete.sucesso(new Bilhete("bilhete-53", null));
        var p = controle.getPartida();
        assertEquals("Iara", p.getPiloto());
        assertEquals(Dificuldade.DIFICIL, p.getDificuldade());
        assertEquals(2200, p.getCaverna().getLargura());
        assertEquals(1600, p.getCaverna().getPlataformas().get(1).getX());
        for (int i = 0; i < 5; i++) p.avancarUmPasso(Comandos.MOTOR);
        assertTrue(p.getNave().getVelocidadeX() > 0);
        assertEquals(List.of(new EventoAplicado(TipoDeEvento.PANE_DE_GRAVIDADE, 971, 3)), p.eventosAplicados());
    }

    @Test void perigoDoRoteiroTemNomeEmPortuguesNoPainel() {
        pedir();
        servidor.bilhete.sucesso(new Bilhete("bilhete-53", null));
        controle.atualizar(0.05f);
        assertTrue(controle.getMensagem().contains("Pane de gravidade"), controle.getMensagem());
    }

    @Test void registroConservaHordaEBilheteEMostraColocacaoEMoedasDoServidor() {
        pedir();
        servidor.bilhete.sucesso(new Bilhete("bilhete-53", null));
        controle.abortar();
        controle.atualizar(0);
        var fim = controle.getFim().orElseThrow();
        assertFalse(fim.entrouNoTop());
        var envio = EnvioDoResultado.iniciar(Optional.of(PILOTO), servidor, fim.paraOServidor());
        servidor.registro.sucesso(new RegistroDePartida(0, 29, 3));
        assertEquals("HORDA", servidor.partida.modo());
        assertEquals(53L, servidor.partida.horda());
        assertEquals("bilhete-53", servidor.partida.bilhete());
        assertEquals(Optional.of("3º lugar na Horda"), envio.textoDaColocacao());
        assertEquals(Optional.of("+0 moedas  |  Saldo: 29"), envio.textoDasMoedas());
    }

    @Test void fimExplicaQuandoAJanelaDeRegistroDaHordaFechou() {
        pedir();
        servidor.bilhete.sucesso(new Bilhete("bilhete-53", null));
        controle.abortar();
        controle.atualizar(0);
        var envio = EnvioDoResultado.iniciar(Optional.of(PILOTO), servidor, controle.getFim().orElseThrow().paraOServidor());
        servidor.registro.falha(new Falha(Falha.Motivo.CONFLITO, "Conflito.", "HORDA_ENCERRADA"));
        assertTrue(envio.texto().contains("prazo"), envio.texto());
    }

    @Test void historicoReconheceOsPousosDaHorda() {
        var registro = new PartidaDoHistorico("VITORIA", "FACIL", 70, 3, 7, 140, "2026-11-26T21:00:00Z", "HORDA");
        String linha = TextosDaInterface.historico(List.of(registro)).get(0);
        assertTrue(linha.contains("Horda"), linha);
        assertTrue(linha.contains("7 pousos"), linha);
    }

    private static final class Servidor implements ServidorDoJogo {
        Resposta<Bilhete> bilhete;
        PartidaParaEnviar partida;
        Resposta<RegistroDePartida> registro;

        public void iniciarHorda(Sessao s, Dificuldade d, long numero, Resposta<Bilhete> r) {
            assertEquals(PILOTO, s);
            assertEquals(Dificuldade.DIFICIL, d);
            assertEquals(53, numero);
            bilhete = r;
        }
        public void registrarPartidaComMoedas(Sessao s, PartidaParaEnviar p, Resposta<RegistroDePartida> r) {
            partida = p;
            registro = r;
        }
        public void entrar(String n, String s, Resposta<Sessao> r) { fail(); }
        public void cadastrar(String n, String s, Resposta<Void> r) { fail(); }
        public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { fail(); }
        public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { fail(); }
        public void ranking(Modo m, Resposta<List<PosicaoGlobal>> r) { fail(); }
    }
}
