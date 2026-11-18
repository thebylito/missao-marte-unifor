package br.edu.unifor.missaomarte.grafico.recursos;

/**
 * As folhas de sprites que a Fase 4 usa, com a mesma geometria do {@code assets/sprites/manifest.json} gerado pelo
 * Blender ({@code arte/blender/render_all.py}). Cada folha é uma grade de quadros do mesmo tamanho, lida da esquerda
 * para a direita e de cima para baixo. Um teste confere que estes números batem com o manifesto e com os PNGs.
 */
public enum Folha {
    //            arquivo                         largura altura colunas linhas quadros
    NAVE("sprites/nave.png", 128, 128, 8, 8, 64),
    EXPLOSAO("sprites/explosao.png", 128, 128, 4, 4, 16),
    ASTEROIDE("sprites/asteroide.png", 96, 96, 8, 4, 32),
    INIMIGO("sprites/inimigo.png", 96, 96, 8, 1, 8),
    ASTRONAUTA("sprites/astronauta.png", 48, 64, 8, 1, 8),
    ENGENHEIRO("sprites/engenheiro.png", 48, 64, 8, 1, 8),
    PROFESSOR("sprites/professor.png", 48, 64, 8, 1, 8),
    PLATAFORMA("sprites/plataforma_base.png", 256, 64, 1, 1, 1),
    FUNDO("sprites/fundo.png", 1200, 900, 1, 1, 1);

    private final String caminho;
    private final int largura;
    private final int altura;
    private final int colunas;
    private final int linhas;
    private final int quadros;

    Folha(String caminho, int largura, int altura, int colunas, int linhas, int quadros) {
        this.caminho = caminho;
        this.largura = largura;
        this.altura = altura;
        this.colunas = colunas;
        this.linhas = linhas;
        this.quadros = quadros;
    }

    /** Caminho dentro de {@code assets/}. */
    public String getCaminho() { return caminho; }
    /** Largura de um quadro, em pixels. */
    public int getLargura() { return largura; }
    /** Altura de um quadro, em pixels. */
    public int getAltura() { return altura; }
    public int getColunas() { return colunas; }
    public int getLinhas() { return linhas; }
    public int getQuadros() { return quadros; }

    /** Nome usado no manifesto (o nome do arquivo sem a extensão). */
    public String getNomeNoManifesto() {
        return caminho.substring(caminho.lastIndexOf('/') + 1, caminho.length() - ".png".length());
    }
}
