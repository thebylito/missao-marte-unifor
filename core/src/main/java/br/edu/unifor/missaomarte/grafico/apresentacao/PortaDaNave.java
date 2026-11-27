package br.edu.unifor.missaomarte.grafico.apresentacao;

/** A abertura acompanha o embarque, e o fechamento continua depois de o passageiro entrar. */
public final class PortaDaNave {
    private float abertura;

    public void atualizar(float segundos, boolean pousada, boolean embarcando) {
        if (!pousada) {
            abertura = 0;
            return;
        }
        float passo = Math.max(0, segundos) / (embarcando ? .15f : .22f);
        abertura = embarcando ? Math.min(1, abertura + passo) : Math.max(0, abertura - passo);
    }

    public float abertura() { return abertura; }

    public static float progressoDaEntrada(float progressoDoEmbarque) {
        return Math.max(0, Math.min(1, (progressoDoEmbarque - .25f) / .75f));
    }
}
