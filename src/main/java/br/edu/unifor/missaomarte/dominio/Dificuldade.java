package br.edu.unifor.missaomarte.dominio;

/**
 * Cada dificuldade conhece os próprios números (GRASP Information Expert). Na versão original esses
 * valores ficavam espalhados em dois switch dentro do Main.
 */
public enum Dificuldade {
    //      pontos iniciais, passageiros, asteroides, inimigos
    FACIL(30, 4, 1, 1),
    MEDIO(20, 5, 2, 2),
    DIFICIL(45, 5, 3, 3);

    private final int pontosIniciais;
    private final int passageiros;
    private final int asteroides;
    private final int inimigos;

    Dificuldade(int pontosIniciais, int passageiros, int asteroides, int inimigos) {
        this.pontosIniciais = pontosIniciais;
        this.passageiros = passageiros;
        this.asteroides = asteroides;
        this.inimigos = inimigos;
    }

    public int getPontosIniciais() { return pontosIniciais; }
    public int getPassageiros() { return passageiros; }
    public int getAsteroides() { return asteroides; }
    public int getInimigos() { return inimigos; }

    /** Converte o texto digitado; qualquer valor desconhecido vira MEDIO. */
    public static Dificuldade deString(String s) {
        if (s == null) {
            return MEDIO;
        }
        switch (s.trim().toLowerCase()) {
            case "facil":
            case "fácil":
                return FACIL;
            case "dificil":
            case "difícil":
                return DIFICIL;
            default:
                return MEDIO;
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case FACIL: return "Fácil";
            case DIFICIL: return "Difícil";
            default: return "Médio";
        }
    }
}
