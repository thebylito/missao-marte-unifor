package br.edu.unifor.missaomarte.servidor;

import br.edu.unifor.missaomarte.servidor.ranking.ServicoDeFechamentos;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Agendador {

    private static final Logger LOG = LoggerFactory.getLogger(Agendador.class);

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
        try {
            canal.verificarEventos(agora);
        } catch (RuntimeException e) {
            LOG.error("falha ao verificar os eventos ao vivo", e);
        }
        try {
            fechamentos.verificar(agora);
        } catch (RuntimeException e) {
            LOG.error("falha ao verificar os fechamentos", e);
        }
    }
}
