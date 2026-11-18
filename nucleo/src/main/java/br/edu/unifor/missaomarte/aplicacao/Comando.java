package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.Direcao;
import java.util.Optional;

/** O que o jogador pediu no turno, já independente de como foi digitado. */
public enum Comando {
    CIMA(Direcao.CIMA),
    BAIXO(Direcao.BAIXO),
    ESQUERDA(Direcao.ESQUERDA),
    DIREITA(Direcao.DIREITA),
    EMBARCAR(null),
    ABORTAR(null),
    INVALIDO(null);

    private final Direcao direcao;

    Comando(Direcao direcao) {
        this.direcao = direcao;
    }

    /** Preenchido só nos comandos de movimento. */
    public Optional<Direcao> getDirecao() {
        return Optional.ofNullable(direcao);
    }

    /** Teclas do jogo de console: w/s/a/d movem, c embarca, q aborta. */
    public static Comando daTecla(char tecla) {
        switch (tecla) {
            case 'w': return CIMA;
            case 's': return BAIXO;
            case 'a': return ESQUERDA;
            case 'd': return DIREITA;
            case 'c': return EMBARCAR;
            case 'q': return ABORTAR;
            default: return INVALIDO;
        }
    }
}
