package br.edu.unifor.missaomarte.servidor;

import br.edu.unifor.missaomarte.servidor.banco.GuardaDoBanco;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Servidor de ranking do Missão Marte (Fase 5). Guarda jogadores e partidas e calcula o ranking global; não
 * executa a lógica do jogo: confia na pontuação que o cliente envia (limitação documentada no tutorial).
 */
@SpringBootApplication
public class ServidorApplication {

    public static void main(String[] args) {
        GuardaDoBanco.verificar(System.getenv());   // Fase 8: na imagem, só com o PostgreSQL configurado
        SpringApplication.run(ServidorApplication.class, args);
    }

    /** Relógio injetado: os testes controlam datas de cadastro, de partida e de expiração do token. */
    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
