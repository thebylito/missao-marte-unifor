package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partidas do jogador autenticado: registrar ({@code POST}) e o próprio histórico ({@code GET /minhas}). Fase 8: o
 * bilhete do começo da partida ({@code POST /inicio}), sem o qual a vitória não rende moedas.
 */
@RestController
@RequestMapping("/api/partidas")
public class ControladorDePartidas {

    private final ServicoDePartidas servico;
    private final ServicoDeBilhetes bilhetes;

    public ControladorDePartidas(ServicoDePartidas servico, ServicoDeBilhetes bilhetes) {
        this.servico = servico;
        this.bilhetes = bilhetes;
    }

    @PostMapping
    public ResponseEntity<PartidaRegistrada> registrar(@AuthenticationPrincipal Jwt token,
                                                       @Valid @RequestBody RegistroDePartida dados) {
        PartidaRegistrada salva = servico.registrar(ServicoDeTokens.idDoJogador(token), dados);
        return ResponseEntity.created(URI.create("/api/partidas/" + salva.id())).body(salva);
    }

    /** Fase 8: o bilhete da partida que vai começar. Pedir outro anula o anterior. */
    @PostMapping("/inicio")
    public ResponseEntity<BilheteResposta> iniciar(@AuthenticationPrincipal Jwt token,
                                                   @Valid @RequestBody InicioDePartida pedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bilhetes.emitir(ServicoDeTokens.idDoJogador(token), pedido));
    }

    @GetMapping("/minhas")
    public List<PartidaResposta> minhas(@AuthenticationPrincipal Jwt token) {
        return servico.historico(ServicoDeTokens.idDoJogador(token));
    }
}
