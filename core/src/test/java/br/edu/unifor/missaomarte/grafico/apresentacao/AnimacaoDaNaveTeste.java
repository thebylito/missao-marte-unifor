package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimacaoDaNaveTeste {
    @Test void motorSobeGradualmenteEEsfriaAoDesligar() {
        var animacao = new AnimacaoDaNave();
        animacao.atualizar(.06f, true, false);
        assertEquals(.5, animacao.motor(), .001);
        animacao.atualizar(.06f, true, false);
        assertEquals(1, animacao.motor(), .001);
        animacao.atualizar(.14f, false, false);
        assertEquals(.5, animacao.motor(), .001);
        animacao.atualizar(.14f, false, false);
        assertEquals(0, animacao.motor(), .001);
    }

    @Test void pousoMaisForteComprimeMaisEAmortece() {
        var leve = new AnimacaoDaNave();
        var forte = new AnimacaoDaNave();
        leve.pousar(15);
        forte.pousar(60);
        leve.atualizar(.1f, false, false);
        forte.atualizar(.1f, false, false);
        assertTrue(forte.compressao() > leve.compressao());
        forte.atualizar(2, false, false);
        assertTrue(forte.compressao() < .001);
    }

    @Test void bracoConectaEDesconectaSemUltrapassarOsLimites() {
        var animacao = new AnimacaoDaNave();
        animacao.atualizar(.325f, false, true);
        assertEquals(.5, animacao.braco(), .001);
        animacao.atualizar(1, false, true);
        assertEquals(1, animacao.braco(), .001);
        animacao.atualizar(1, false, false);
        assertEquals(0, animacao.braco(), .001);
    }
}
