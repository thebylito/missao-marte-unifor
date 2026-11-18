package br.edu.unifor.missaomarte.aplicacao;

/**
 * Os modos de jogo (Fase 7). O clássico é o jogo de grade das fases anteriores (e o único do console); a pilotagem é
 * a melhoria da Fase 7. Os nomes das constantes são os mesmos da API do servidor (campo {@code modo}).
 */
public enum Modo {
    CLASSICO("Clássico"),
    PILOTAGEM("Pilotagem");

    private final String nome;

    Modo(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /** O outro modo (para alternar com uma tecla ou um botão). */
    public Modo outro() {
        return this == CLASSICO ? PILOTAGEM : CLASSICO;
    }
}
