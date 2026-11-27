package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class ProgressoDaHistoriaTeste {
    @Test void iniciarMarcaRetomadaSemConcluir() {
        var p=ProgressoDaHistoria.novo().iniciarM01();
        assertEquals("M01",p.retomada());
        assertFalse(p.m01Concluida());
    }
    @Test void concluirLimpaRetomadaEMantemMelhorResultadoNaRepeticao() {
        var p=ProgressoDaHistoria.novo().iniciarM01().concluirM01(80);
        assertTrue(p.m01Concluida());assertEquals("",p.retomada());
        assertEquals(80,p.iniciarM01().concluirM01(40).melhorPontuacao());
        assertTrue(p.iniciarM01().m01Concluida());
        assertEquals(95,p.iniciarM01().concluirM01(95).melhorPontuacao());
    }
    @Test void progressoInvalidoNaoPodeSerCriado() {
        assertThrows(IllegalArgumentException.class,()->new ProgressoDaHistoria(false,"M99",0));
        assertThrows(IllegalArgumentException.class,()->ProgressoDaHistoria.novo().concluirM01(-1));
    }
    @Test void primeiroSinalComecaSeguroComMariaEUmaRotaAberta() {
        var encontro=MissaoHistoria.primeiroSinal();
        var voo=encontro.partida();
        assertEquals(1,voo.getCaverna().totalDePassageiros());
        assertEquals(br.edu.unifor.missaomarte.pilotagem.NaveEmVoo.Estado.POUSADA,voo.getNave().getEstado());
        assertTrue(voo.getCaverna().getPlataformas().get(1).getEsperando().orElseThrow().getNome().contains("Maria"));
        assertTrue(encontro.inimigoX()>voo.getNave().getX()+500);
    }
}
