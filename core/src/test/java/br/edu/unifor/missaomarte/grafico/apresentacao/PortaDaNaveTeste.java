package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PortaDaNaveTeste {
    @Test void abreAntesDaCaminhadaEFechaDepoisDoEmbarque() {
        var porta = new PortaDaNave();
        porta.atualizar(.15f, true, true);
        assertEquals(1, porta.abertura(), .001);
        assertEquals(0, PortaDaNave.progressoDaEntrada(.25f), .001);
        assertEquals(1, PortaDaNave.progressoDaEntrada(1), .001);
        porta.atualizar(.11f, true, false);
        assertEquals(.5, porta.abertura(), .001);
        porta.atualizar(.11f, true, false);
        assertEquals(0, porta.abertura(), .001);
    }

    @Test void decolarDuranteOEmbarqueFechaAPorta() {
        var porta = new PortaDaNave();
        porta.atualizar(.1f, true, true);
        assertTrue(porta.abertura() > 0);
        porta.atualizar(.01f, false, true);
        assertEquals(0, porta.abertura());
    }

    @Test void pousoSemPassageiroOuSemLugarMantemAPortaFechada() {
        var porta = new PortaDaNave();
        porta.atualizar(1, true, false);
        assertEquals(0, porta.abertura());
    }
}
