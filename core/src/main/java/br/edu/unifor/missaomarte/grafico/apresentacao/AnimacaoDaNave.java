package br.edu.unifor.missaomarte.grafico.apresentacao;

/** Respostas visuais do motor, da suspensão e do abastecimento. */
public final class AnimacaoDaNave {
    private float motor;
    private float braco;
    private float desdePouso = 10;
    private float impacto;

    public void atualizar(float delta, boolean ligado, boolean abastecendo) {
        float passo = Math.max(0, delta);
        motor = aproximar(motor, ligado ? 1 : 0, passo / (ligado ? .12f : .28f));
        braco = aproximar(braco, abastecendo ? 1 : 0, passo / .65f);
        desdePouso += passo;
    }

    public void pousar(double velocidadeVertical) {
        impacto = (float) Math.min(1, Math.max(.15, velocidadeVertical / 90));
        desdePouso = 0;
    }

    private static float aproximar(float atual, float destino, float passo) {
        return atual < destino ? Math.min(destino, atual + passo) : Math.max(destino, atual - passo);
    }

    public float compressao() {
        return impacto * (float) (Math.abs(Math.sin(desdePouso * 11)) * Math.exp(-desdePouso * 5));
    }

    public float motor() { return motor; }
    public float braco() { return braco; }
}
