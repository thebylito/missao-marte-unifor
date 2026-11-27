package br.edu.unifor.missaomarte.grafico.apresentacao;

/** Transições visuais com histerese, para o aceno não oscilar na borda do alcance. */
public final class AnimacaoDoPassageiro {
    public enum Fase { ESPERA, REACAO, SINALIZAR, ANDAR, PROTEGER, COMEMORAR }

    private Fase fase = Fase.ESPERA;
    private float tempo;
    private float ameacaRestante;
    private float progressoDoEmbarque;
    private Fase faseAnterior = Fase.ESPERA;
    private int quadroAnterior;
    private float transicao = 1;
    private final float defasagem;

    public AnimacaoDoPassageiro(float defasagem) {
        this.defasagem = defasagem;
    }

    public void atualizar(float segundos, double distancia, boolean embarcando) {
        atualizar(segundos, distancia, embarcando, false);
    }

    public void atualizar(float segundos, double distancia, boolean embarcando, boolean ameacado) {
        float delta = Math.max(0, segundos);
        tempo += delta;
        transicao = Math.min(1, transicao + delta / .12f);
        ameacaRestante = ameacado ? .6f : Math.max(0, ameacaRestante - delta);
        if (embarcando || progressoDoEmbarque > 0) {
            mudarPara(Fase.ANDAR);
        } else if (ameacaRestante > 0) {
            mudarPara(Fase.PROTEGER);
        } else if (fase == Fase.PROTEGER) {
            mudarPara(Fase.ESPERA);
        } else if (distancia >= 320) {
            mudarPara(Fase.ESPERA);
        } else if (fase == Fase.ESPERA && distancia < 270) {
            mudarPara(Fase.REACAO);
        } else if (fase == Fase.REACAO && tempo >= .6f) {
            mudarPara(Fase.SINALIZAR);
        } else if (fase == Fase.ANDAR) {
            mudarPara(Fase.ESPERA);
        }
    }

    /** O jato é um cone abaixo do bocal; estar perto da nave não basta para ser atingido. */
    public static boolean jatoAlcanca(double bocalX, double bocalY, double anguloEmGraus,
                                      double passageiroX, double passageiroY) {
        double angulo = Math.toRadians(anguloEmGraus);
        double direcaoX = Math.sin(angulo);
        double direcaoY = -Math.cos(angulo);
        double dx = passageiroX - bocalX;
        double dy = passageiroY - bocalY;
        double distancia = dx * direcaoX + dy * direcaoY;
        double afastamento = Math.abs(dx * direcaoY - dy * direcaoX);
        return distancia >= 0 && distancia <= 110 && afastamento <= 8 + distancia * .07;
    }

    private void mudarPara(Fase proxima) {
        if (fase != proxima) {
            quadroAnterior = quadro();
            faseAnterior = fase;
            fase = proxima;
            tempo = 0;
            transicao = 0;
        }
    }

    public Fase fase() { return fase; }
    public Fase faseAnterior() { return faseAnterior; }
    public int quadroAnterior() { return quadroAnterior; }
    public float transicao() { return transicao; }
    public float progressoDoEmbarque() { return progressoDoEmbarque; }

    public void atualizarEmbarque(float segundos, float destino) {
        float alvo = Math.max(0, Math.min(1, destino));
        float passo = Math.max(0, segundos) * 3;
        progressoDoEmbarque = progressoDoEmbarque < alvo
                ? Math.min(alvo, progressoDoEmbarque + passo)
                : Math.max(alvo, progressoDoEmbarque - passo);
    }

    /** Altura contínua da entrada: mesmo com a escotilha alinhada, começa no piso. */
    public static float subidaNaRampa(float progresso, float distanciaDaPorta) {
        float perto = Math.max(0, Math.min(1, (24 - Math.abs(distanciaDaPorta)) / 24));
        float avanco = Math.max(0, Math.min(1, progresso / .65f));
        return perto * avanco * 26;
    }

    private float posicaoNoCiclo() {
        return switch (fase) {
            case ESPERA -> (tempo + defasagem) * 6;
            case REACAO -> Math.min(11, tempo * 20);
            case PROTEGER -> tempo * 8;
            case SINALIZAR, COMEMORAR -> (tempo + defasagem) * 12;
            case ANDAR -> progressoDoEmbarque * 24;
        };
    }

    public int quadro() {
        return (int) posicaoNoCiclo() % (fase == Fase.ESPERA ? 16 : 12);
    }

}
