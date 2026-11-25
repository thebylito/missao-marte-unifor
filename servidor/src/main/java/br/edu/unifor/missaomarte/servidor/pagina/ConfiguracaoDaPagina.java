package br.edu.unifor.missaomarte.servidor.pagina;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Publicação: o servidor entrega, na mesma origem da API, a página do jogo e os arquivos para baixar.
 * <ul>
 *   <li>A página ({@code /}, {@code /pagina/**}) vem de {@code static/} dentro do jar, como em qualquer Spring Boot.
 *       Ela lê o ranking de {@code /api/ranking}: mesma origem, então não é preciso CORS.</li>
 *   <li>Os downloads ({@code /downloads/**}) vêm de uma pasta do disco ({@code MISSAOMARTE_DOWNLOADS}). Na imagem,
 *       o GitHub Actions coloca lá o APK e o jogo desktop já apontando para o servidor publicado. Ficam fora do jar
 *       para que ele não cresça, e os testes usam uma pasta temporária.</li>
 * </ul>
 */
@Configuration
public class ConfiguracaoDaPagina implements WebMvcConfigurer {

    private final String pastaDosDownloads;

    public ConfiguracaoDaPagina(@Value("${missaomarte.downloads}") String pastaDosDownloads) {
        // o Spring só trata como pasta um endereço "file:" que termina em barra
        String uri = Path.of(pastaDosDownloads).toAbsolutePath().normalize().toUri().toString();
        this.pastaDosDownloads = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/downloads/**").addResourceLocations(pastaDosDownloads);
    }
}
