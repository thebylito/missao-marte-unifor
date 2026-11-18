package br.edu.unifor.missaomarte.servidor.jogador;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro ({@code POST /api/jogadores}, público) e perfil do jogador autenticado ({@code GET /api/jogadores/eu}). */
@RestController
@RequestMapping("/api/jogadores")
public class ControladorDeJogadores {

    private final ServicoDeJogadores servico;

    public ControladorDeJogadores(ServicoDeJogadores servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<JogadorResposta> cadastrar(@Valid @RequestBody CadastroDeJogador pedido) {
        JogadorResposta criado = servico.cadastrar(pedido);
        return ResponseEntity.created(URI.create("/api/jogadores/" + criado.id())).body(criado);
    }

    @GetMapping("/eu")
    public JogadorResposta eu(@AuthenticationPrincipal Jwt token) {
        return servico.perfil(ServicoDeTokens.idDoJogador(token));
    }
}
