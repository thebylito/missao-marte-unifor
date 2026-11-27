package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimacaoDoPassageiroTeste {
    @Test void rampaAlinhadaComecaNoChaoESobeGradualmente() {
        assertEquals(0,AnimacaoDoPassageiro.subidaNaRampa(0,0));
        float inicio=AnimacaoDoPassageiro.subidaNaRampa(.01f,0);
        assertTrue(inicio>0 && inicio<1);
        assertEquals(26,AnimacaoDoPassageiro.subidaNaRampa(1,0));
        assertEquals(0,AnimacaoDoPassageiro.subidaNaRampa(.5f,30));
    }

    @Test void aproximacaoExecutaReacaoUmaVezEConservaAcenoNaMargem() {
        var animacao = new AnimacaoDoPassageiro(0);
        animacao.atualizar(.1f, 260, false);
        assertEquals(AnimacaoDoPassageiro.Fase.REACAO, animacao.fase());
        animacao.atualizar(.5f, 275, false);
        assertEquals(AnimacaoDoPassageiro.Fase.REACAO, animacao.fase());
        animacao.atualizar(.5f, 275, false);
        assertEquals(AnimacaoDoPassageiro.Fase.SINALIZAR, animacao.fase());
        animacao.atualizar(.1f, 320, false);
        assertEquals(AnimacaoDoPassageiro.Fase.ESPERA, animacao.fase());
    }

    @Test void embarqueInterrompeReacaoEUsaCicloDeCaminhada() {
        var animacao = new AnimacaoDoPassageiro(4);
        animacao.atualizar(.1f, 100, false);
        animacao.atualizar(.1f, 100, true);
        assertEquals(AnimacaoDoPassageiro.Fase.ANDAR, animacao.fase());
        assertEquals(0, animacao.quadro());
        animacao.atualizarEmbarque(.5f, .7f);
        animacao.atualizar(.5f, 100, true);
        assertEquals(4, animacao.quadro());
        animacao.atualizar(.1f, 100, false);
        assertEquals(AnimacaoDoPassageiro.Fase.ANDAR, animacao.fase());
        animacao.atualizarEmbarque(.3f, 0);
        animacao.atualizar(.1f, 100, false);
        assertEquals(AnimacaoDoPassageiro.Fase.ESPERA, animacao.fase());
    }

    @Test void protegeDoPerigoEVaiParaOCicloNormalDepois() {
        var animacao = new AnimacaoDoPassageiro(0);
        animacao.atualizar(.1f, 500, false, true);
        assertEquals(AnimacaoDoPassageiro.Fase.PROTEGER, animacao.fase());
        animacao.atualizar(.7f, 500, false, false);
        assertEquals(AnimacaoDoPassageiro.Fase.ESPERA, animacao.fase());
        animacao.atualizar(.1f, 20, true, true);
        assertEquals(AnimacaoDoPassageiro.Fase.ANDAR, animacao.fase());
    }

    @Test void pulsosCurtosDoMotorNaoFazemAPoseOscilar() {
        var animacao = new AnimacaoDoPassageiro(0);
        for (int i = 0; i < 30; i++) {
            animacao.atualizar(.016f, 100, false, i % 2 == 0);
            assertEquals(AnimacaoDoPassageiro.Fase.PROTEGER, animacao.fase());

        }
        animacao.atualizar(.016f, 80, true, false);
        assertEquals(AnimacaoDoPassageiro.Fase.ANDAR, animacao.fase());
        animacao.atualizar(.016f, 80, true, true);
        assertEquals(AnimacaoDoPassageiro.Fase.ANDAR, animacao.fase());
    }

    @Test void protecaoContinuaAnimadaComTransicaoEntreQuadros() {
        var animacao = new AnimacaoDoPassageiro(0);
        animacao.atualizar(.016f, 100, false, true);
        int primeiro = animacao.quadro();
        animacao.atualizar(.3f, 100, false, true);
        assertNotEquals(primeiro, animacao.quadro());
    }

    @Test void interrupcaoDoEmbarqueFazOPassageiroRetornarGradualmente() {
        var animacao = new AnimacaoDoPassageiro(0);
        animacao.atualizarEmbarque(.2f, .6f);
        animacao.atualizarEmbarque(.016f, 0);
        assertTrue(animacao.progressoDoEmbarque() > .5f);
        animacao.atualizarEmbarque(.3f, 0);
        assertEquals(0, animacao.progressoDoEmbarque());
    }

    @Test void navePousandoAoLadoNaoInterrompeOAceno() {
        var animacao = new AnimacaoDoPassageiro(0);
        animacao.atualizar(.1f, 200, false);
        animacao.atualizar(.7f, 180, false);
        for (int i = 0; i < 30; i++) {
            boolean jato = AnimacaoDoPassageiro.jatoAlcanca(0, 100 - i, 0, 44, 24);
            assertFalse(jato);
            animacao.atualizar(.016f, 100, false, jato);
            assertEquals(AnimacaoDoPassageiro.Fase.SINALIZAR, animacao.fase());
        }
    }

    @Test void protecaoExigeJatoDirecionadoEAoAlcance() {
        assertTrue(AnimacaoDoPassageiro.jatoAlcanca(0, 100, 0, 0, 24));
        assertTrue(AnimacaoDoPassageiro.jatoAlcanca(0, 100, 45, 50, 50));
        assertFalse(AnimacaoDoPassageiro.jatoAlcanca(0, 100, 0, 44, 24));
        assertFalse(AnimacaoDoPassageiro.jatoAlcanca(0, 100, 180, 0, 24));
        assertFalse(AnimacaoDoPassageiro.jatoAlcanca(0, 100, 0, 0, -80));
    }

    @Test void passageirosAfastadosNaoRepetemOMesmoQuadro() {
        var primeiro = new AnimacaoDoPassageiro(0);
        var segundo = new AnimacaoDoPassageiro(1);
        assertNotEquals(primeiro.quadro(), segundo.quadro());
        primeiro.atualizar(30, 500, false);
        assertTrue(primeiro.quadro() < 16);
    }
}
