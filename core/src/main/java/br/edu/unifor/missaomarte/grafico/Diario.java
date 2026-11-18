package br.edu.unifor.missaomarte.grafico;

import com.badlogic.gdx.Gdx;

/**
 * Registra no log as etapas do ciclo de vida (create, resize, pause, resume, dispose e troca de telas),
 * para que o aluno veja a ordem em que a libGDX chama cada método. Nos testes não há aplicação
 * (Gdx.app é nulo) e nada é registrado.
 */
public final class Diario {

    public static final String ETIQUETA = "Ciclo";

    private Diario() {
    }

    public static void registrar(String mensagem) {
        if (Gdx.app != null) {
            Gdx.app.log(ETIQUETA, mensagem);
        }
    }
}
