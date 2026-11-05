package br.edu.unifor.missaomarte.dominio;

/** Direções de movimento na grade. O eixo y cresce para baixo: CIMA diminui y. */
public enum Direcao {
    CIMA(0, -1),
    BAIXO(0, 1),
    ESQUERDA(-1, 0),
    DIREITA(1, 0);

    private final int dx;
    private final int dy;

    Direcao(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Posicao aplicar(Posicao origem) {
        return origem.deslocar(dx, dy);
    }
}
