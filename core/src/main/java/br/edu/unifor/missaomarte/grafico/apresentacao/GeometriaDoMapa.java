package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Posicao;

/**
 * Converte uma célula do mapa em um quadrado na tela, dentro de uma área quadrada.
 *
 * <p>Os eixos são diferentes: no domínio, "w" diminui o y (no console, a linha de cima tem o menor y);
 * na libGDX o y da tela cresce para cima. Por isso a linha do menor y fica no topo da área.
 */
public final class GeometriaDoMapa {

    /** Quadrado na tela, com o canto inferior esquerdo em (x, y). */
    public record Quadrado(float x, float y, float lado) {
        public float centroX() { return x + lado / 2; }
        public float centroY() { return y + lado / 2; }
    }

    private final Mapa mapa;
    private final float esquerda;
    private final float topo;
    private final float ladoDaCelula;

    /**
     * @param esquerda   x do canto esquerdo da área do mapa
     * @param base       y do canto inferior da área do mapa
     * @param ladoDaArea tamanho da área quadrada onde o mapa inteiro precisa caber
     */
    public GeometriaDoMapa(Mapa mapa, float esquerda, float base, float ladoDaArea) {
        this.mapa = mapa;
        this.esquerda = esquerda;
        this.topo = base + ladoDaArea;
        this.ladoDaCelula = ladoDaArea / (mapa.getMax() - mapa.getMin() + 1);
    }

    public float getLadoDaCelula() {
        return ladoDaCelula;
    }

    public Quadrado celula(Posicao p) {
        int coluna = p.x() - mapa.getMin();
        int linha = p.y() - mapa.getMin();            // linha 0 = menor y = topo da área
        float x = esquerda + coluna * ladoDaCelula;
        float y = topo - (linha + 1) * ladoDaCelula;
        return new Quadrado(x, y, ladoDaCelula);
    }
}
