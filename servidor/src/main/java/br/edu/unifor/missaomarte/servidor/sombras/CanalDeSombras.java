package br.edu.unifor.missaomarte.servidor.sombras;

import br.edu.unifor.missaomarte.pilotagem.Caverna;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Modo shadow (Fase 7): cada piloto conectado manda a posição da própria nave, umas 10 vezes por segundo, e recebe
 * as dos outros, que o jogo desenha como "sombras" translúcidas. Não há colisão nem interação: cada um joga a sua
 * partida. O servidor não simula nada, só confere e repassa.
 *
 * <p>Regras:
 * <ul>
 *   <li>só entra quem tem token (o aperto de mão passa pela mesma segurança da API: {@code Authorization: Bearer});
 *   o nome mostrado vem do token, nunca da mensagem;</li>
 *   <li>a mensagem precisa caber em {@value #TAMANHO_MAXIMO} bytes (senão a conexão é fechada) e ter números
 *   dentro da caverna (senão é ignorada);</li>
 *   <li>no máximo uma posição a cada {@value #INTERVALO_MINIMO_MS} ms por piloto (as que chegam antes são
 *   descartadas);</li>
 *   <li>no máximo {@value #PILOTOS_MAXIMOS} pilotos conectados ao mesmo tempo.</li>
 * </ul>
 * Mensagens (JSON): o cliente manda {@code {"tipo":"posicao","x":..,"y":..,"angulo":..,"motor":true}}; os outros
 * recebem {@code {"tipo":"sombra","piloto":7,"nome":"Ana","x":..,"y":..,"angulo":..,"motor":true}} e, quando alguém
 * sai, {@code {"tipo":"saiu","piloto":7}}. Quando a nave de alguém bate, o cliente dele manda
 * {@code {"tipo":"explodiu","x":..,"y":..}} e os outros recebem {@code {"tipo":"explosao","piloto":7,"x":..,"y":..}},
 * com as mesmas regras de tamanho e caverna da posição e um limite próprio (uma por segundo). O número do piloto
 * identifica a conexão (a mesma conta em dois aparelhos são duas sombras).
 */
@Component
public class CanalDeSombras extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CanalDeSombras.class);

    static final int TAMANHO_MAXIMO = 512;
    static final long INTERVALO_MINIMO_MS = 40;
    /**
     * A explosão tem o seu próprio limite: a batida acontece a qualquer instante, então não pode disputar os 40 ms com
     * as posições. Uma por segundo basta, porque a nave leva segundos para renascer.
     */
    static final long INTERVALO_MINIMO_ENTRE_EXPLOSOES_MS = 1000;
    static final int PILOTOS_MAXIMOS = 50;
    /**
     * O tamanho da caverna da pilotagem: posições fora dela são recusadas. Desde a Fase 8 (DA-39) vem do próprio
     * jogo ({@code nucleo}), não de uma cópia dos números; quando houver salas, cada sala terá o tamanho da sua.
     */
    static final double LARGURA_DA_CAVERNA = Caverna.marciana().getLargura();
    static final double ALTURA_DA_CAVERNA = Caverna.marciana().getAltura();
    private static final int ESPERA_DE_ENVIO_MS = 1000;
    private static final int FILA_DE_ENVIO_BYTES = 16 * 1024;

    /** Um piloto conectado; {@code ultimoRepasseMs} guarda, por tipo (sombra, explosão), quando ele foi repassado. */
    private record Piloto(int numero, String nome, WebSocketSession sessao, Map<String, Long> ultimoRepasseMs) {
    }

    private final Map<String, Piloto> pilotos = new ConcurrentHashMap<>();
    private final AtomicInteger proximoNumero = new AtomicInteger(1);
    private final JsonMapper json = JsonMapper.builder().build();
    private final Clock relogio;

    public CanalDeSombras(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sessao) throws IOException {
        if (!(sessao.getPrincipal() instanceof JwtAuthenticationToken token)) {
            sessao.close(CloseStatus.POLICY_VIOLATION);   // não acontece: a segurança barra antes do aperto de mão
            return;
        }
        if (pilotos.size() >= PILOTOS_MAXIMOS) {
            sessao.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        sessao.setTextMessageSizeLimit(TAMANHO_MAXIMO);
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sessao, ESPERA_DE_ENVIO_MS,
                FILA_DE_ENVIO_BYTES);
        String nome = String.valueOf(token.getToken().getClaims().getOrDefault("nome", "Piloto"));
        Piloto piloto = new Piloto(proximoNumero.getAndIncrement(), nome, segura, new HashMap<>());
        pilotos.put(sessao.getId(), piloto);
        LOG.info("sombras: {} entrou (piloto {}); {} conectado(s)", nome, piloto.numero(), pilotos.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession sessao, TextMessage mensagem) throws IOException {
        Piloto piloto = pilotos.get(sessao.getId());
        if (piloto == null) {
            return;
        }
        if (mensagem.getPayloadLength() > TAMANHO_MAXIMO) {
            sessao.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        long agora = relogio.millis();
        ObjectNode repasse = repasseDe(piloto, mensagem.getPayload());
        if (repasse == null) {
            return;   // mensagem inválida: ignorada, sem derrubar a conexão
        }
        String tipo = repasse.get("tipo").asString();
        long intervalo = tipo.equals("explosao") ? INTERVALO_MINIMO_ENTRE_EXPLOSOES_MS : INTERVALO_MINIMO_MS;
        synchronized (piloto) {
            Long ultimo = piloto.ultimoRepasseMs().get(tipo);
            if (ultimo != null && agora - ultimo < intervalo) {
                return;   // rápido demais: descarta (10 posições por segundo bastam para uma sombra suave)
            }
            piloto.ultimoRepasseMs().put(tipo, agora);
            repassar(piloto, json.writeValueAsString(repasse));
        }
    }

    /** O que repassar aos outros, ou null se a mensagem não for uma posição nem uma explosão válida. */
    private ObjectNode repasseDe(Piloto piloto, String texto) {
        JsonNode m;
        try {
            m = json.readTree(texto);
        } catch (JacksonException e) {
            return null;
        }
        if (m == null || !m.isObject()) {
            return null;
        }
        return switch (m.path("tipo").asString()) {
            case "posicao" -> sombraDe(piloto, m);
            case "explodiu" -> explosaoDe(piloto, m);
            default -> null;
        };
    }

    /** A explosão da nave de um piloto, no ponto da caverna em que ela bateu (as vidas e o renascer são do jogo). */
    private ObjectNode explosaoDe(Piloto piloto, JsonNode m) {
        JsonNode x = m.get("x");
        JsonNode y = m.get("y");
        if (!numero(x, 0, LARGURA_DA_CAVERNA) || !numero(y, 0, ALTURA_DA_CAVERNA)) {
            return null;
        }
        ObjectNode explosao = json.createObjectNode();
        explosao.put("tipo", "explosao");
        explosao.put("piloto", piloto.numero());
        explosao.put("x", x.asDouble());
        explosao.put("y", y.asDouble());
        return explosao;
    }

    /** A sombra a repassar, ou null se a posição não for válida dentro da caverna. */
    private ObjectNode sombraDe(Piloto piloto, JsonNode m) {
        JsonNode x = m.get("x");
        JsonNode y = m.get("y");
        JsonNode angulo = m.get("angulo");
        JsonNode motor = m.get("motor");
        if (!numero(x, 0, LARGURA_DA_CAVERNA) || !numero(y, 0, ALTURA_DA_CAVERNA) || !numero(angulo, -180, 180)
                || motor == null || !motor.isBoolean()) {
            return null;
        }
        ObjectNode sombra = json.createObjectNode();
        sombra.put("tipo", "sombra");
        sombra.put("piloto", piloto.numero());
        sombra.put("nome", piloto.nome());
        sombra.put("x", x.asDouble());
        sombra.put("y", y.asDouble());
        sombra.put("angulo", angulo.asDouble());
        sombra.put("motor", motor.asBoolean());
        return sombra;
    }

    private static boolean numero(JsonNode n, double minimo, double maximo) {
        return n != null && n.isNumber() && Double.isFinite(n.asDouble()) && n.asDouble() >= minimo
                && n.asDouble() <= maximo;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sessao, CloseStatus status) throws IOException {
        Piloto piloto = pilotos.remove(sessao.getId());
        if (piloto == null) {
            return;
        }
        ObjectNode saiu = json.createObjectNode();
        saiu.put("tipo", "saiu");
        saiu.put("piloto", piloto.numero());
        repassar(piloto, json.writeValueAsString(saiu));
        LOG.info("sombras: {} saiu (piloto {}); {} conectado(s)", piloto.nome(), piloto.numero(), pilotos.size());
    }

    /** Para todos os outros pilotos. Quem não consegue receber (conexão caindo) é simplesmente pulado. */
    private void repassar(Piloto de, String texto) {
        TextMessage mensagem = new TextMessage(texto);
        for (Piloto outro : pilotos.values()) {
            if (outro.numero() == de.numero() || !outro.sessao().isOpen()) {
                continue;
            }
            try {
                outro.sessao().sendMessage(mensagem);
            } catch (IOException | IllegalStateException e) {
                LOG.debug("sombras: não deu para enviar ao piloto {}", outro.numero());
            }
        }
    }

    /** Quantos pilotos estão conectados (para os testes e o log). */
    int conectados() {
        return pilotos.size();
    }
}
