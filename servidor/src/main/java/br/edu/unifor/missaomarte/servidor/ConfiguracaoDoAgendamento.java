package br.edu.unifor.missaomarte.servidor;

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

    private final Agendador agendador;

    public ConfiguracaoDoAgendamento(Agendador agendador) {
        this.agendador = agendador;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperar() {
        agendador.verificar();
    }

    @Scheduled(fixedDelay = 500, initialDelay = 500)
    public void agendar() {
        agendador.verificar();
    }
}
