package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import com.badlogic.gdx.Input.Keys;
import java.util.Optional;

/**
 * Adapter entre o teclado da libGDX e o {@link Comando} do jogo. Só traduz: não decide nada sobre a partida.
 * O console recebe letras ({@link Comando#daTecla(char)}); a janela recebe códigos de tecla.
 */
public final class MapaDeTeclas {

    private MapaDeTeclas() {
    }

    /** Vazio para teclas que o jogo não usa: elas são ignoradas e não gastam turno. */
    public static Optional<Comando> comandoPara(int tecla) {
        switch (tecla) {
            case Keys.W:
            case Keys.UP:
                return Optional.of(Comando.CIMA);
            case Keys.S:
            case Keys.DOWN:
                return Optional.of(Comando.BAIXO);
            case Keys.A:
            case Keys.LEFT:
                return Optional.of(Comando.ESQUERDA);
            case Keys.D:
            case Keys.RIGHT:
                return Optional.of(Comando.DIREITA);
            case Keys.C:
            case Keys.SPACE:
                return Optional.of(Comando.EMBARCAR);
            case Keys.Q:
            case Keys.ESCAPE:
                return Optional.of(Comando.ABORTAR);
            default:
                return Optional.empty();
        }
    }
}
