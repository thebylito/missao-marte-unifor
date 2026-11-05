package br.edu.unifor.missaomarte.dominio;

/** Uma célula do mapa. Imutável: mover a nave produz uma nova posição. */
public record Posicao(int x, int y) {

    /** A plataforma de pouso fica sempre na origem. */
    public static final Posicao ORIGEM = new Posicao(0, 0);

    public Posicao deslocar(int dx, int dy) {
        return new Posicao(x + dx, y + dy);
    }
}
