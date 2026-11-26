package br.edu.unifor.missaomarte.pilotagem.horda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Os dados da horda: os tipos de evento e as listas imutáveis dos records (o servidor e o cliente leem do JSON). */
class DescricaoDaHordaTeste {

    @Test
    void cadaEventoTemADuracaoDaTabelaDaAnalise() {
        assertEquals(20, TipoDeEvento.CHUVA_DE_METEOROS.duracaoSegundos());
        assertEquals(25, TipoDeEvento.TEMPESTADE.duracaoSegundos());
        assertEquals(20, TipoDeEvento.PANE_DE_GRAVIDADE.duracaoSegundos());
        assertEquals(45, TipoDeEvento.PASSAGEIRO_VIP.duracaoSegundos());
    }

    @Test
    void soOVipNaoEPerigo() {
        assertTrue(TipoDeEvento.CHUVA_DE_METEOROS.perigo());
        assertTrue(TipoDeEvento.TEMPESTADE.perigo());
        assertTrue(TipoDeEvento.PANE_DE_GRAVIDADE.perigo());
        assertFalse(TipoDeEvento.PASSAGEIRO_VIP.perigo());
    }

    @Test
    void aDescricaoDaCavernaCopiaAsListas() {
        List<RochaDescrita> rochas = new ArrayList<>(List.of(new RochaDescrita(0, 0, 100, 10)));
        List<PontoDePouso> pontos = new ArrayList<>(List.of(new PontoDePouso(10, 46)));
        List<AsteroideDescrito> asteroides = new ArrayList<>();
        DescricaoDeCaverna c = new DescricaoDeCaverna("teste", "Teste", 100, 100, rochas, new PontoDePouso(0, 58),
                pontos, asteroides, true);

        rochas.add(new RochaDescrita(0, 90, 100, 10));
        pontos.clear();
        asteroides.add(new AsteroideDescrito(50, 50, 5, 0, 0, 1, 0, 0));

        assertEquals(1, c.rochas().size());
        assertEquals(1, c.pontosDePouso().size());
        assertEquals(0, c.asteroides().size());
        assertThrows(UnsupportedOperationException.class, () -> c.rochas().add(new RochaDescrita(0, 0, 1, 1)));
    }

    @Test
    void aDescricaoDaHordaCopiaAsListas() {
        List<PassageiroNaHorda> passageiros = new ArrayList<>(List.of(new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 0)));
        List<Double> fases = new ArrayList<>(List.of(1.0));
        List<Modificador> modificadores = new ArrayList<>();
        List<PerigoDoRoteiro> roteiro = new ArrayList<>(List.of(new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 30, 7)));
        DescricaoDaHorda h = new DescricaoDaHorda(1, Dificuldade.MEDIO, null, passageiros, 1.0, fases, modificadores,
                roteiro);

        passageiros.clear();
        fases.add(2.0);
        modificadores.add(new Modificador(TipoDeModificador.VENTO, 10));
        roteiro.clear();

        assertEquals(1, h.passageiros().size());
        assertEquals(List.of(1.0), h.fasesDosAsteroides());
        assertEquals(List.of(), h.modificadores());
        assertEquals(1, h.roteiro().size());
        assertThrows(UnsupportedOperationException.class, () -> h.fasesDosAsteroides().add(3.0));
    }

    @Test
    void registrosIguaisSaoIguais() {
        assertEquals(new Modificador(TipoDeModificador.GRAVIDADE, 1.15), new Modificador(TipoDeModificador.GRAVIDADE, 1.15));
        assertEquals(new PerigoDoRoteiro(TipoDeEvento.PANE_DE_GRAVIDADE, 42.5, -3L),
                new PerigoDoRoteiro(TipoDeEvento.PANE_DE_GRAVIDADE, 42.5, -3L));
    }
}
