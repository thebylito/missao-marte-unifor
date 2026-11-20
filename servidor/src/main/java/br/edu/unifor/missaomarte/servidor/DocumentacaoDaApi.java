package br.edu.unifor.missaomarte.servidor;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cabeçalho da documentação OpenAPI ({@code /v3/api-docs} e {@code /swagger-ui.html}): título, versão, endereço do
 * servidor e o esquema "Bearer" do token, que faz o Swagger mostrar o botão "Authorize". As rotas públicas
 * (cadastro, login, ranking) continuam funcionando sem token.
 *
 * <p>O endereço é o que o "Try it out" do Swagger usa. Em desenvolvimento, {@code http://localhost:8080}; publicado,
 * o endereço público ({@code MISSAOMARTE_URL_PUBLICA}, por exemplo {@code https://ranking.exemplo.com}). Sem isso, o
 * Swagger de um servidor publicado mandaria os pedidos para o localhost de quem abriu a página (visto na Fase 6).
 */
@Configuration
public class DocumentacaoDaApi {

    static final String ENDERECO_DE_DESENVOLVIMENTO = "http://localhost:8080";

    @Bean
    OpenAPI documentacao(@Value("${missaomarte.url-publica:" + ENDERECO_DE_DESENVOLVIMENTO + "}") String urlPublica) {
        String endereco = urlPublica == null || urlPublica.isBlank() ? ENDERECO_DE_DESENVOLVIMENTO : urlPublica.trim();
        String descricao = endereco.equals(ENDERECO_DE_DESENVOLVIMENTO) ? "desenvolvimento" : "publicado";
        return new OpenAPI()
                .info(new Info()
                        .title("Missão Marte — servidor de ranking")
                        .version("5.0.0")
                        .description("Cadastro e login de jogadores (token JWT), histórico de partidas e ranking "
                                + "global Top 10. O servidor guarda pontuações; não executa o jogo."))
                .servers(List.of(new Server().url(endereco).description(descricao)))
                .components(new Components().addSecuritySchemes("token", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("O token devolvido por POST /api/sessoes")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
