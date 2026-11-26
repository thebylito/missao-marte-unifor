package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class CatalogoDeCavernasTeste {

    private final JsonMapper json = new JsonMapper();
    private final Resource marciana = new ClassPathResource("cavernas/marciana.json");

    @Test
    void springCarregaAsTresCavernasESoLiberaAMarciana() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(JsonMapper.class, () -> json);
            contexto.scan(CatalogoDeCavernas.class.getPackageName());
            contexto.refresh();
            var catalogo = contexto.getBean(CatalogoDeCavernas.class);
            assertThat(catalogo.todas()).extracting(DescricaoDeCaverna::id)
                    .containsExactlyInAnyOrder("marciana", "poco-polar", "veio-de-ferro");
            assertThat(catalogo.liberadas()).extracting(DescricaoDeCaverna::id).containsExactly("marciana");
            assertThat(catalogo.porId("poco-polar")).get().extracting(DescricaoDeCaverna::nome).isEqualTo("Poço Polar");
            assertThat(catalogo.porId("ausente")).isEmpty();
        }
    }

    @Test
    void excluiInvalidaERegistraOsProblemas(CapturedOutput log) throws Exception {
        var invalida = json.readTree(marciana.getContentAsByteArray()).asObject();
        invalida.put("id", "teste");
        invalida.putNull("base");
        var catalogo = new CatalogoDeCavernas(json, new Resource[]{recurso(json.writeValueAsString(invalida)), marciana});
        assertThat(catalogo.todas()).extracting(DescricaoDeCaverna::id).containsExactly("marciana");
        assertThat(catalogo.porId("teste")).isEmpty();
        assertThat(log.getAll()).containsPattern("WARN[^\n]*teste[^\n]*não tem base");
    }

    @Test
    void jsonIlegivelNaoImpedeACargaDasDemais(CapturedOutput log) throws Exception {
        var catalogo = new CatalogoDeCavernas(json, new Resource[]{recurso("{"), marciana});
        assertThat(catalogo.todas()).extracting(DescricaoDeCaverna::id).containsExactly("marciana");
        assertThat(log.getAll()).containsPattern("WARN[^\n]*teste.json");
    }

    @Test
    void idRepetidoImpedeASubida() {
        assertThatThrownBy(() -> new CatalogoDeCavernas(json, new Resource[]{marciana, marciana}))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("repetido").hasMessageContaining("marciana");
    }

    @Test
    void ordenaAsLiberadasPorId() throws Exception {
        var outra = json.readTree(marciana.getContentAsByteArray()).asObject();
        outra.put("id", "aurora");
        var catalogo = new CatalogoDeCavernas(json, new Resource[]{marciana, recurso(json.writeValueAsString(outra))});
        assertThat(catalogo.liberadas()).extracting(DescricaoDeCaverna::id).containsExactly("aurora", "marciana");
    }

    private Resource recurso(String conteudo) {
        return new ByteArrayResource(conteudo.getBytes(StandardCharsets.UTF_8), "teste.json");
    }
}
