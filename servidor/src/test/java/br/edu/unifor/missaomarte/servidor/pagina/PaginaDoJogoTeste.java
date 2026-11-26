package br.edu.unifor.missaomarte.servidor.pagina;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Publicação: o próprio servidor entrega a página do jogo (ranking ao vivo e downloads) na mesma origem da API.
 * A página e os downloads são públicos; o resto da API continua exigindo o token.
 */
class PaginaDoJogoTeste extends ApiTeste {

    @TempDir
    static Path downloads;

    @DynamicPropertySource
    static void pastaDeDownloads(DynamicPropertyRegistry registro) {
        registro.add("missaomarte.downloads", () -> downloads.toString());
    }

    @BeforeAll
    static void criarArquivos() throws IOException {
        Files.write(downloads.resolve("missao-marte.apk"), new byte[] {1, 2, 3, 4});
    }

    @Test
    void aRaizEntregaAPaginaSemToken() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                // recurso estático vai sem charset no Content-Type: o navegador usa o <meta charset="utf-8">
                .andExpect(r -> assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8),
                        containsString("<title>Missão Marte Unifor</title>")));
    }

    @Test
    void osArquivosDaPaginaSaoPublicos() throws Exception {
        mvc.perform(get("/pagina/app.js")).andExpect(status().isOk());
        mvc.perform(get("/pagina/estilo.css")).andExpect(status().isOk());
        mvc.perform(get("/pagina/icone.png")).andExpect(status().isOk());
    }

    @Test
    void painelDeEventosEntregaOsFormulariosEOJavaScriptSemToken() throws Exception {
        mvc.perform(get("/pagina/admin.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"login\"")))
                .andExpect(content().string(containsString("id=\"evento\"")));
        mvc.perform(get("/pagina/admin.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/sessoes")))
                .andExpect(content().string(containsString("/api/admin/eventos")));
    }

    @Test
    void osDownloadsVemDaPastaConfiguradaSemToken() throws Exception {
        mvc.perform(get("/downloads/missao-marte.apk"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3, 4}));
        mvc.perform(head("/downloads/missao-marte.apk"))
                .andExpect(status().isOk())
                .andExpect(header().longValue("Content-Length", 4));
    }

    @Test
    void downloadQueNaoExisteDa404ENao401() throws Exception {
        mvc.perform(get("/downloads/nao-existe.zip")).andExpect(status().isNotFound());
    }

    @Test
    void oRestoDaApiContinuaExigindoOToken() throws Exception {
        mvc.perform(get("/api/partidas/minhas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/qualquer-outra-coisa")).andExpect(status().isUnauthorized());
    }
}
