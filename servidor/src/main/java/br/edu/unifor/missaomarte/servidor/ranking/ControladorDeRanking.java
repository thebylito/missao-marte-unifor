package br.edu.unifor.missaomarte.servidor.ranking;

import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.partida.Modo;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ranking global, público: {@code GET /api/ranking}, ou {@code GET /api/ranking?modo=PILOTAGEM} (Fase 7). */
@RestController
@SecurityRequirements
@RequestMapping("/api/ranking")
public class ControladorDeRanking {

    private final ServicoDeRanking servico;

    private final Clock relogio;
    private final PlacarSemanal placar;

    public ControladorDeRanking(ServicoDeRanking servico, Clock relogio, PlacarSemanal placar) {
        this.relogio = relogio;
        this.servico = servico;
        this.placar = placar;
    }

    /** Sem o parâmetro, o modo clássico: os clientes das Fases 5 e 6 continuam recebendo o mesmo ranking. */
    @GetMapping
    public List<PosicaoNoRanking> top10(@RequestParam(defaultValue = "CLASSICO") Modo modo,
                                        @RequestParam(required = false) Long horda) {
        Long numero = modo == Modo.HORDA && horda == null ? Long.valueOf(ServicoDeHordas.numeroEm(relogio.instant()))
                : horda;
        return servico.top10(modo, numero);
    }
    @GetMapping("/semanal")
    public PlacarSemanal.Placar semanal() {
        return placar.placarDe(PlacarSemanal.semanaDe(relogio.instant()));
    }
}
