package br.edu.unifor.missaomarte.servidor.jogador;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login: {@code POST /api/sessoes} com nome e senha devolve o token JWT. */
@RestController
@RequestMapping("/api/sessoes")
public class ControladorDeSessoes {

    private final ServicoDeJogadores servico;

    public ControladorDeSessoes(ServicoDeJogadores servico) {
        this.servico = servico;
    }

    @PostMapping
    public TokenResposta entrar(@Valid @RequestBody Credenciais credenciais) {
        return servico.entrar(credenciais);
    }
}
