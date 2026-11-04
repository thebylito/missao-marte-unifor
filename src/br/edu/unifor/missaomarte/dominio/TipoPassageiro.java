package br.edu.unifor.missaomarte.dominio;

/** Identifica o tipo do passageiro sem que a interface precise usar instanceof. */
public enum TipoPassageiro {
    PROFESSOR("Professor"),
    ENGENHEIRO("Engenheiro"),
    ASTRONAUTA("Astronauta");

    private final String nome;

    TipoPassageiro(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
