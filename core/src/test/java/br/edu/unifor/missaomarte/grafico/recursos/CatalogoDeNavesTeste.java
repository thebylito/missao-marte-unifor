package br.edu.unifor.missaomarte.grafico.recursos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogoDeNavesTeste {
    @TempDir Path pasta;

    @Test
    void leMetadadosOpcionaisSemMudarOrdemDosAtlas() {
        var catalogo = CatalogoDeNaves.ler("""
                [{"id":"azul-2","nome":"Azul II","descricao":"Descrição","linha":"azul","nivel":2},
                 {"id":"verde","nome":"Verde","descricao":"Clássica"}]
                """);
        var primeira = catalogo.naves().get(0);
        assertEquals("Azul II", primeira.nome());
        assertEquals("Descrição", primeira.descricao());
        assertEquals("azul", primeira.linha());
        assertEquals(2, primeira.nivel());
        assertEquals(0, primeira.indice());
        var segunda = catalogo.naves().get(1);
        assertNull(segunda.linha());
        assertNull(segunda.nivel());
        assertEquals(1, segunda.indice());
    }

    @Test
    void skinDesconhecidaViraPadrao() {
        var catalogo = CatalogoDeNaves.ler("[{\"id\":\"verde\",\"nome\":\"Verde\",\"descricao\":\"\"}]");
        assertEquals("padrao", catalogo.skinDisponivel("inexistente"));
        assertEquals("padrao", catalogo.skinDisponivel(null));
        assertEquals("verde", catalogo.skinDisponivel("verde"));
    }

    @Test
    void registraArquivosOpcionaisDaDistribuicao() throws IOException {
        Path naves = Files.createDirectories(pasta.resolve("sprites/naves"));
        Files.writeString(naves.resolve("catalogo.json"), "[{\"id\":\"azul\",\"nome\":\"Azul\",\"descricao\":\"\"}]");
        for (String nome : List.of("azul.png", "previas.png", "sombras.png")) {
            Files.createFile(naves.resolve(nome));
        }
        var arquivos = CatalogoDeAssets.arquivos(new FileHandle(pasta.toFile()));
        assertTrue(arquivos.containsAll(List.of("sprites/naves/catalogo.json", "sprites/naves/azul.png",
                "sprites/naves/previas.png", "sprites/naves/sombras.png")));
    }

    @Test
    void escolheAnguloMaisProximoComVoltaCompleta() {
        assertEquals(0, RecursosDasNaves.quadroDaSombra(11.24));
        assertEquals(1, RecursosDasNaves.quadroDaSombra(11.26));
        assertEquals(15, RecursosDasNaves.quadroDaSombra(-22));
        assertEquals(8, RecursosDasNaves.quadroDaSombra(-180));
        assertEquals(0, RecursosDasNaves.quadroDaSombra(359));
        assertEquals(4, RecursosDasNaves.quadroDaSombra(450));
    }
}
