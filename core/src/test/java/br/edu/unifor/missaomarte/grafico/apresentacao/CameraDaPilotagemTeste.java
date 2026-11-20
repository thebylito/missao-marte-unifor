package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** CT-22: a câmera segue a nave sem mostrar o lado de fora da caverna (3840 x 1440). */
class CameraDaPilotagemTeste {

    private final CameraDaPilotagem camera = new CameraDaPilotagem(3840, 1440);

    @Test
    void naPrimeiraVezVaiDiretoParaANaveUmPoucoAFrente() {
        double[] c = camera.seguir(2000, 700, 100, 0, 1280, 720, 1 / 60.0);
        assertEquals(2000 + 100 * CameraDaPilotagem.ANTECIPACAO, c[0], 1e-9);
        assertEquals(700, c[1], 1e-9);
    }

    @Test
    void depoisSegueComSuavidade() {
        camera.seguir(2000, 700, 0, 0, 1280, 720, 1 / 60.0);
        double[] c = camera.seguir(2300, 700, 0, 0, 1280, 720, 1 / 60.0);
        assertTrue(c[0] > 2000 && c[0] < 2300, "andou só um pouco: " + c[0]);
    }

    @Test
    void nuncaMostraOLadoDeForaDaCaverna() {
        double[] canto = camera.seguir(100, 150, -300, -300, 1280, 720, 0);
        assertEquals(640, canto[0], 1e-9, "metade da largura visível");
        assertEquals(360, canto[1], 1e-9);
        double[] outroCanto = camera.seguir(3800, 1400, 300, 300, 1280, 720, 0);
        assertEquals(3840 - 640, outroCanto[0], 1e-9);
        assertEquals(1440 - 360, outroCanto[1], 1e-9);
    }

    @Test
    void numaTelaMaiorQueACavernaACavernaFicaNoMeio() {
        // celular em pé: a câmera mostra 1280 de largura e 2773 de altura, mais alto que a caverna
        double[] c = camera.seguir(500, 300, 0, 0, 1280, 2773, 0);
        assertEquals(1440 / 2.0, c[1], 1e-9);
        assertEquals(640, c[0], 1e-9);
    }
}
