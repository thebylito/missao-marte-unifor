package br.edu.unifor.missaomarte.pilotagem;

import static br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem.PASSO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import br.edu.unifor.missaomarte.pilotagem.horda.PerigoDoRoteiro;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.List;
import org.junit.jupiter.api.Test;

class EfeitosDePilotagemTeste {

    private static final ConfigDeVoo MEDIO = ConfigDeVoo.medio();

    @Test
    void ventoConstanteDeslocaANaveEmUmSegundoEPodeSerDesligado() {
        PartidaDePilotagem partida = partida(config(0, 0));
        posicionarEmVoo(partida, 1000, 700, 0);
        partida.definirVentoConstante(24);
        avancar(partida, 120);
        assertEquals(1012.1, partida.getNave().getX(), 1e-9);
        assertEquals(24, partida.getNave().getVelocidadeX(), 1e-9);
        partida.definirVentoConstante(0);
        avancar(partida, 120);
        assertEquals(1036.1, partida.getNave().getX(), 1e-9);
        assertEquals(0, partida.getPontos());
    }

    @Test
    void roteiroOrdenaOsInstantesERegistraOsPassosESementes() {
        PartidaDePilotagem partida = partida(MEDIO);
        partida.definirRoteiro(List.of(
                new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 20.001, 8),
                new PerigoDoRoteiro(TipoDeEvento.PANE_DE_GRAVIDADE, 20.0, 7)));
        avancar(partida, 2399);
        assertTrue(partida.eventosAplicados().isEmpty());

        avancar(partida, 1);
        EventoAplicado primeiro = new EventoAplicado(TipoDeEvento.PANE_DE_GRAVIDADE, 7, 2400);
        assertEquals(List.of(primeiro), partida.eventosAplicados());
        assertEquals(List.of(Tipo.PERIGO_INICIOU), tipos(partida));

