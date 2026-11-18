package br.edu.unifor.missaomarte.grafico.audio;

import br.edu.unifor.missaomarte.grafico.Diario;
import br.edu.unifor.missaomarte.grafico.recursos.Recursos;

/**
 * Toca os sons carregados pelo {@link Recursos}. Se não houver placa de som ou o som ainda não tiver carregado, o
 * jogo continua em silêncio: som é retorno, não regra.
 */
public final class SonsLibgdx implements Sons {

    private static final float VOLUME = 0.7f;

    private final Recursos recursos;
    private boolean mudo;

    public SonsLibgdx(Recursos recursos) {
        this.recursos = recursos;
    }

    @Override
    public void tocar(Som som) {
        if (mudo || !recursos.isCarregado()) {
            return;
        }
        try {
            recursos.som(som).play(VOLUME);
        } catch (RuntimeException e) {
            Diario.registrar("som indisponível (" + som + "): " + e.getMessage());
        }
    }

    @Override
    public void alternarMudo() {
        mudo = !mudo;
        Diario.registrar(mudo ? "som desligado" : "som ligado");
    }

    @Override
    public boolean isMudo() {
        return mudo;
    }
}
