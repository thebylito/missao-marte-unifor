package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import com.badlogic.gdx.Input.Keys;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** O adapter só traduz teclas: os códigos da libGDX são constantes, então o teste não precisa de janela. */
class MapaDeTeclasTeste {

    @Test
    void wasdESetasMovemANave() {
        assertEquals(Optional.of(Comando.CIMA), MapaDeTeclas.comandoPara(Keys.W));
        assertEquals(Optional.of(Comando.CIMA), MapaDeTeclas.comandoPara(Keys.UP));
        assertEquals(Optional.of(Comando.BAIXO), MapaDeTeclas.comandoPara(Keys.S));
        assertEquals(Optional.of(Comando.BAIXO), MapaDeTeclas.comandoPara(Keys.DOWN));
        assertEquals(Optional.of(Comando.ESQUERDA), MapaDeTeclas.comandoPara(Keys.A));
        assertEquals(Optional.of(Comando.ESQUERDA), MapaDeTeclas.comandoPara(Keys.LEFT));
        assertEquals(Optional.of(Comando.DIREITA), MapaDeTeclas.comandoPara(Keys.D));
        assertEquals(Optional.of(Comando.DIREITA), MapaDeTeclas.comandoPara(Keys.RIGHT));
    }

    @Test
    void cOuEspacoEmbarcamEQOuEscAbortam() {
        assertEquals(Optional.of(Comando.EMBARCAR), MapaDeTeclas.comandoPara(Keys.C));
        assertEquals(Optional.of(Comando.EMBARCAR), MapaDeTeclas.comandoPara(Keys.SPACE));
        assertEquals(Optional.of(Comando.ABORTAR), MapaDeTeclas.comandoPara(Keys.Q));
        assertEquals(Optional.of(Comando.ABORTAR), MapaDeTeclas.comandoPara(Keys.ESCAPE));
    }

    @Test
    void teclasQueOJogoNaoUsaSaoIgnoradas() {
        for (int tecla : new int[] {Keys.X, Keys.ENTER, Keys.NUM_1, Keys.P, Keys.SHIFT_LEFT}) {
            assertTrue(MapaDeTeclas.comandoPara(tecla).isEmpty(), Keys.toString(tecla));
        }
    }

    @Test
    void asLetrasDoConsoleDaoOMesmoComandoNaJanela() {
        Map<Character, Integer> letras = Map.of('w', Keys.W, 'a', Keys.A, 's', Keys.S, 'd', Keys.D,
                'c', Keys.C, 'q', Keys.Q);
        letras.forEach((letra, tecla) ->
                assertEquals(Optional.of(Comando.daTecla(letra)), MapaDeTeclas.comandoPara(tecla), "tecla " + letra));
    }
}
