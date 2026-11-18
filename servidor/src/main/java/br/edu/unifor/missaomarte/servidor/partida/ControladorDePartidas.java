package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Partidas do jogador autenticado: registrar ({@code POST}) e o próprio histórico ({@code GET /minhas}). */
@RestController
@RequestMapping("/api/partidas")
public class ControladorDePartidas {

    private final ServicoDePartidas servico;

    public ControladorDePartidas(ServicoDePartidas servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PartidaResposta> registrar(@AuthenticationPrincipal Jwt token,
                                                     @Valid @RequestBody RegistroDePartida dados) {
        PartidaResposta salva = servico.registrar(ServicoDeTokens.idDoJogador(token), dados);
        return ResponseEntity.created(URI.create("/api/partidas/" + salva.id())).body(salva);
    }

    @GetMapping("/minhas")
    public List<PartidaResposta> minhas(@AuthenticationPrincipal Jwt token) {
        return servico.historico(ServicoDeTokens.idDoJogador(token));
    }
}
