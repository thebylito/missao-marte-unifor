package br.edu.unifor.missaomarte.grafico.telas;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;

/**
 * Evita que uma tecla "vaze" de uma tela para a próxima.
 *
 * <p>Uma tecla gera três eventos: {@code keyDown}, {@code keyTyped} (o caractere) e, ao soltar,
 * {@code keyUp}. Se a tela troca dentro do {@code keyDown} (por exemplo, "1" no menu abre a nova
 * partida), o {@code keyTyped} e o {@code keyUp} dessa mesma tecla chegam à tela nova: o nome do piloto
 * começaria com "1". Este filtro só repassa {@code keyTyped} e {@code keyUp} depois que a tela recebeu
 * o seu primeiro {@code keyDown}.
 */
public final class FiltroDeEntrada extends InputAdapter {

    private final InputProcessor alvo;
    private boolean recebeuKeyDown;

    public FiltroDeEntrada(InputProcessor alvo) {
        this.alvo = alvo;
    }

    @Override
    public boolean keyDown(int tecla) {
        recebeuKeyDown = true;
        return alvo.keyDown(tecla);
    }

    @Override
    public boolean keyTyped(char caractere) {
        return recebeuKeyDown && alvo.keyTyped(caractere);
    }

    @Override
    public boolean keyUp(int tecla) {
        return recebeuKeyDown && alvo.keyUp(tecla);
    }

    /** Fase 6: toques (e cliques do mouse) passam direto: não há "toque vazado" de uma tela para a outra. */
    @Override
    public boolean touchDown(int x, int y, int dedo, int botao) {
        return alvo.touchDown(x, y, dedo, botao);
    }

    /** Fase 7: soltar e arrastar o dedo também passam direto (a pilotagem segura os botões). */
    @Override
    public boolean touchUp(int x, int y, int dedo, int botao) {
        return alvo.touchUp(x, y, dedo, botao);
    }

    @Override
    public boolean touchDragged(int x, int y, int dedo) {
        return alvo.touchDragged(x, y, dedo);
    }
}
