package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Posicao;
import org.junit.jupiter.api.Test;

/** Mapa de tamanho 2 (células de -2 a 2) numa área de 500x500 com o canto em (100, 50): células de 100. */
class GeometriaDoMapaTeste {

    private static final float DELTA = 0.001f;
    private final GeometriaDoMapa geometria = new GeometriaDoMapa(new Mapa(2), 100, 50, 500);

    @Test
    void oLadoDaCelulaDivideAAreaPeloNumeroDeCelulas() {
        assertEquals(100, geometria.getLadoDaCelula(), DELTA);
        assertEquals(20, new GeometriaDoMapa(new Mapa(10), 0, 0, 420).getLadoDaCelula(), DELTA);
    }

    @Test
    void oMenorYDoDominioFicaNoTopoDaTela() {
        GeometriaDoMapa.Quadrado cantoSuperiorEsquerdo = geometria.celula(new Posicao(-2, -2));
        assertEquals(100, cantoSuperiorEsquerdo.x(), DELTA);
        assertEquals(450, cantoSuperiorEsquerdo.y(), DELTA);

        GeometriaDoMapa.Quadrado cantoInferiorDireito = geometria.celula(new Posicao(2, 2));
        assertEquals(500, cantoInferiorDireito.x(), DELTA);
        assertEquals(50, cantoInferiorDireito.y(), DELTA);
    }

    @Test
    void aPlataformaFicaNoCentroDaArea() {
        GeometriaDoMapa.Quadrado origem = geometria.celula(Posicao.ORIGEM);
        assertEquals(350, origem.centroX(), DELTA);
        assertEquals(300, origem.centroY(), DELTA);
    }

    @Test
    void wSobeNaTelaEmboraDiminuaOYDoDominio() {
        Posicao acima = Direcao.CIMA.aplicar(Posicao.ORIGEM);
        assertEquals(-1, acima.y());
        assertTrue(geometria.celula(acima).y() > geometria.celula(Posicao.ORIGEM).y());
    }

    @Test
    void oMapaInteiroCabeNaArea() {
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                GeometriaDoMapa.Quadrado q = geometria.celula(new Posicao(x, y));
                assertTrue(q.x() >= 100 && q.x() + q.lado() <= 600 + DELTA, "x de " + x + "," + y);
                assertTrue(q.y() >= 50 - DELTA && q.y() + q.lado() <= 550 + DELTA, "y de " + x + "," + y);
            }
        }
    }

    @Test
    void posicaoFracionariaFicaEntreAsDuasCelulas() {
        GeometriaDoMapa.Quadrado meio = geometria.celula(0.5f, 0f);       // nave deslizando de (0,0) para (1,0)
        assertEquals(400, meio.centroX(), DELTA);
        assertEquals(300, meio.centroY(), DELTA);
        assertEquals(geometria.celula(new Posicao(1, 2)), geometria.celula(1f, 2f));
    }
}
