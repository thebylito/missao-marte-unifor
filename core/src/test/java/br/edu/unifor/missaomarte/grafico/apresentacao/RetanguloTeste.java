package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** As três perguntas que o layout e os toques fazem a um retângulo (Fase 6). */
class RetanguloTeste {

    private final Retangulo r = new Retangulo(10, 20, 100, 50);   // de (10,20) até (110,70)

    @Test
    void toqueNaBordaContaComoDentro() {
        assertTrue(r.contem(10, 20));
        assertTrue(r.contem(110, 70));
        assertTrue(r.contem(60, 45));
        assertFalse(r.contem(9.9f, 45));
        assertFalse(r.contem(60, 70.1f));
    }

    @Test
    void encostarNaoEhSobrepor() {
        assertFalse(r.sobrepoe(new Retangulo(110, 20, 30, 30)), "lado a lado");
        assertFalse(r.sobrepoe(new Retangulo(10, 70, 30, 30)), "um em cima do outro");
        assertTrue(r.sobrepoe(new Retangulo(109, 69, 30, 30)), "um pixel em comum");
    }

    @Test
    void distanciaDeUmPontoAteORetangulo() {
        assertEquals(0, r.distancia(60, 45), 1e-6, "dentro");
        assertEquals(0, r.distancia(10, 20), 1e-6, "na borda");
        assertEquals(5, r.distancia(5, 45), 1e-6, "à esquerda");
        assertEquals(8, r.distancia(60, 78), 1e-6, "acima");
        assertEquals(5, r.distancia(113, 74), 1e-6, "na diagonal do canto (3, 4, 5)");
    }

    @Test
    void dentroDeAceitaOMesmoTamanhoMasNaoUmPixelParaFora() {
        assertTrue(r.dentroDe(r));
        assertTrue(new Retangulo(20, 30, 10, 10).dentroDe(r));
        assertFalse(new Retangulo(9, 30, 10, 10).dentroDe(r));
        assertFalse(new Retangulo(20, 30, 100, 10).dentroDe(r));
    }
}
