package br.edu.unifor.missaomarte.servidor.sombras;

import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.horda.PassageiroNaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.PontoDePouso;
import br.edu.unifor.missaomarte.servidor.horda.CatalogoDeCavernas;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.loja.Skin;
import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
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
 *   o nome mostrado vem do token, nunca da mensagem; a skin (Fase 8) vem do perfil do jogador, lida na conexão,
 *   também nunca da mensagem;</li>
 *   <li>a mensagem precisa caber em {@value #TAMANHO_MAXIMO} bytes (senão a conexão é fechada) e ter números
 *   dentro da caverna (senão é ignorada);</li>
 *   <li>no máximo uma posição a cada {@value #INTERVALO_MINIMO_MS} ms por piloto (as que chegam antes são
 *   descartadas);</li>
 *   <li>no máximo {@value #PILOTOS_MAXIMOS} pilotos conectados ao mesmo tempo.</li>
 * </ul>
 * Mensagens (JSON): o cliente manda {@code {"tipo":"posicao","x":..,"y":..,"angulo":..,"motor":true}}; os outros
 * recebem {@code {"tipo":"sombra","piloto":7,"nome":"Ana","skin":"guara","x":..,"y":..,"angulo":..,"motor":true}}
 * (os clientes 7.x ignoram o campo {@code skin}) e, quando alguém
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
     * A sala livre usa os limites da Marciana do nucleo (DA-39). Cada sala de horda usa os dados publicados.
     */
    private static final Caverna MARCIANA = Caverna.marciana();
    static final double LARGURA_DA_CAVERNA = MARCIANA.getLargura();
    static final double ALTURA_DA_CAVERNA = MARCIANA.getAltura();
    private static final int ESPERA_DE_ENVIO_MS = 1000;
    private static final int FILA_DE_ENVIO_BYTES = 16 * 1024;

    /** Um piloto conectado; {@code ultimoRepasseMs} guarda, por tipo (sombra, explosão), quando ele foi repassado. */
    record Piloto(int numero, long jogadorId, String nome, String skin, WebSocketSession sessao,
                  Sala sala, Instant conectadoEm, Map<String, Long> ultimoRepasseMs) {}

    record Sala(String nome, double largura, double altura, List<PontoDePouso> pontosVip, Instant fechaEm) {
        boolean fechadaEm(Instant agora) {
            return fechaEm != null && !agora.isBefore(fechaEm);
        }
    }

    private final Map<String, Piloto> pilotos = new ConcurrentHashMap<>();
    private final AtomicInteger proximoNumero = new AtomicInteger(1);
    private final JsonMapper json = JsonMapper.builder().build();
    private final Clock relogio;
    private final RepositorioDeJogadores jogadores;
    private final ServicoDeHordas hordas;
    private final EventosNoCanal eventos;
    private final Sala livre;

    public CanalDeSombras(Clock relogio, RepositorioDeJogadores jogadores, ServicoDeHordas hordas,
                          CatalogoDeCavernas catalogo, EventosNoCanal eventos) {
        this.relogio = relogio;
        this.jogadores = jogadores;
        this.hordas = hordas;
        this.eventos = eventos;
        var ocupados = MARCIANA.getPlataformas().stream()
                .map(p -> new PontoDePouso(p.getX(), p.getSuperficie())).collect(Collectors.toSet());
        var pontosLivres = catalogo.porId("marciana").orElseThrow().pontosDePouso().stream()
                .filter(p -> !ocupados.contains(p)).toList();
        livre = new Sala("livre", LARGURA_DA_CAVERNA, ALTURA_DA_CAVERNA, pontosLivres, null);
    }

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession sessao) throws IOException {
        if (!(sessao.getPrincipal() instanceof JwtAuthenticationToken token)) {
            sessao.close(CloseStatus.POLICY_VIOLATION);   // não acontece: a segurança barra antes do aperto de mão
            return;
        }
        if (pilotos.size() >= PILOTOS_MAXIMOS) {
            sessao.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        Instant agora = relogio.instant();
        Sala sala = salaPedida(sessao.getUri(), agora);
        if (sala == null) {
            sessao.close(CloseStatus.POLICY_VIOLATION.withReason("sala desconhecida ou encerrada"));
            return;
        }
        sessao.setTextMessageSizeLimit(TAMANHO_MAXIMO);
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sessao, ESPERA_DE_ENVIO_MS,
                FILA_DE_ENVIO_BYTES);
        String nome = String.valueOf(token.getToken().getClaims().getOrDefault("nome", "Piloto"));
        // a skin equipada no momento da conexão; uma troca no Hangar vale na próxima conexão
        long jogadorId = ServicoDeTokens.idDoJogador(token.getToken());
        String skin = jogadores.skinDe(jogadorId).orElse(Skin.PADRAO);
        Piloto piloto = new Piloto(proximoNumero.getAndIncrement(), jogadorId, nome, skin, segura, sala,
                relogio.instant(), new HashMap<>());
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
        Instant instante = relogio.instant();
        if (piloto.sala().fechadaEm(instante)) {
            sessao.close(CloseStatus.POLICY_VIOLATION.withReason("sala encerrada"));
            return;
        }
        JsonNode pedido = lerObjeto(mensagem.getPayload());
        if (pedido == null) {
            return;
        }
        String tipoPedido = pedido.path("tipo").asString();
        if (tipoPedido.equals("evento-resultado") || tipoPedido.equals("vip")) {
            eventos.receber(piloto, tipoPedido, pedido, instante, pilotos.values());
            return;
        }
        ObjectNode repasse = switch (tipoPedido) {
            case "posicao" -> sombraDe(piloto, pedido);
            case "explodiu" -> explosaoDe(piloto, pedido);
            default -> null;
        };
        if (repasse == null) {
            return;   // mensagem inválida: ignorada, sem derrubar a conexão
        }
        long agora = instante.toEpochMilli();
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

    private JsonNode lerObjeto(String texto) {
        try {
            JsonNode m = json.readTree(texto);
            return m != null && m.isObject() ? m : null;
        } catch (JacksonException e) {
            return null;
        }
    }

    private Sala salaPedida(URI uri, Instant agora) {
        List<String> salas = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("sala");
        if (salas == null) {
            return livre;
        }
        if (salas.size() != 1 || salas.get(0) == null) {
            return null;
        }
        String sala = salas.get(0);
        if (sala.equals("livre")) {
            return livre;
        }
        if (!sala.startsWith("horda-")) {
            return null;
        }
        try {
            long numero = Long.parseLong(sala.substring("horda-".length()));
            if (!sala.equals("horda-" + numero)) {
                return null;
            }
            return hordas.sala(numero, agora).map(CanalDeSombras::salaDaHorda).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Sala salaDaHorda(Horda horda) {
        var descricao = horda.descricao();
        var caverna = descricao.caverna();
        var pontos = caverna.pontosDePouso();
        var ocupados = descricao.passageiros().stream().map(PassageiroNaHorda::ponto).collect(Collectors.toSet());
        var livres = IntStream.range(0, pontos.size()).filter(i -> !ocupados.contains(i)).mapToObj(pontos::get).toList();
        return new Sala("horda-" + horda.numero(), caverna.largura(), caverna.altura(),
                livres.isEmpty() ? pontos : livres, horda.fim().plus(ServicoDeHordas.TOLERANCIA));
    }

    public void verificarEventos(Instant agora) {
        var ativos = pilotos.values().stream().filter(p -> !p.sala().fechadaEm(agora)).toList();
        eventos.verificar(agora, ativos);
    }

    /** A explosão da nave de um piloto, no ponto da caverna em que ela bateu (as vidas e o renascer são do jogo). */
    private ObjectNode explosaoDe(Piloto piloto, JsonNode m) {
        JsonNode x = m.get("x");
        JsonNode y = m.get("y");
        if (!numero(x, 0, piloto.sala().largura()) || !numero(y, 0, piloto.sala().altura())) {
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
        if (!numero(x, 0, piloto.sala().largura()) || !numero(y, 0, piloto.sala().altura())
                || !numero(angulo, -180, 180) || motor == null || !motor.isBoolean()) {
            return null;
        }
        ObjectNode sombra = json.createObjectNode();
        sombra.put("tipo", "sombra");
        sombra.put("piloto", piloto.numero());
        sombra.put("nome", piloto.nome());
        sombra.put("skin", piloto.skin());
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

    /** Para os outros pilotos da mesma sala. */
    private void repassar(Piloto de, String texto) {
        TextMessage mensagem = new TextMessage(texto);
        for (Piloto outro : pilotos.values()) {
            if (outro.numero() != de.numero() && outro.sala().nome().equals(de.sala().nome())) {
                enviar(outro, mensagem);
            }
        }
    }

    static void enviar(Piloto piloto, TextMessage mensagem) {
        if (!piloto.sessao().isOpen()) {
            return;
        }
        try {
            piloto.sessao().sendMessage(mensagem);
        } catch (IOException | IllegalStateException e) {
            LOG.debug("sombras: não deu para enviar ao piloto {}", piloto.numero());
        }
    }

    /** Quantos pilotos estão conectados (para os testes e o log). */
    int conectados() {
        return pilotos.size();
    }
}
