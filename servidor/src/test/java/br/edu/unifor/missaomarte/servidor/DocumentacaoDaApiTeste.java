package br.edu.unifor.missaomarte.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O endereço que o Swagger usa (Fase 6, publicação): o público, quando configurado; senão, o de desenvolvimento.
 * O servidor sobe com MISSAOMARTE_URL_PUBLICA simulada pela propriedade.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:missaomarte-documentacao;DB_CLOSE_DELAY=-1" + ApiTeste.MODO_POSTGRESQL,
        "missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais",
        "missaomarte.url-publica=https://ranking.exemplo.com"})
@AutoConfigureMockMvc
class DocumentacaoDaApiTeste {

    @Autowired
    private MockMvc mvc;

    @Test
    void publicadoODocumentoApontaParaOEnderecoPublico() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://ranking.exemplo.com"))
                .andExpect(jsonPath("$.servers[0].description").value("publicado"));
    }

    @Test
    void semEnderecoPublicoFicaODeDesenvolvimento() {
        OpenAPI doc = new DocumentacaoDaApi().documentacao("  ");
        assertEquals("http://localhost:8080", doc.getServers().get(0).getUrl());
        assertEquals("desenvolvimento", doc.getServers().get(0).getDescription());
    }
}
