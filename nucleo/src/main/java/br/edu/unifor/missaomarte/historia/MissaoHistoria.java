package br.edu.unifor.missaomarte.historia;

import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;

/** Conteúdo autoral da missão disponível nesta entrega. */
public final class MissaoHistoria {
    public static final String ID = "M01";
    public static final String TITULO = "Primeiro sinal";
    public static final String BRIEFING = "Uma tempestade interrompeu as comunicações da expedição. "
            + "Maria conseguiu transmitir um pedido de socorro. Siga a baliza azul, pouse devagar "
            + "e traga-a de volta à base.";
    private MissaoHistoria() {}
    public static EncontroDeResgate primeiroSinal() { return EncontroDeResgate.novo(); }
}
