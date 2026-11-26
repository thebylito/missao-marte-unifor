package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class ContratoDaHordaTeste {

    private final JsonMapper json = new JsonMapper();

    @Test
    void exemploPublicadoCorrespondeAoSorteioDaHorda42() throws IOException {
        var esperada = SorteioDaHorda.sortear(42, 20261271L, List.of(CavernasEmJsonTeste.ler("marciana")), null);
        try (var entrada = exemplo("horda.json")) {
            assertThat(json.readValue(entrada, DescricaoDaHorda.class)).isEqualTo(esperada);
        }
    }

    /** Fase 8 (DA-48): as hordas gravadas antes dos ajustes não têm o campo, e usam o padrão. */
    @Test
    void semOCampoAjustesAHordaUsaOPadrao() throws IOException {
        ObjectNode semAjustes;
        try (var entrada = exemplo("horda.json")) {
            semAjustes = (ObjectNode) json.readTree(entrada);
        }
        semAjustes.remove("ajustes");

        DescricaoDaHorda lida = json.treeToValue(semAjustes, DescricaoDaHorda.class);

        assertThat(lida.ajustes()).isEqualTo(AjustesDeVoo.PADRAO);
        assertThat(lida.roteiro()).hasSize(2);
    }

    /** O exemplo de /api/ajustes é o padrão no formato em que o servidor o escreve. */
    @Test
    void exemploDosAjustesEOPadraoNoFormatoDoServidor() throws IOException {
        JsonNode publicado;
        try (var entrada = exemplo("ajustes.json")) {
            publicado = json.readTree(entrada);
        }

        assertThat(json.treeToValue(publicado, AjustesDeVoo.class)).isEqualTo(AjustesDeVoo.PADRAO);
        JsonNode escrito = json.valueToTree(AjustesDeVoo.PADRAO);
        assertThat(escrito).isEqualTo(publicado);
    }

    private static InputStream exemplo(String nome) throws IOException {
        return new ClassPathResource("exemplos/" + nome).getInputStream();
    }
}
