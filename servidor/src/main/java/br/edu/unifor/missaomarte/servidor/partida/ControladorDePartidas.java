package br.edu.unifor.missaomarte.servidor.partida;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partidas do jogador autenticado: registrar ({@code POST}) e o próprio histórico ({@code GET /minhas}). Fase 8: o
 * bilhete do começo da partida ({@code POST /inicio}), sem o qual a vitória não rende moedas.
 */
@RestController
@RequestMapping("/api/partidas")
public class ControladorDePartidas {

    private static final String EXEMPLO_HORDA_ENCERRADA = """
            {"status":409,"title":"Conflict","detail":"a horda está encerrada","motivo":"HORDA_ENCERRADA"}
            """;

    private final ServicoDePartidas servico;
    private final ServicoDeBilhetes bilhetes;

    public ControladorDePartidas(ServicoDePartidas servico, ServicoDeBilhetes bilhetes) {
        this.servico = servico;
        this.bilhetes = bilhetes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Partida registrada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Dados ou bilhete inválidos", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "HORDA_ENCERRADA", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = EXEMPLO_HORDA_ENCERRADA)))
    public ResponseEntity<PartidaRegistrada> registrar(@AuthenticationPrincipal Jwt token,
                                                       @Valid @RequestBody RegistroDePartida dados) {
        PartidaRegistrada salva = servico.registrar(ServicoDeTokens.idDoJogador(token), dados);
        return ResponseEntity.created(URI.create("/api/partidas/" + salva.id())).body(salva);
    }

    /** Fase 8: o bilhete da partida que vai começar. Pedir outro anula o anterior. */
    @PostMapping("/inicio")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Bilhete emitido", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Dados da partida inválidos", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "HORDA_ENCERRADA", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = EXEMPLO_HORDA_ENCERRADA)))
    public BilheteResposta iniciar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody InicioDePartida pedido) {
        return bilhetes.emitir(ServicoDeTokens.idDoJogador(token), pedido);
    }

    @GetMapping("/minhas")
    public List<PartidaResposta> minhas(@AuthenticationPrincipal Jwt token) {
        return servico.historico(ServicoDeTokens.idDoJogador(token));
    }
}
