package br.edu.unifor.missaomarte.pilotagem;

import static br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem.PASSO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import br.edu.unifor.missaomarte.pilotagem.horda.PerigoDoRoteiro;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
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
        double fase = new SplittableRandom(semente).nextDouble(2 * Math.PI);
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
    void chuvaRepeteTrajetoriasDaMesmaSementeETerminaEmVinteSegundos() {
        PartidaDePilotagem primeira = partida(MEDIO);
        PartidaDePilotagem segunda = partida(MEDIO);
        primeira.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);
        segunda.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);
        assertTrue(primeira.getMeteoros().isEmpty());

        Map<Double, Meteoro> vistos = new HashMap<>();
        int ultimoSurgimento = 0;
        for (int i = 0; i < 2400; i++) {
            avancar(primeira, 1);
            avancar(segunda, 1);
            List<Meteoro> meteoros = primeira.getMeteoros();
            assertEquals(meteoros, segunda.getMeteoros());
            for (Meteoro meteoro : meteoros) {
                assertTrue(meteoro.raio() >= 18 && meteoro.raio() <= 28);
                Meteoro anterior = vistos.put(meteoro.raio(), meteoro);
                if (anterior == null) {
                    ultimoSurgimento = i;
                } else {
                    double velocidade = Math.hypot(meteoro.x() - anterior.x(), meteoro.y() - anterior.y()) / PASSO;
                    assertTrue(velocidade >= 300 && velocidade <= 500);
                }
            }
        }
        assertTrue(vistos.size() >= 6 && vistos.size() <= 10);
        assertTrue(ultimoSurgimento > 1200, "há entradas também na segunda metade da chuva");
        assertTrue(tipos(primeira).stream().noneMatch(tipo -> tipo == Tipo.PERIGO_TERMINOU));
        avancar(primeira, 1);
        assertTrue(primeira.getMeteoros().isEmpty());
        assertTrue(tipos(primeira).contains(Tipo.PERIGO_TERMINOU));
        assertEquals(0, primeira.getPontos());
    }

    @Test
    void meteoroExplodeANaveParadaEmSuaRota() {
        PontoDaChuva alvo = alvoDaChuva();
        PartidaDePilotagem partida = partida(config(0, 0));
        posicionarEmVoo(partida, alvo.meteoro().x(), alvo.meteoro().y(), 0);
        partida.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);
        avancar(partida, alvo.passo());

        EventoDeVoo explosao = partida.consumirEventos().stream().filter(e -> e.tipo() == Tipo.EXPLODIU)
                .findFirst().orElseThrow();
        assertEquals("Atingida por um meteoro!", explosao.mensagem());
        assertEquals(2, partida.getVidas());
        assertEquals(0, partida.getPontos());
    }

    @Test
    void asteroideTemPrioridadeQuandoAMesmaPosicaoTambemTemMeteoro() {
        PontoDaChuva alvo = alvoDaChuva();
        double x = alvo.meteoro().x();
        double y = alvo.meteoro().y();
        Caverna caverna = new Caverna(3000, 1500).base(100, 70)
                .asteroide(new AsteroideEmOrbita(x, y, 20, 0, 0, 5, 0, 0));
        PartidaDePilotagem partida = new PartidaDePilotagem("Ana", Dificuldade.MEDIO, caverna, config(0, 0));
        partida.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);
        avancar(partida, alvo.passo() - 1);
        posicionarEmVoo(partida, x, y, 0);
        avancar(partida, 1);
        assertTrue(partida.getMeteoros().contains(alvo.meteoro()));
        assertEquals("Atingida por um asteroide!", partida.consumirEventos().stream()
                .filter(e -> e.tipo() == Tipo.EXPLODIU).findFirst().orElseThrow().mensagem());
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

    private record PontoDaChuva(Meteoro meteoro, int passo) {}

    private static PontoDaChuva alvoDaChuva() {
        PartidaDePilotagem referencia = partida(MEDIO);
        referencia.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);
        for (int i = 1; i <= 2400; i++) {
            avancar(referencia, 1);
            for (Meteoro meteoro : referencia.getMeteoros()) {
                if (meteoro.x() > 300 && meteoro.x() < 2700 && meteoro.y() > 400 && meteoro.y() < 1100) {
                    return new PontoDaChuva(meteoro, i);
                }
            }
        }
        throw new AssertionError("A chuva precisa cruzar o interior da caverna");
    }

    private static int valorDoFim(PartidaDePilotagem partida) {
        return partida.consumirEventos().stream().filter(e -> e.tipo() == Tipo.PERIGO_TERMINOU)
                .findFirst().orElseThrow().valor();
    }
}
