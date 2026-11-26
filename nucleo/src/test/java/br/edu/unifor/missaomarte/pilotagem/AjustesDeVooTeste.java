package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Fase 8 (DA-48): os números que o servidor pode ajustar por variável de ambiente. */
class AjustesDeVooTeste {

    private static final AjustesDaDificuldade MEDIO = AjustesDeVoo.PADRAO.medio();
    private static final AjustesDosEventos EVENTOS = AjustesDeVoo.PADRAO.eventos();

    @Test
    void oPadraoRepeteOsNumerosDaFase8() {
        assertEquals(new AjustesDaDificuldade(130, 90, 60, 25, 0.75), AjustesDeVoo.PADRAO.facil());
        assertEquals(new AjustesDaDificuldade(100, 75, 50, 20, 1.0), AjustesDeVoo.PADRAO.medio());
        assertEquals(new AjustesDaDificuldade(100, 65, 40, 15, 1.3), AjustesDeVoo.PADRAO.dificil());
        assertEquals(new AjustesDosEventos(150, 250, 20, 25, 20, 45), AjustesDeVoo.PADRAO.eventos());
    }

    @Test
    void comOPadraoAConfiguracaoEAMesmaDeAntes() {
        for (Dificuldade d : Dificuldade.values()) {
            assertEquals(ConfigDeVoo.para(d), ConfigDeVoo.para(d, AjustesDeVoo.PADRAO), d.name());
        }
        assertEquals(AjustesDosEventos.PADRAO, ConfigDeVoo.medio().eventos());
        assertEquals(ConfigDeVoo.medio(), ConfigDeVoo.para(Dificuldade.MEDIO));
    }

    @Test
    void aDuracaoPadraoDeCadaEventoEADoTipo() {
        for (TipoDeEvento tipo : TipoDeEvento.values()) {
            assertEquals(tipo.duracaoSegundos(), EVENTOS.duracao(tipo), tipo.name());
        }
    }

    @Test
    void osAjustesDaDificuldadeEDosEventosEntramNaConfiguracao() {
        AjustesDosEventos eventos = new AjustesDosEventos(300, 400, 30, 15, 12, 60);
        AjustesDeVoo ajustes = new AjustesDeVoo(AjustesDeVoo.PADRAO.facil(),
                new AjustesDaDificuldade(140, 80, 55, 30, 1.2), AjustesDeVoo.PADRAO.dificil(), eventos);

        ConfigDeVoo config = ConfigDeVoo.para(Dificuldade.MEDIO, ajustes);

        ConfigDeVoo m = ConfigDeVoo.medio();
        assertEquals(new ConfigDeVoo(m.gravidade(), m.empuxo(), m.giroGrausPorSegundo(), m.arrasto(), 140,
                m.consumoPorSegundo(), m.reabastecimentoPorSegundo(), 80, 55, 30, m.raioDoCasco(), m.peX(), m.peY(),
                m.tempoDeEmbarque(), m.capacidade(), m.vidas(), m.tempoParaRenascer(), 1.2, m.bonusMaximo(),
                eventos), config);
        assertEquals(ConfigDeVoo.para(Dificuldade.FACIL).inclinacaoMaxima(),
                ConfigDeVoo.para(Dificuldade.FACIL, ajustes).inclinacaoMaxima());
    }

    @Test
    void cadaDificuldadeTemOsSeusAjustes() {
        assertEquals(AjustesDeVoo.PADRAO.facil(), AjustesDeVoo.PADRAO.para(Dificuldade.FACIL));
        assertEquals(AjustesDeVoo.PADRAO.medio(), AjustesDeVoo.PADRAO.para(Dificuldade.MEDIO));
        assertEquals(AjustesDeVoo.PADRAO.dificil(), AjustesDeVoo.PADRAO.para(Dificuldade.DIFICIL));
    }

