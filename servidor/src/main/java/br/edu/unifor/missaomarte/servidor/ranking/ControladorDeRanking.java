package br.edu.unifor.missaomarte.servidor.ranking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ranking global, público: {@code GET /api/ranking}. */
@RestController
@RequestMapping("/api/ranking")
public class ControladorDeRanking {

    private final ServicoDeRanking servico;

    public ControladorDeRanking(ServicoDeRanking servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<PosicaoNoRanking> top10() {
        return servico.top10();
    }
}
