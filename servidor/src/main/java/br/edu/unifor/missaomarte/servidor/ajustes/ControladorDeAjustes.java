package br.edu.unifor.missaomarte.servidor.ajustes;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fase 8 (DA-48): os ajustes que a Pilotagem Livre usa. Público, como o ranking: o convidado também joga. */
@RestController
@SecurityRequirements
public class ControladorDeAjustes {

    private final AjustesDeVoo ajustes;

    public ControladorDeAjustes(AjustesDeVoo ajustes) {
        this.ajustes = ajustes;
    }

    @GetMapping("/api/ajustes")
    @Operation(summary = "Ajustes da Pilotagem",
            description = "Tanque, limites de pouso e asteroides por dificuldade, velocidade dos meteoros e duração "
                    + "dos eventos, lidos das variáveis MISSAOMARTE_AJUSTE_* na subida do servidor. A horda usa os "
                    + "ajustes gravados nela (descricao.ajustes em /api/hordas/atual).")
    public AjustesDeVoo ajustes() {
        return ajustes;
    }
}
