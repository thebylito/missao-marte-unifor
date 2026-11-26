package br.edu.unifor.missaomarte.servidor.loja;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fase 8: o Hangar do jogador autenticado: saldo, skins e a equipada ({@code GET /api/jogadores/eu/hangar}), e a
 * troca da skin equipada ({@code PUT /api/jogadores/eu/skin}).
 */
@RestController
@RequestMapping("/api/jogadores/eu")
public class ControladorDoHangar {

    private final ServicoDaLoja loja;

    public ControladorDoHangar(ServicoDaLoja loja) {
        this.loja = loja;
    }

    @GetMapping("/hangar")
    public Hangar hangar(@AuthenticationPrincipal Jwt token) {
        return loja.hangar(ServicoDeTokens.idDoJogador(token));
    }

    /** 200 com a skin equipada; 404 skin desconhecida; 409 skin que o jogador não tem ({@code motivo: NAO_POSSUI}). */
    @PutMapping("/skin")
    public SkinEquipada equipar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody PedidoDeSkin pedido) {
        return loja.equipar(ServicoDeTokens.idDoJogador(token), pedido.skin());
    }
}
