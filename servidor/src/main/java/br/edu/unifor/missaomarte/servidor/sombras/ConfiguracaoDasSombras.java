package br.edu.unifor.missaomarte.servidor.sombras;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * O endereço do modo shadow: {@code ws://<servidor>/ws/sombras} (ou {@code wss://} atrás de um proxy com HTTPS). O
 * aperto de mão é um GET comum, então passa pela segurança da API: sem {@code Authorization: Bearer <token>}
 * válido, a resposta é 401 e a conexão não abre.
 */
@Configuration
@EnableWebSocket
public class ConfiguracaoDasSombras implements WebSocketConfigurer {

    public static final String ENDERECO = "/ws/sombras";

    private final CanalDeSombras canal;

    public ConfiguracaoDasSombras(CanalDeSombras canal) {
        this.canal = canal;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        // os clientes são o próprio jogo (não um navegador): não mandam Origin, então qualquer origem é aceita
        registro.addHandler(canal, ENDERECO).setAllowedOriginPatterns("*");
    }
}
