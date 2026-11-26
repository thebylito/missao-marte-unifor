package br.edu.unifor.missaomarte.servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "missaomarte.agendador.habilitado", havingValue = "true", matchIfMissing = true)
public class ConfiguracaoDoAgendamento {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDoAgendamento.class);

    private final Agendador agendador;

    public ConfiguracaoDoAgendamento(Agendador agendador) {
        this.agendador = agendador;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperar() {
        try {
            agendador.verificar();
        } catch (RuntimeException e) {
            LOG.error("falha na recuperação inicial do agendador; a verificação será repetida no próximo ciclo", e);
        }
    }

    @Scheduled(fixedDelay = 500, initialDelay = 500)
    public void agendar() {
        agendador.verificar();
    }
}
