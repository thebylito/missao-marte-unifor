package br.edu.unifor.missaomarte.grafico.apresentacao;

/** O que aparece numa célula do mapa. Equivale aos símbolos do console (@ P E T # X L .). */
public enum Conteudo {
    VAZIO('.'),
    PLATAFORMA('L'),
    NAVE('@'),
    PROFESSOR('P'),
    ENGENHEIRO('E'),
    ASTRONAUTA('T'),
    ASTEROIDE('#'),
    INIMIGO('X');

    private final char simbolo;

    Conteudo(char simbolo) {
        this.simbolo = simbolo;
    }

    /** O mesmo símbolo do console; a janela o escreve dentro da célula. */
    public char getSimbolo() {
        return simbolo;
    }
}
