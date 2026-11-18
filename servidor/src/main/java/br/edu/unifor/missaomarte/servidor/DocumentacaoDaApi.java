package br.edu.unifor.missaomarte.servidor;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cabeçalho da documentação OpenAPI ({@code /v3/api-docs} e {@code /swagger-ui.html}): título, versão, endereço de
 * desenvolvimento e o esquema "Bearer" do token, que faz o Swagger mostrar o botão "Authorize". As rotas públicas
 * (cadastro, login, ranking) continuam funcionando sem token.
 */
@Configuration
public class DocumentacaoDaApi {

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info()
                        .title("Missão Marte — servidor de ranking")
                        .version("5.0.0")
                        .description("Cadastro e login de jogadores (token JWT), histórico de partidas e ranking "
                                + "global Top 10. O servidor guarda pontuações; não executa o jogo."))
                .servers(List.of(new Server().url("http://localhost:8080").description("desenvolvimento")))
                .components(new Components().addSecuritySchemes("token", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("O token devolvido por POST /api/sessoes")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
