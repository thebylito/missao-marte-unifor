package br.edu.unifor.missaomarte.servidor.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 401 no mesmo formato de todos os outros erros da API (RFC 9457), em vez da página padrão. Não diz se o token
 * faltou, expirou ou foi adulterado: para quem chama, a ação é a mesma (entrar de novo).
 */
@Component
public class RespostaNaoAutorizado implements AuthenticationEntryPoint {

    static final String CORPO = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Token ausente, inválido ou expirado. Entre novamente.\"}";

    @Override
    public void commence(HttpServletRequest pedido, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write(CORPO);
    }
}