        avancar(partida, 1);
        assertEquals(List.of(primeiro, new EventoAplicado(TipoDeEvento.TEMPESTADE, 8, 2401)),
                partida.eventosAplicados());
        assertEquals(List.of(Tipo.PERIGO_INICIOU), tipos(partida));
    }

    @Test
    void eventoAoVivoEntraNoProximoPasso() {
        PartidaDePilotagem partida = partida(MEDIO);
        avancar(partida, 17);
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 42);
        assertTrue(partida.eventosAplicados().isEmpty());
        assertTrue(partida.consumirEventos().isEmpty());

        partida.atualizar(2 * PASSO, Comandos.NENHUM);
        assertEquals(List.of(new EventoAplicado(TipoDeEvento.PANE_DE_GRAVIDADE, 42, 18)),
                partida.eventosAplicados());
        assertEquals(List.of(Tipo.PERIGO_INICIOU), tipos(partida));
    }

    @Test
    void tempestadeIntegraCincoCiclosDeMediaZeroComNevoaEntreZeroEUm() {
        PartidaDePilotagem partida = partida(config(0, 0));
        posicionarEmVoo(partida, 1000, 700, 0);
        long semente = 19;
        double fase = new java.util.SplittableRandom(semente).nextDouble(2 * Math.PI);
        partida.iniciarEvento(TipoDeEvento.TEMPESTADE, semente);
        assertEquals(0, partida.intensidadeDaTempestade());

        double picoDaNevoa = 0;
        double nevoaAnterior = 0;
        for (int i = 0; i < 3000; i++) {
            double velocidadeAntes = partida.getNave().getVelocidadeX();
            avancar(partida, 1);
            double aceleracao = (partida.getNave().getVelocidadeX() - velocidadeAntes) / PASSO;
            assertEquals(30 * Math.sin(2 * Math.PI * (i * PASSO) / 5 + fase), aceleracao, 1e-9);
            double nevoa = partida.intensidadeDaTempestade();
            assertTrue(nevoa >= 0 && nevoa <= 1);
            assertTrue(Math.abs(nevoa - nevoaAnterior) < 0.01);
            nevoaAnterior = nevoa;
            picoDaNevoa = Math.max(picoDaNevoa, nevoa);
        }
        assertTrue(picoDaNevoa > 0.5);
        assertEquals(0, partida.getNave().getVelocidadeX(), 1e-9);
        avancar(partida, 1);
        assertEquals(0, partida.intensidadeDaTempestade());
        assertEquals(0, partida.getNave().getVelocidadeX(), 1e-9);
        assertEquals(0, partida.getPontos());
    }

    @Test
    void paneMultiplicaGravidadePorUmVirgulaTres() {
        assertEquals(55 * 1.3, gravidadeComPane(55), 1e-9);
    }

    @Test
    void modificadorDaHordaComPaneRespeitaOTeto() {
        assertEquals(55 * 1.5, gravidadeComPane(55 * 1.2), 1e-9);
    }

    @Test
    void perigoTerminaComValorUmSeANaveSobreviveu() {
        PartidaDePilotagem partida = partida(MEDIO);
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 2);
        avancar(partida, 2400);
        assertEquals(List.of(Tipo.PERIGO_INICIOU), tipos(partida));
        avancar(partida, 1);
        assertEquals(1, valorDoFim(partida));
        assertEquals(0, partida.getPontos());
    }

    @Test
    void perigoTerminaComValorZeroMesmoSeANaveJaRenasceu() {
        PartidaDePilotagem partida = partida(MEDIO);
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 2);
        avancar(partida, 1);
        posicionarEmVoo(partida, -100, 700, 0);
        avancar(partida, 1);
        assertEquals(List.of(Tipo.PERIGO_INICIOU, Tipo.EXPLODIU), tipos(partida));

        avancar(partida, 2399);
        assertEquals(NaveEmVoo.Estado.POUSADA, partida.getNave().getEstado());
        assertEquals(0, valorDoFim(partida));
        assertEquals(0, partida.getPontos());
    }

    private static PartidaDePilotagem partida(ConfigDeVoo config) {
        return new PartidaDePilotagem("Ana", Dificuldade.MEDIO, new Caverna(3000, 1500).base(100, 70), config);
    }

    private static ConfigDeVoo config(double gravidade, double arrasto) {
        return new ConfigDeVoo(gravidade, MEDIO.empuxo(), MEDIO.giroGrausPorSegundo(), arrasto,
                MEDIO.tanque(), MEDIO.consumoPorSegundo(), MEDIO.reabastecimentoPorSegundo(), MEDIO.quedaMaxima(),
                MEDIO.deslizeMaximo(), MEDIO.inclinacaoMaxima(), MEDIO.raioDoCasco(), MEDIO.peX(), MEDIO.peY(),
                MEDIO.tempoDeEmbarque(), MEDIO.capacidade(), MEDIO.vidas(), MEDIO.tempoParaRenascer(),
                MEDIO.velocidadeDosAsteroides(), MEDIO.bonusMaximo());
    }

    private static void posicionarEmVoo(PartidaDePilotagem partida, double x, double y, double velocidadeY) {
        NaveEmVoo nave = partida.getNave();
        nave.estado = NaveEmVoo.Estado.VOANDO;
        nave.plataforma = null;
        nave.x = x;
        nave.y = y;
        nave.velocidadeY = velocidadeY;
    }

    private static void avancar(PartidaDePilotagem partida, int passos) {
        for (int i = 0; i < passos; i++) {
            partida.avancarUmPasso(Comandos.NENHUM);
        }
    }

    private static List<Tipo> tipos(PartidaDePilotagem partida) {
        return partida.consumirEventos().stream().map(EventoDeVoo::tipo).toList();
    }

    private static double gravidadeComPane(double gravidade) {
        PartidaDePilotagem partida = partida(config(gravidade, 0));
        posicionarEmVoo(partida, 1000, 700, 0);
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 1);
        avancar(partida, 1);
        return -partida.getNave().getVelocidadeY() / PASSO;
    }

    private static int valorDoFim(PartidaDePilotagem partida) {
        return partida.consumirEventos().stream().filter(e -> e.tipo() == Tipo.PERIGO_TERMINOU)
                .findFirst().orElseThrow().valor();
    }
}
