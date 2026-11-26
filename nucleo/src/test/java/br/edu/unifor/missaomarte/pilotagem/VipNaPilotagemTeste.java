package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import java.util.List;
import org.junit.jupiter.api.Test;

class VipNaPilotagemTeste {

    private static final double X = 900;
    private static final double SUPERFICIE = 500;
    private final PartidaDePilotagem partida = new PartidaDePilotagem("Ana", Dificuldade.MEDIO,
            new Caverna(3000, 1500).base(100, 70), ConfigDeVoo.medio());

    @Test
    void vipSurgeNoProximoPassoNaPosicaoDoServidorEEntraNoHistorico() {
        avancar(17);
        partida.iniciarVip(X, SUPERFICIE);
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertTrue(partida.eventosAplicados().isEmpty());

        avancar(1);
        PlataformaDePouso capsula = partida.getCapsulaVip().orElseThrow();
        assertEquals(PlataformaDePouso.Tipo.VIP, capsula.getTipo());
        assertEquals(X, capsula.getX());
        assertEquals(SUPERFICIE, capsula.getSuperficie());
        assertEquals(160, capsula.getLargura());
        assertEquals(36, capsula.getAltura());
        assertEquals(List.of(Tipo.VIP_SURGIU), tipos());
        assertEquals(List.of(new EventoAplicado(TipoDeEvento.PASSAGEIRO_VIP, 0, 18)), partida.eventosAplicados());
        assertEquals(1, partida.getCaverna().getPlataformas().size());
        assertThrows(IllegalArgumentException.class, () -> partida.iniciarEvento(TipoDeEvento.PASSAGEIRO_VIP, 3));
    }

    @Test
    void pousoSeguroPegaVipSemPontosPousoContadoOuEmbarque() {
        iniciarVip();
        posicionarParaPouso(-10);
        avancar(240);

        assertEquals(NaveEmVoo.Estado.POUSADA, partida.getNave().getEstado());
        assertEquals(partida.getCapsulaVip().orElseThrow(), partida.getNave().getPlataforma());
        assertEquals(List.of(Tipo.VIP_PEGO), tipos());
        assertEquals(0, partida.getPontos());
        assertEquals(0, partida.getPousos());
        assertTrue(partida.getNave().getABordo().isEmpty());
    }

    @Test
    void pousoInseguroNaCapsulaExplodeComoPousoDuro() {
        iniciarVip();
        posicionarParaPouso(-90);
        avancar(1);
        EventoDeVoo explosao = partida.consumirEventos().get(0);
        assertEquals(Tipo.EXPLODIU, explosao.tipo());
        assertTrue(explosao.mensagem().startsWith("Pouso duro!"));
        assertEquals(NaveEmVoo.Estado.DESTRUIDA, partida.getNave().getEstado());
        assertEquals(0, partida.getPontos());
        assertEquals(0, partida.getPousos());
    }

    @Test
    void capsulaOcupadaContinuaAposExpirarESomeAoDecolar() {
        iniciarVip();
        posicionarParaPouso(-10);
        avancar(5400);
        assertEquals(List.of(Tipo.VIP_PEGO), tipos());
        assertTrue(partida.getCapsulaVip().isPresent());

        partida.avancarUmPasso(Comandos.MOTOR);
        assertEquals(NaveEmVoo.Estado.VOANDO, partida.getNave().getEstado());
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertEquals(List.of(Tipo.DECOLOU, Tipo.VIP_SUMIU), tipos());
        assertEquals(0, partida.getPontos());
    }

    @Test
    void capsulaDesocupadaExpiraEmQuarentaECincoSegundos() {
        iniciarVip();
        avancar(5399);
        assertTrue(partida.getCapsulaVip().isPresent());
        avancar(1);
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertEquals(List.of(Tipo.VIP_SUMIU), tipos());
        assertEquals(0, partida.getPontos());
    }

    @Test
    void avisoDeOutroPilotoRemoveCapsulaDesocupadaUmaVez() {
        iniciarVip();
        partida.encerrarVip();
        partida.encerrarVip();
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertEquals(List.of(Tipo.VIP_SUMIU), tipos());
        assertEquals(0, partida.getPontos());
    }

    @Test
    void avisoDeOutroPilotoCancelaCapsulaQueAindaNaoSurgiu() {
        partida.iniciarVip(X, SUPERFICIE);
        partida.encerrarVip();
        avancar(1);
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertTrue(tipos().isEmpty());
        assertTrue(partida.eventosAplicados().isEmpty());
    }

    @Test
    void avisoDeOutroPilotoPreservaCapsulaOcupadaAteADecolagem() {
        iniciarVip();
        posicionarParaPouso(-10);
        avancar(1);
        partida.encerrarVip();
        assertTrue(partida.getCapsulaVip().isPresent());
        assertEquals(List.of(Tipo.VIP_PEGO), tipos());

        partida.avancarUmPasso(Comandos.MOTOR);
        assertTrue(partida.getCapsulaVip().isEmpty());
        assertEquals(List.of(Tipo.DECOLOU, Tipo.VIP_SUMIU), tipos());
    }

    private void iniciarVip() {
        partida.iniciarVip(X, SUPERFICIE);
        avancar(1);
        assertEquals(List.of(Tipo.VIP_SURGIU), tipos());
    }

    private void posicionarParaPouso(double velocidadeY) {
        NaveEmVoo nave = partida.getNave();
        nave.estado = NaveEmVoo.Estado.VOANDO;
        nave.plataforma = null;
        nave.x = X + 80;
        nave.y = SUPERFICIE - partida.getConfig().peY();
        nave.velocidadeY = velocidadeY;
    }

    private void avancar(int passos) {
        for (int i = 0; i < passos; i++) {
            partida.avancarUmPasso(Comandos.NENHUM);
        }
    }

    private List<Tipo> tipos() {
        return partida.consumirEventos().stream().map(EventoDeVoo::tipo).toList();
    }
}
