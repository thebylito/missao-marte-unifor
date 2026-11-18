package br.edu.unifor.missaomarte.servidor.seguranca;

import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o token do login: assinado (HS256), com o id do jogador no "subject" e prazo de validade. */
@Service
public class ServicoDeTokens {

    public static final String EMISSOR = "missao-marte";

    private final JwtEncoder codificador;
    private final Clock relogio;
    private final Duration validade;

    public ServicoDeTokens(JwtEncoder codificador, Clock relogio,
                           @Value("${missaomarte.jwt.validade:PT2H}") Duration validade) {
        this.codificador = codificador;
        this.relogio = relogio;
        this.validade = validade;
    }

    public Jwt emitir(Jogador jogador) {
        Instant agora = relogio.instant();
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(jogador.getId()))
                .claim("nome", jogador.getNome())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), dados));
    }

    /** O id do jogador autenticado, lido do token já validado pelo Spring Security. */
    public static long idDoJogador(Jwt token) {
        return Long.parseLong(token.getSubject());
    }
}
