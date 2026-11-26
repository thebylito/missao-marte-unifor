package br.edu.unifor.missaomarte.servidor.horda;

import static br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas.FORTALEZA;

import br.edu.unifor.missaomarte.servidor.partida.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.Modificador;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirements
public class ControladorDeHordas {

    private static final DateTimeFormatter HORA_LOCAL = DateTimeFormatter.ofPattern("HH:mm").withZone(FORTALEZA);

    private final ServicoDeHordas hordas;
    private final Clock relogio;

    public ControladorDeHordas(ServicoDeHordas hordas, Clock relogio) {
        this.hordas = hordas;
        this.relogio = relogio;
    }

    @GetMapping("/api/hordas/atual")
    public HordaAtual atual() {
        Instant agora = relogio.instant();
        Horda horda = hordas.atual(agora);
        DescricaoDaHorda d = horda.descricao();
        long segundos = Duration.between(agora, horda.fim()).toSeconds();
        return new HordaAtual(horda.numero(), horda.inicio(), horda.fim(), segundos, segundos,
                HORA_LOCAL.format(horda.fim()), new CavernaResumida(d.caverna().id(), d.caverna().nome()),
                Dificuldade.valueOf(d.dificuldade().name()), d.modificadores(), d);
    }

    public record CavernaResumida(String id, String nome) {}

    public record HordaAtual(long numero, Instant inicio, Instant fim, long segundosParaAcabar,
                             long segundosParaProxima, String proximaAs, CavernaResumida caverna,
                             Dificuldade dificuldade, List<Modificador> modificadores, DescricaoDaHorda descricao) {}
}
