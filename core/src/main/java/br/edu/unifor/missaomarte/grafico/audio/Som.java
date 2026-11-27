package br.edu.unifor.missaomarte.grafico.audio;

/**
 * Efeitos sonoros do jogo. Os arquivos são gerados por {@code arte/sons/gerar_sons.py} (síntese PCM, sem nenhum
 * som de fora) e ficam em {@code assets/sons}.
 */
public enum Som {
    AMBIENTE_MARTE("sons/ambiente_marte.wav"),
    AMBIENTE_GELO("sons/ambiente_gelo.wav"),
    AMBIENTE_FERRO("sons/ambiente_ferro.wav"),
    EMBARQUE_ALTERNATIVO("sons/embarque_alternativo.wav"),
    MOVER("sons/mover.wav"),
    EMBARQUE("sons/embarque.wav"),
    COLISAO("sons/colisao.wav"),
    EXPLOSAO("sons/explosao.wav"),
    ALERTA("sons/alerta.wav"),
    INVALIDO("sons/invalido.wav"),
    VITORIA("sons/vitoria.wav"),
    DERROTA("sons/derrota.wav");

    private final String caminho;

    Som(String caminho) {
        this.caminho = caminho;
    }

    /** Caminho dentro de {@code assets/}. */
    public String getCaminho() {
        return caminho;
    }
}
