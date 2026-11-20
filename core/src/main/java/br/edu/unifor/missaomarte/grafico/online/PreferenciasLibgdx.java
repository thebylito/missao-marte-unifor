package br.edu.unifor.missaomarte.grafico.online;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Preferências da libGDX: no Android, as SharedPreferences do app; no desktop, um arquivo em ~/.prefs. Abre na
 * primeira leitura (o {@code Gdx.app} só existe depois de a janela abrir).
 */
public final class PreferenciasLibgdx implements ConfiguracaoDoServidor.Preferencias {

    private final String nome;
    private Preferences preferencias;

    public PreferenciasLibgdx(String nome) {
        this.nome = nome;
    }

    private Preferences abrir() {
        if (preferencias == null) {
            preferencias = Gdx.app.getPreferences(nome);
        }
        return preferencias;
    }

    @Override
    public String ler(String chave) {
        return Gdx.app == null ? null : abrir().getString(chave, null);
    }

    @Override
    public void gravar(String chave, String valor) {
        if (Gdx.app != null) {
            abrir().putString(chave, valor);
            abrir().flush();
        }
    }
}