    @Test
    void aPartidaNovaUsaOsAjustes() {
        AjustesDeVoo ajustes = new AjustesDeVoo(AjustesDeVoo.PADRAO.facil(),
                new AjustesDaDificuldade(100, 75, 50, 33, 1.0), AjustesDeVoo.PADRAO.dificil(), EVENTOS);

        PartidaDePilotagem partida = PartidaDePilotagem.nova("Ana", Dificuldade.MEDIO, ajustes);

        assertEquals(33, partida.getConfig().inclinacaoMaxima());
        assertEquals(ConfigDeVoo.para(Dificuldade.MEDIO), PartidaDePilotagem.nova("Ana", Dificuldade.MEDIO).getConfig());
    }

    @Test
    void valoresForaDaFaixaSaoRecusadosComONomeDoCampo() {
        List<Recusa> recusas = List.of(
                new Recusa("tanque", () -> new AjustesDaDificuldade(49, 75, 50, 20, 1)),
                new Recusa("tanque", () -> new AjustesDaDificuldade(301, 75, 50, 20, 1)),
                new Recusa("quedaMaxima", () -> new AjustesDaDificuldade(100, 19, 50, 20, 1)),
                new Recusa("quedaMaxima", () -> new AjustesDaDificuldade(100, 201, 50, 20, 1)),
                new Recusa("deslizeMaximo", () -> new AjustesDaDificuldade(100, 75, 9, 20, 1)),
                new Recusa("deslizeMaximo", () -> new AjustesDaDificuldade(100, 75, 151, 20, 1)),
                new Recusa("inclinacaoMaxima", () -> new AjustesDaDificuldade(100, 75, 50, 4, 1)),
                new Recusa("inclinacaoMaxima", () -> new AjustesDaDificuldade(100, 75, 50, 46, 1)),
                new Recusa("velocidadeDosAsteroides", () -> new AjustesDaDificuldade(100, 75, 50, 20, 0.2)),
                new Recusa("velocidadeDosAsteroides", () -> new AjustesDaDificuldade(100, 75, 50, 20, 3.1)),
                new Recusa("tanque", () -> new AjustesDaDificuldade(Double.NaN, 75, 50, 20, 1)),
                new Recusa("velocidadeMinimaDosMeteoros", () -> new AjustesDosEventos(49, 250, 20, 25, 20, 45)),
                new Recusa("velocidadeMaximaDosMeteoros", () -> new AjustesDosEventos(150, 601, 20, 25, 20, 45)),
                new Recusa("menor que a máxima", () -> new AjustesDosEventos(250, 250, 20, 25, 20, 45)),
                new Recusa("menor que a máxima", () -> new AjustesDosEventos(300, 200, 20, 25, 20, 45)),
                new Recusa("duracaoDaChuva", () -> new AjustesDosEventos(150, 250, 9, 25, 20, 45)),
                new Recusa("duracaoDaTempestade", () -> new AjustesDosEventos(150, 250, 20, 91, 20, 45)),
                new Recusa("duracaoDaPane", () -> new AjustesDosEventos(150, 250, 20, 25, 9.5, 45)),
                new Recusa("duracaoDoVip", () -> new AjustesDosEventos(150, 250, 20, 25, 20, Double.POSITIVE_INFINITY)));
        for (Recusa recusa : recusas) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, recusa.criar()::get,
                    recusa.campo());
            assertTrue(erro.getMessage().contains(recusa.campo()), erro.getMessage());
        }
    }

    @Test
    void osLimitesDaFaixaSaoAceitos() {
        new AjustesDaDificuldade(50, 20, 10, 5, 0.25);
        new AjustesDaDificuldade(300, 200, 150, 45, 3);
        new AjustesDosEventos(50, 600, 10, 10, 10, 10);
        new AjustesDosEventos(50, 51, 90, 90, 90, 90);
    }

    @Test
    void ajustesSemAlgumaParteSaoRecusados() {
        assertThrows(NullPointerException.class, () -> new AjustesDeVoo(null, MEDIO, MEDIO, EVENTOS));
        assertThrows(NullPointerException.class, () -> new AjustesDeVoo(MEDIO, MEDIO, MEDIO, null));
    }

    private record Recusa(String campo, Supplier<Object> criar) {}
}
