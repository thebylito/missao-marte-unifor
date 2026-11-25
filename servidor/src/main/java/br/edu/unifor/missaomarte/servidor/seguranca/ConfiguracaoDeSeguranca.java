package br.edu.unifor.missaomarte.servidor.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quem pode o quê. Público: cadastro, login, ranking, saúde, documentação, a página do jogo e os downloads (só
 * leitura). O resto exige o token JWT no cabeçalho
 * {@code Authorization: Bearer ...}, validado aqui (assinatura HS256 e validade). Sem sessão no servidor
 * (stateless): cada pedido traz o próprio token.
 */
@Configuration
public class ConfiguracaoDeSeguranca {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDeSeguranca.class);
    /** HS256 exige pelo menos 256 bits (32 bytes) de segredo. */
    static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    @Bean
    SecurityFilterChain regras(HttpSecurity http, RespostaNaoAutorizado naoAutorizado) throws Exception {
        http.csrf(c -> c.disable())   // API sem cookies: o token vai no cabeçalho, então CSRF não se aplica
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/jogadores", "/api/sessoes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ranking").permitAll()
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/error").permitAll()
                        // publicação: a página do jogo (ranking ao vivo e downloads), na mesma origem da API
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/pagina/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/downloads/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/downloads/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> { }).authenticationEntryPoint(naoAutorizado))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutorizado));
        return http.build();
    }

    @Bean
    SecretKey chaveDosTokens(@Value("${missaomarte.jwt.segredo:}") String segredo) {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[TAMANHO_MINIMO_DO_SEGREDO];
            new SecureRandom().nextBytes(bytes);
            LOG.warn("MISSAOMARTE_JWT_SEGREDO não definido: usando um segredo temporário. "
                    + "Os tokens deixam de valer quando o servidor reinicia.");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < TAMANHO_MINIMO_DO_SEGREDO) {
                throw new IllegalStateException("MISSAOMARTE_JWT_SEGREDO precisa ter pelo menos "
                        + TAMANHO_MINIMO_DO_SEGREDO + " bytes (HS256).");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorDeTokens(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /**
     * Confere a assinatura, a validade (com o mesmo relógio que emite, para os testes controlarem a expiração) e o
     * emissor.
     */
    @Bean
    JwtDecoder decodificadorDeTokens(SecretKey chave, Clock relogio) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validade = new JwtTimestampValidator();
        validade.setClock(relogio);
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validade,
                new JwtIssuerValidator(ServicoDeTokens.EMISSOR)));
        return decodificador;
    }

    /** BCrypt: hash lento e com sal aleatório. A senha em texto puro nunca é gravada nem registrada em log. */
    @Bean
    PasswordEncoder codificadorDeSenhas() {
        return new BCryptPasswordEncoder();
    }
}
