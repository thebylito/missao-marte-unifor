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
    GELO_POLAR("sprites/gelo_polar.png", 96, 96, 8, 4, 32),
    CRATERADO("sprites/craterado.png", 96, 96, 8, 4, 32),
    FERRO("sprites/ferro.png", 96, 96, 8, 4, 32),
    METEORO("sprites/meteoro.png", 96, 96, 8, 4, 32),
    ASTRONAUTA_ESPERA("sprites/astronauta_espera.png", 64, 80, 8, 2, 16),
    ENGENHEIRO_ESPERA("sprites/engenheiro_espera.png", 64, 80, 8, 2, 16),
    PROFESSOR_ESPERA("sprites/professor_espera.png", 64, 80, 8, 2, 16),
    SONDA("sprites/sonda.png", 384, 256, 4, 2, 8),
    GUINDASTE("sprites/guindaste.png", 384, 256, 4, 2, 8),
    ANTENA("sprites/antena.png", 384, 256, 4, 2, 8),
    PAREDE_GELO("sprites/parede_gelo.png", 256, 256, 1, 1, 1),
    PAREDE_FERRO("sprites/parede_ferro.png", 256, 256, 1, 1, 1),
    ASTRONAUTA_REACAO("sprites/astronauta_reacao.png", 64, 80, 6, 2, 12),
    ASTRONAUTA_ANDAR("sprites/astronauta_andar.png", 64, 80, 6, 2, 12),
    ASTRONAUTA_SINALIZAR("sprites/astronauta_sinalizar.png", 64, 80, 6, 2, 12),
    ENGENHEIRO_REACAO("sprites/engenheiro_reacao.png", 64, 80, 6, 2, 12),
    ENGENHEIRO_ANDAR("sprites/engenheiro_andar.png", 64, 80, 6, 2, 12),
    ENGENHEIRO_SINALIZAR("sprites/engenheiro_sinalizar.png", 64, 80, 6, 2, 12),
    PROFESSOR_REACAO("sprites/professor_reacao.png", 64, 80, 6, 2, 12),
    PROFESSOR_ANDAR("sprites/professor_andar.png", 64, 80, 6, 2, 12),
    PROFESSOR_SINALIZAR("sprites/professor_sinalizar.png", 64, 80, 6, 2, 12),
    ASTRONAUTA_PROTEGER("sprites/astronauta_proteger.png", 64, 80, 6, 2, 12),
    ASTRONAUTA_COMEMORAR("sprites/astronauta_comemorar.png", 64, 80, 6, 2, 12),
    ENGENHEIRO_PROTEGER("sprites/engenheiro_proteger.png", 64, 80, 6, 2, 12),
    ENGENHEIRO_COMEMORAR("sprites/engenheiro_comemorar.png", 64, 80, 6, 2, 12),
    PROFESSOR_PROTEGER("sprites/professor_proteger.png", 64, 80, 6, 2, 12),
    PROFESSOR_COMEMORAR("sprites/professor_comemorar.png", 64, 80, 6, 2, 12),
    ABRIGO("sprites/abrigo.png", 128, 128, 4, 2, 8),
    INIMIGO("sprites/inimigo.png", 96, 96, 8, 1, 8),
    ASTRONAUTA("sprites/astronauta.png", 48, 64, 8, 1, 8),
    ENGENHEIRO("sprites/engenheiro.png", 48, 64, 8, 1, 8),
    PROFESSOR("sprites/professor.png", 48, 64, 8, 1, 8),
    PLATAFORMA("sprites/plataforma_base.png", 256, 64, 1, 1, 1),
    FUNDO_POLAR("sprites/fundo_polar.png", 1200, 900, 1, 1, 1),
    FUNDO_FERRO("sprites/fundo_ferro.png", 1200, 900, 1, 1, 1),
    FUNDO("sprites/fundo.png", 1200, 900, 1, 1, 1),
    // Fase 7 (pilotagem): já estavam em assets/ desde a Fase 4, vindos da POC
    CHAMA("sprites/chama.png", 64, 96, 4, 1, 4),
    ROCHA("sprites/rocha_tile.png", 256, 256, 1, 1, 1),
    PLATAFORMA_PASSAGEIRO("sprites/plataforma_passageiro.png", 160, 48, 1, 1, 1);

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
