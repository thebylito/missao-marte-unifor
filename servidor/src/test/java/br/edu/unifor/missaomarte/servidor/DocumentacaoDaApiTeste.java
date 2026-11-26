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
    void publicaAsRotasDaHordaESeuErroDeJanelaNoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/hordas/atual'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/hordas/atual'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/ranking/semanal'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/ranking/semanal'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/admin/eventos'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/partidas'].post.responses['409']"
                        + ".content['application/problem+json'].example.motivo").value("HORDA_ENCERRADA"))
                .andExpect(jsonPath("$.paths['/api/partidas/inicio'].post.responses['409']"
                        + ".content['application/problem+json'].example.motivo").value("HORDA_ENCERRADA"));
    }

    @Test
    void catalogoDaLojaNaoHerdaAutenticacaoDoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/loja'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/loja'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/loja/compras'].post.security").doesNotExist());
    }

    @Test
    void semEnderecoPublicoFicaODeDesenvolvimento() {
        OpenAPI doc = new DocumentacaoDaApi().documentacao("  ");
        assertEquals("http://localhost:8080", doc.getServers().get(0).getUrl());
        assertEquals("desenvolvimento", doc.getServers().get(0).getDescription());
    }
}
