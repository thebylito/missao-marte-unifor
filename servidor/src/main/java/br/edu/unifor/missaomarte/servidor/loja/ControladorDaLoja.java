package br.edu.unifor.missaomarte.servidor.loja;

import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import jakarta.validation.Valid;
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

/** Fase 8: o catálogo da loja ({@code GET /api/loja}, público) e a compra de uma skin (autenticada). */
@RestController
@RequestMapping("/api/loja")
public class ControladorDaLoja {

    private final ServicoDaLoja loja;

    public ControladorDaLoja(ServicoDaLoja loja) {
        this.loja = loja;
    }

    @GetMapping
    public List<Skin> catalogo() {
        return loja.catalogo();
    }

    /** 201 com o saldo e as skins; 404 skin desconhecida; 409 já possui ou saldo insuficiente (campo {@code motivo}). */
    @PostMapping("/compras")
    public ResponseEntity<CompraResposta> comprar(@AuthenticationPrincipal Jwt token,
                                                  @Valid @RequestBody PedidoDeSkin pedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loja.comprar(ServicoDeTokens.idDoJogador(token), pedido.skin()));
    }
}
