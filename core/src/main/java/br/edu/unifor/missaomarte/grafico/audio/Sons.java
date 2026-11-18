package br.edu.unifor.missaomarte.grafico.audio;

/**
 * Quem toca os efeitos sonoros. As telas e a cena da partida dependem desta interface, não da libGDX: nos testes
 * entra uma implementação que só anota o que foi pedido, e nada depende de placa de som.
 */
public interface Sons {

    void tocar(Som som);

    /** Liga ou desliga o som (tecla M na partida). */
    void alternarMudo();

    boolean isMudo();

    /** Objeto nulo (padrão Null Object): não toca nada e está sempre mudo. Serve antes do carregamento. */
    Sons NENHUM = new Sons() {
        @Override
        public void tocar(Som som) {
        }

        @Override
        public void alternarMudo() {
        }

        @Override
        public boolean isMudo() {
            return true;
        }
    };
}
