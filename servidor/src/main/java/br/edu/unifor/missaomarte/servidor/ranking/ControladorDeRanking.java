package br.edu.unifor.missaomarte.servidor.ranking;

import br.edu.unifor.missaomarte.servidor.partida.Modo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ranking global, público: {@code GET /api/ranking}, ou {@code GET /api/ranking?modo=PILOTAGEM} (Fase 7). */
@RestController
@RequestMapping("/api/ranking")
public class ControladorDeRanking {

    private final ServicoDeRanking servico;

    public ControladorDeRanking(ServicoDeRanking servico) {
        this.servico = servico;
    }

    /** Sem o parâmetro, o modo clássico: os clientes das Fases 5 e 6 continuam recebendo o mesmo ranking. */
    @GetMapping
    public List<PosicaoNoRanking> top10(@RequestParam(defaultValue = "CLASSICO") Modo modo) {
        return servico.top10(modo);
    }
}
