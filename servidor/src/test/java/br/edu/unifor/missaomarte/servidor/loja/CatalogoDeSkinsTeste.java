package br.edu.unifor.missaomarte.servidor.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Fase 8: o catálogo da loja ({@code skins.json}) é lido uma vez, na subida do servidor. Um catálogo com erro
 * impede o servidor de subir, em vez de aparecer como uma loja quebrada no jogo.
 */
class CatalogoDeSkinsTeste {

    private static String skin(String id, String linha, int nivel, int preco) {
        return "{\"id\":\"" + id + "\",\"nome\":\"Nave " + id + "\",\"linha\":\"" + linha + "\",\"nivel\":" + nivel
                + ",\"preco\":" + preco + "}";
    }

    private static final String PADRAO = skin("padrao", "padrao", 1, 0);

    @Test
    void oCatalogoDoServidorTemAsDuasLinhas() {
        CatalogoDeSkins catalogo = new CatalogoDeSkins();
        assertThat(catalogo.todas()).extracting(Skin::id).containsExactly("padrao", "padrao_2", "padrao_3",
                "padrao_4", "padrao_5", "guara", "guara_2", "guara_3", "guara_4");
        assertThat(catalogo.todas()).extracting(Skin::preco).containsExactly(0, 60, 120, 200, 300, 60, 120, 200,
                300);
        assertThat(catalogo.buscar("guara")).hasValueSatisfying(s -> {
            assertThat(s.nome()).isEqualTo("Guará");
            assertThat(s.linha()).isEqualTo("guara");
            assertThat(s.nivel()).isEqualTo(1);
        });
        assertThat(catalogo.buscar("bruma")).isEmpty();
    }

    @Test
    void asSkinsSaemEmOrdemDeLinhaENivel() {
        CatalogoDeSkins catalogo = CatalogoDeSkins.de("[" + String.join(",", skin("guara_2", "guara", 2, 120),
                PADRAO, skin("guara", "guara", 1, 60), skin("padrao_2", "padrao", 2, 60)) + "]");
        // as linhas na ordem em que aparecem; dentro de cada uma, os níveis em ordem
        assertThat(catalogo.todas()).extracting(Skin::id).containsExactly("guara", "guara_2", "padrao", "padrao_2");
    }

    @Test
    void recusaIdForaDoPadrao() {
        for (String ruim : new String[] {"Guara", "guará", "guara-2", "", "a".repeat(33)}) {
            assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin(ruim, "guara", 1, 60) + "]"))
                    .as(ruim).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void recusaIdRepetido() {
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin("guara", "guara", 1, 60) + ","
                + skin("guara", "guara", 2, 120) + "]"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("guara");
    }

    @Test
    void recusaNivelRepetidoNaMesmaLinha() {
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin("guara", "guara", 1, 60) + ","
                + skin("guara_2", "guara", 1, 120) + "]"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("guara_2");
    }

    @Test
    void exigeAPadraoDeGraca() {
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + skin("guara", "guara", 1, 60) + "]"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("padrao");
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + skin("padrao", "padrao", 1, 10) + "]"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("padrao");
    }

    @Test
    void recusaPrecoNegativoNivelZeroENomeVazio() {
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin("guara", "guara", 1, -1) + "]"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin("guara", "guara", 0, 60) + "]"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + ","
                + "{\"id\":\"guara\",\"nome\":\" \",\"linha\":\"guara\",\"nivel\":1,\"preco\":60}]"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoDeSkins.de("[" + PADRAO + "," + skin("guara", "Guará", 1, 60) + "]"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaJsonInvalido() {
        assertThatThrownBy(() -> CatalogoDeSkins.de("não é json")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoDeSkins.de("{}")).isInstanceOf(IllegalStateException.class);
    }
}
