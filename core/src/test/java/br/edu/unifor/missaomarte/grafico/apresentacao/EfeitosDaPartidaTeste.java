package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos.Impacto;
import org.junit.jupiter.api.Test;

/** Explosões e tremor avançando com o tempo que o teste informa. */
class EfeitosDaPartidaTeste {

    private final EfeitosDaPartida efeitos = new EfeitosDaPartida();
    private final Posicao celula = new Posicao(-2, 3);

    @Test
    void semImpactoNaoHaEfeito() {
        efeitos.aplicar(Impacto.NENHUM, celula);
        assertFalse(efeitos.emAndamento());
        assertTrue(efeitos.explosoes().isEmpty());
        assertEquals(0, efeitos.tremorX());
    }

    @Test
    void colisaoExplodeNaCelulaDaNaveERodaOs16QuadrosUmaVez() {
        efeitos.aplicar(Impacto.LEVE, celula);
        assertTrue(efeitos.emAndamento());
        assertEquals(celula, efeitos.explosoes().get(0).celula());
        assertEquals(0, efeitos.explosoes().get(0).quadro());
        efeitos.avancar(0.41f);
        assertEquals(8, efeitos.explosoes().get(0).quadro());          // 20 quadros por segundo
        efeitos.avancar(0.38f);
        assertEquals(15, efeitos.explosoes().get(0).quadro());
        efeitos.avancar(0.02f);
        assertTrue(efeitos.explosoes().isEmpty(), "terminou em 0,8 s");
        assertFalse(efeitos.emAndamento());
        assertFalse(efeitos.isNaveDestruida(), "colisão leve: a nave continua no mapa");
    }

    @Test
    void naveDestruidaTemExplosaoMaiorESomeDoMapa() {
        efeitos.aplicar(Impacto.FATAL, celula);
        assertTrue(efeitos.isNaveDestruida());
        EfeitosDaPartida leve = new EfeitosDaPartida();
        leve.aplicar(Impacto.LEVE, celula);
        assertTrue(efeitos.explosoes().get(0).tamanho() > leve.explosoes().get(0).tamanho());
    }

    @Test
    void tremorDiminuiAteParar() {
        efeitos.aplicar(Impacto.FATAL, celula);
        efeitos.avancar(0.01f);
        float inicio = Math.abs(efeitos.tremorX()) + Math.abs(efeitos.tremorY());
        assertTrue(inicio > 0);
        efeitos.avancar(0.5f);
        float depois = Math.abs(efeitos.tremorX()) + Math.abs(efeitos.tremorY());
        assertTrue(depois < 12 * 0.5f, "a amplitude cai com o tempo");
        efeitos.avancar(1);
        assertEquals(0, efeitos.tremorX());
        assertEquals(0, efeitos.tremorY());
    }
}
