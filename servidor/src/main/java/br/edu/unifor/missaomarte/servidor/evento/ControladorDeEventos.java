package br.edu.unifor.missaomarte.servidor.evento;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ControladorDeEventos {

    private final ServicoDeEventos eventos;
    private final RepositorioDeJogadores jogadores;
    private final Clock relogio;
    private final Set<String> admins;

    public ControladorDeEventos(ServicoDeEventos eventos, RepositorioDeJogadores jogadores, Clock relogio,
                                @Value("${missaomarte.admins:}") String admins) {
        this.eventos = eventos;
        this.jogadores = jogadores;
        this.relogio = relogio;
        this.admins = Arrays.stream(admins.split(",")).map(String::strip).filter(nome -> !nome.isEmpty())
                .map(nome -> nome.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @PostMapping("/api/admin/eventos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiResponse(responseCode = "202", description = "Evento agendado com aviso de 15 segundos")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Jogador sem acesso ao painel")
    public void disparar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody Pedido pedido) {
        long id = ServicoDeTokens.idDoJogador(token);
        boolean admin = jogadores.findById(id)
                .map(jogador -> admins.contains(jogador.getNome().toLowerCase(Locale.ROOT))).orElse(false);
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "painel disponível somente para administradores");
        }
        eventos.solicitar(pedido.tipo(), relogio.instant());
    }

    public record Pedido(@NotNull(message = "informe o tipo do evento") TipoDeEvento tipo) {}
}
