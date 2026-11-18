package br.edu.unifor.missaomarte.grafico.recursos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.grafico.recursos.FolhaDeSprites.Recorte;
import org.junit.jupiter.api.Test;

/** Contas das folhas de sprites: recortes, animação em laço e de uma vez, e a direção da nave. */
class FolhaDeSpritesTeste {

    @Test
    void recorteSegueAGradeDaEsquerdaParaADireitaEDeCimaParaBaixo() {
        assertEquals(new Recorte(0, 0, 128, 128), FolhaDeSprites.recorte(Folha.NAVE, 0));
        assertEquals(new Recorte(128, 0, 128, 128), FolhaDeSprites.recorte(Folha.NAVE, 1));
        assertEquals(new Recorte(128, 128, 128, 128), FolhaDeSprites.recorte(Folha.NAVE, 9));      // 2ª linha
        assertEquals(new Recorte(896, 896, 128, 128), FolhaDeSprites.recorte(Folha.NAVE, 63));     // último
        assertEquals(new Recorte(384, 384, 128, 128), FolhaDeSprites.recorte(Folha.EXPLOSAO, 15));
        assertEquals(new Recorte(336, 0, 48, 64), FolhaDeSprites.recorte(Folha.PROFESSOR, 7));
    }

    @Test
    void quadroForaDaFolhaEhErro() {
        assertThrows(IllegalArgumentException.class, () -> FolhaDeSprites.recorte(Folha.INIMIGO, 8));
        assertThrows(IllegalArgumentException.class, () -> FolhaDeSprites.recorte(Folha.INIMIGO, -1));
    }

    @Test
    void animacaoEmLacoVoltaAoComeco() {
        assertEquals(0, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, 0f, 8));
        assertEquals(3, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, 0.4f, 8));
        assertEquals(7, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, 0.99f, 8));
        assertEquals(0, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, 1.0f, 8));     // 8 quadros em 1 s: volta
        assertEquals(1, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, 1.13f, 8));
        assertEquals(0, FolhaDeSprites.quadroEmLaco(Folha.ASTRONAUTA, -5f, 8));      // tempo negativo não quebra
    }

    @Test
    void animacaoDeUmaVezParaNoUltimoQuadro() {
        assertEquals(0, FolhaDeSprites.quadroUmaVez(Folha.EXPLOSAO, 0f, 20));
        assertEquals(10, FolhaDeSprites.quadroUmaVez(Folha.EXPLOSAO, 0.52f, 20));
        assertEquals(15, FolhaDeSprites.quadroUmaVez(Folha.EXPLOSAO, 0.8f, 20));
        assertEquals(15, FolhaDeSprites.quadroUmaVez(Folha.EXPLOSAO, 30f, 20));
        assertEquals(0.8f, FolhaDeSprites.duracao(Folha.EXPLOSAO, 20), 1e-6);
    }

    @Test
    void naveApontaParaADirecaoDoMovimento() {
        // 64 ângulos de 5,625° no sentido anti-horário, quadro 0 de bico para cima
        assertEquals(0, FolhaDeSprites.quadroDaNave(Direcao.CIMA));
        assertEquals(16, FolhaDeSprites.quadroDaNave(Direcao.ESQUERDA));    // 90°
        assertEquals(32, FolhaDeSprites.quadroDaNave(Direcao.BAIXO));       // 180°
        assertEquals(48, FolhaDeSprites.quadroDaNave(Direcao.DIREITA));     // 270°
    }
}
