package br.edu.unifor.missaomarte.servidor.sombras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.pilotagem.Caverna;
import org.junit.jupiter.api.Test;

/**
 * Fase 8 (DA-39): o servidor compila o {@code nucleo}. O canal das sombras confere as posições com o tamanho da
 * caverna do próprio jogo, em vez de uma cópia dos números.
 */
class NucleoNoServidorTeste {

    @Test
    void oCanalDasSombrasUsaOTamanhoDaCavernaDoNucleo() {
        Caverna caverna = Caverna.marciana();
        assertEquals(caverna.getLargura(), CanalDeSombras.LARGURA_DA_CAVERNA);
        assertEquals(caverna.getAltura(), CanalDeSombras.ALTURA_DA_CAVERNA);
        assertEquals(3840, CanalDeSombras.LARGURA_DA_CAVERNA);
        assertEquals(1440, CanalDeSombras.ALTURA_DA_CAVERNA);
    }
}
