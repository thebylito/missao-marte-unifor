package br.edu.unifor.missaomarte.grafico.recursos;

import br.edu.unifor.missaomarte.dominio.Direcao;

/**
 * Contas das folhas de sprites, sem OpenGL: onde fica cada quadro dentro do PNG e qual quadro mostrar em cada
 * instante. A classe {@link Recursos} só aplica estes recortes às texturas carregadas.
 */
public final class FolhaDeSprites {

    /** Retângulo de um quadro dentro da imagem, com a origem no canto superior esquerdo (como o PNG). */
    public record Recorte(int x, int y, int largura, int altura) {
    }

    private FolhaDeSprites() {
    }

    public static Recorte recorte(Folha folha, int quadro) {
        if (quadro < 0 || quadro >= folha.getQuadros()) {
            throw new IllegalArgumentException(folha + " não tem o quadro " + quadro);
        }
        int coluna = quadro % folha.getColunas();
        int linha = quadro / folha.getColunas();
        return new Recorte(coluna * folha.getLargura(), linha * folha.getAltura(), folha.getLargura(),
                folha.getAltura());
    }

    /** Animação em laço (passageiros acenando, asteroide girando): volta ao primeiro quadro depois do último. */
    public static int quadroEmLaco(Folha folha, float segundos, float quadrosPorSegundo) {
        int n = (int) Math.floor(Math.max(0, segundos) * quadrosPorSegundo);
        return n % folha.getQuadros();
    }

    /** Animação que roda uma vez (explosão): para no último quadro. */
    public static int quadroUmaVez(Folha folha, float segundos, float quadrosPorSegundo) {
        int n = (int) Math.floor(Math.max(0, segundos) * quadrosPorSegundo);
        return Math.min(n, folha.getQuadros() - 1);
    }

    /** Quanto tempo a animação leva para mostrar todos os quadros uma vez. */
    public static float duracao(Folha folha, float quadrosPorSegundo) {
        return folha.getQuadros() / quadrosPorSegundo;
    }

    /**
     * Quadro da nave virada para a direção do movimento. A folha tem 64 ângulos, de 5,625 em 5,625 graus no sentido
     * anti-horário, com o quadro 0 de bico para cima: 16 é a esquerda (90°), 32 é baixo (180°) e 48 é a direita (270°).
     * No domínio, CIMA diminui o y, que na tela é para cima.
     */
    public static int quadroDaNave(Direcao direcao) {
        switch (direcao) {
            case ESQUERDA: return 16;
            case BAIXO: return 32;
            case DIREITA: return 48;
            default: return 0;
        }
    }
}
