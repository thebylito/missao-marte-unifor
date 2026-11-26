package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import java.util.Objects;

/**
 * Fase 8 (DA-48): os números do Modo Pilotagem que o servidor pode ajustar sem lançar um jogo novo. O servidor lê
 * as variáveis de ambiente {@code MISSAOMARTE_AJUSTE_*}, publica o resultado em {@code GET /api/ajustes} (a
 * Pilotagem Livre usa) e grava uma cópia em cada horda sorteada (a horda usa a dela até o fim).
 *
 * <p>Cada valor tem uma faixa: fora dela, o construtor recusa com o nome do campo na mensagem. Assim um erro de
 * digitação na variável impede o servidor de subir, em vez de deixar o jogo impossível.
 */
public record AjustesDeVoo(AjustesDaDificuldade facil, AjustesDaDificuldade medio, AjustesDaDificuldade dificil,
                           AjustesDosEventos eventos) {

    /**
     * Os números da Fase 8. O Difícil não tem tanque menor (DA-34): a viagem mais longa já gasta perto de 100. Os
     * limites de inclinação são os da DA-47 (25°, 20° e 15°).
     */
    public static final AjustesDeVoo PADRAO = new AjustesDeVoo(
            new AjustesDaDificuldade(130, 90, 60, 25, 0.75),
            new AjustesDaDificuldade(100, 75, 50, 20, 1.0),
            new AjustesDaDificuldade(100, 65, 40, 15, 1.3),
            AjustesDosEventos.PADRAO);

    public AjustesDeVoo {
        Objects.requireNonNull(facil, "facil");
        Objects.requireNonNull(medio, "medio");
        Objects.requireNonNull(dificil, "dificil");
        Objects.requireNonNull(eventos, "eventos");
    }

    /** Os ajustes desta dificuldade. */
    public AjustesDaDificuldade para(Dificuldade dificuldade) {
        switch (dificuldade) {
            case FACIL:
                return facil;
            case DIFICIL:
                return dificil;
            default:
                return medio;
        }
    }
}
