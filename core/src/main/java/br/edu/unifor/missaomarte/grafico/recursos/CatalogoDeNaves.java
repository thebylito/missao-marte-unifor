package br.edu.unifor.missaomarte.grafico.recursos;

import static br.edu.unifor.missaomarte.grafico.online.Sessao.SKIN_PADRAO;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Metadados da arte instalada. Os índices são as posições originais nos dois atlas. */
public final class CatalogoDeNaves {
    static final String PASTA = "sprites/naves/";
    static final String ARQUIVO = PASTA + "catalogo.json";
    static final String PREVIAS = PASTA + "previas.png";
    static final String SOMBRAS = PASTA + "sombras.png";

    public record Nave(String id, String nome, String descricao, String linha, Integer nivel, int indice) {
        String folha() {
            return SKIN_PADRAO.equals(id) ? Folha.NAVE.getCaminho() : PASTA + id + ".png";
        }
    }

    private final Map<String, Nave> naves;

    private CatalogoDeNaves(List<Nave> naves) {
        this.naves = new LinkedHashMap<>();
        for (Nave nave : naves) {
            this.naves.putIfAbsent(nave.id(), nave);
        }
    }

    public static CatalogoDeNaves ler(String texto) {
        JsonValue raiz = new JsonReader().parse(texto);
        if (raiz == null || !raiz.isArray()) {
            throw new IllegalArgumentException("Esperava uma lista de naves");
        }
        List<Nave> naves = new ArrayList<>();
        int indice = 0;
        for (JsonValue item : raiz) {
            String id = item.getString("id", "");
            if (id.matches("[a-zA-Z0-9_-]+")) {
                Integer nivel = item.has("nivel") && !item.get("nivel").isNull() ? item.getInt("nivel") : null;
                naves.add(new Nave(id, item.getString("nome", id), item.getString("descricao", ""),
                        item.getString("linha", null), nivel, indice));
            }
            indice++;
        }
        return new CatalogoDeNaves(naves);
    }

    static CatalogoDeNaves carregar(FileHandle assets) {
        FileHandle arquivo = assets.child(ARQUIVO);
        if (!arquivo.exists()) {
            return new CatalogoDeNaves(List.of());
        }
        try {
            return ler(arquivo.readString("UTF-8"));
        } catch (RuntimeException e) {
            return new CatalogoDeNaves(List.of());
        }
    }

    CatalogoDeNaves comFolhasInstaladas(FileHandle assets) {
        List<Nave> instaladas = new ArrayList<>();
        for (Nave nave : naves.values()) {
            if (assets.child(nave.folha()).exists()) {
                instaladas.add(nave);
            }
        }
        return new CatalogoDeNaves(instaladas);
    }

    public List<Nave> naves() {
        return List.copyOf(naves.values());
    }

    public Nave nave(String id) {
        return naves.get(id);
    }

    public String skinDisponivel(String id) {
        return naves.containsKey(id) ? id : SKIN_PADRAO;
    }
}
