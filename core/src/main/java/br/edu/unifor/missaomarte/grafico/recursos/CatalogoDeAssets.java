package br.edu.unifor.missaomarte.grafico.recursos;

import br.edu.unifor.missaomarte.grafico.audio.Som;
import java.util.ArrayList;
import java.util.List;

/**
 * Tudo o que o jogo carrega de {@code assets/}, num só lugar. Java puro: os testes conferem, sem abrir janela, que
 * cada arquivo existe, tem o formato esperado e tem a origem registrada em {@code assets/ORIGEM.md}.
 */
public final class CatalogoDeAssets {

    /** Fontes TrueType (licença SIL Open Font License, texto em {@code fontes/OFL-ChakraPetch.txt}). */
    public static final String FONTE_TEXTO = "fontes/ChakraPetch-Regular.ttf";
    public static final String FONTE_TITULO = "fontes/ChakraPetch-SemiBold.ttf";

    /** Ícone da janela (o launcher desktop usa; não passa pelo AssetManager). */
    public static final String ICONE = "sprites/icone.png";

    private CatalogoDeAssets() {
    }

    /** Todos os arquivos usados pelo jogo, com caminho relativo a {@code assets/}. */
    public static List<String> arquivos() {
        List<String> todos = new ArrayList<>();
        for (Folha f : Folha.values()) {
            todos.add(f.getCaminho());
        }
        for (Som s : Som.values()) {
            todos.add(s.getCaminho());
        }
        todos.add(FONTE_TEXTO);
        todos.add(FONTE_TITULO);
        todos.add(ICONE);
        return todos;
    }
}
