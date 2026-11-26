package br.edu.unifor.missaomarte.pilotagem;

import static br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem.PASSO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import br.edu.unifor.missaomarte.pilotagem.horda.PerigoDoRoteiro;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Fase 8 (DA-48): a duração dos eventos e a velocidade dos meteoros vêm dos ajustes da configuração. Um evento ao
 * vivo usa a duração que o servidor mandou na mensagem (a da horda, que pode ser outra que a dos ajustes do jogo).
 */
class AjustesNosEventosTeste {

    private static final AjustesDosEventos CURTOS = new AjustesDosEventos(300, 400, 12, 10, 15, 30);

    @Test
    void oEventoAoVivoDuraOQueOServidorMandou() {
        PartidaDePilotagem partida = partida(ConfigDeVoo.medio());
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 42, 12);

        assertTerminaNoPasso(partida, 1 + 12 * 120);
    }

    @Test
    void semDuracaoOEventoAoVivoUsaADosAjustes() {
        PartidaDePilotagem partida = partida(comEventos(CURTOS));
        partida.iniciarEvento(TipoDeEvento.PANE_DE_GRAVIDADE, 42);

        assertTerminaNoPasso(partida, 1 + 15 * 120);
    }

    @Test
    void semAjustesOEventoDuraOPadrao() {
        PartidaDePilotagem partida = partida(ConfigDeVoo.medio());
        partida.iniciarEvento(TipoDeEvento.TEMPESTADE, 42);

        assertTerminaNoPasso(partida, 1 + 25 * 120);
    }

    @Test
    void oRoteiroUsaADuracaoDosAjustes() {
        PartidaDePilotagem partida = partida(comEventos(CURTOS));
        partida.definirRoteiro(List.of(new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 20.0, 8)));

        assertTerminaNoPasso(partida, 2400 + 10 * 120);
    }

    @Test
    void aTempestadeCurtaChegaAoPicoNaMetadeDela() {
        PartidaDePilotagem partida = partida(ConfigDeVoo.medio());
        partida.iniciarEvento(TipoDeEvento.TEMPESTADE, 19, 10);
        avancar(partida, 1 + 5 * 120);

        assertEquals(1, partida.intensidadeDaTempestade(), 1e-9);
    }

    @Test
    void aChuvaDuraOQueFoiPedidoEOsMeteorosAndamNaFaixaDosAjustes() {
        PartidaDePilotagem partida = partida(comEventos(CURTOS));
        partida.iniciarEvento(TipoDeEvento.CHUVA_DE_METEOROS, 73);

        Map<Double, Meteoro> vistos = new HashMap<>();
        for (int i = 0; i < 12 * 120; i++) {
            avancar(partida, 1);
            for (Meteoro meteoro : partida.getMeteoros()) {
                Meteoro anterior = vistos.put(meteoro.raio(), meteoro);
                if (anterior != null) {
                    double velocidade = Math.hypot(meteoro.x() - anterior.x(), meteoro.y() - anterior.y()) / PASSO;
                    assertTrue(velocidade >= 300 - 1e-6 && velocidade <= 400 + 1e-6, "velocidade " + velocidade);
                }
            }
        }
        assertTrue(vistos.size() >= 6, "meteoros vistos: " + vistos.size());
        assertFalse(tipos(partida).contains(Tipo.PERIGO_TERMINOU));
        avancar(partida, 1);
        assertTrue(partida.getMeteoros().isEmpty());
        assertTrue(tipos(partida).contains(Tipo.PERIGO_TERMINOU));
    }

    @Test
    void oVipDuraOQueOServidorMandou() {
        PartidaDePilotagem partida = partida(ConfigDeVoo.medio());
        partida.iniciarVip(900, 500, 30);
        avancar(partida, 1);
        assertTrue(partida.getCapsulaVip().isPresent());

        avancar(partida, 30 * 120 - 1);
        assertTrue(partida.getCapsulaVip().isPresent());
        avancar(partida, 1);
        assertTrue(partida.getCapsulaVip().isEmpty());
    }

    @Test
    void semDuracaoOVipUsaADosAjustes() {
        PartidaDePilotagem partida = partida(comEventos(CURTOS));
        partida.iniciarVip(900, 500);
        avancar(partida, 30 * 120);
        assertTrue(partida.getCapsulaVip().isPresent());
        avancar(partida, 1);
        assertTrue(partida.getCapsulaVip().isEmpty());
    }

    @Test
    void duracaoInvalidaEErro() {
        PartidaDePilotagem partida = partida(ConfigDeVoo.medio());
        assertThrows(IllegalArgumentException.class, () -> partida.iniciarEvento(TipoDeEvento.TEMPESTADE, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> partida.iniciarEvento(TipoDeEvento.TEMPESTADE, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> partida.iniciarVip(900, 500, -1));
    }

    private static void assertTerminaNoPasso(PartidaDePilotagem partida, int passo) {
        avancar(partida, passo - 1);
        assertFalse(tipos(partida).contains(Tipo.PERIGO_TERMINOU), "terminou antes do passo " + passo);
        avancar(partida, 1);
        assertTrue(tipos(partida).contains(Tipo.PERIGO_TERMINOU), "não terminou no passo " + passo);
    }

    private static ConfigDeVoo comEventos(AjustesDosEventos eventos) {
        ConfigDeVoo m = ConfigDeVoo.medio();
        return new ConfigDeVoo(m.gravidade(), m.empuxo(), m.giroGrausPorSegundo(), m.arrasto(), m.tanque(),
                m.consumoPorSegundo(), m.reabastecimentoPorSegundo(), m.quedaMaxima(), m.deslizeMaximo(),
                m.inclinacaoMaxima(), m.raioDoCasco(), m.peX(), m.peY(), m.tempoDeEmbarque(), m.capacidade(),
                m.vidas(), m.tempoParaRenascer(), m.velocidadeDosAsteroides(), m.bonusMaximo(), eventos);
    }

    private static PartidaDePilotagem partida(ConfigDeVoo config) {
        return new PartidaDePilotagem("Ana", Dificuldade.MEDIO, new Caverna(3000, 1500).base(100, 70), config);
    }

    private static void avancar(PartidaDePilotagem partida, int passos) {
        for (int i = 0; i < passos; i++) {
            partida.avancarUmPasso(Comandos.NENHUM);
        }
    }

    private static List<Tipo> tipos(PartidaDePilotagem partida) {
        return partida.consumirEventos().stream().map(EventoDeVoo::tipo).toList();
    }
}
