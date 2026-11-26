package br.edu.unifor.missaomarte.servidor;

import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeFechamentos;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class Agendador {

    private final Clock relogio;
    private final ServicoDeFechamentos fechamentos;
    private final CanalDeSombras canal;

    public Agendador(Clock relogio, ServicoDeFechamentos fechamentos, CanalDeSombras canal) {
        this.relogio = relogio;
        this.fechamentos = fechamentos;
        this.canal = canal;
    }

    public void verificar() {
        Instant agora = relogio.instant();
        canal.verificarEventos(agora);
        fechamentos.verificar(agora);
    }
}
