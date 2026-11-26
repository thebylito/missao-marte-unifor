package br.edu.unifor.missaomarte.servidor;

import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeFechamentos;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class Agendador {

    private final Clock relogio;
    private final ServicoDeFechamentos fechamentos;

    public Agendador(Clock relogio, ServicoDeFechamentos fechamentos) {
        this.relogio = relogio;
        this.fechamentos = fechamentos;
    }

    public void verificar() {
        fechamentos.verificar(relogio.instant());
    }
}
