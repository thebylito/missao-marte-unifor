package br.edu.unifor.missaomarte.servidor.sombras;

import br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo;
import br.edu.unifor.missaomarte.servidor.evento.ServicoDeEventos;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras.Piloto;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras.Sala;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
class EventosNoCanal {

    private static final class Transmissao {
        final EventoAoVivo evento;
        final Set<Integer> participantes = ConcurrentHashMap.newKeySet();
        final AtomicBoolean vipAnunciado = new AtomicBoolean();
        boolean iniciou;

        Transmissao(EventoAoVivo evento) {
            this.evento = evento;
        }
    }

    private final ServicoDeEventos eventos;
    private final JsonMapper json;
    private final Map<String, Transmissao> transmissoes = new ConcurrentHashMap<>();

    EventosNoCanal(ServicoDeEventos eventos, JsonMapper json) {
        this.eventos = eventos;
        this.json = json;
    }

    synchronized void verificar(Instant agora, Collection<Piloto> pilotos) {
        transmissoes.values().removeIf(t -> agora.isAfter(t.evento.fim()
                .plusSeconds(EventoAoVivo.FOLGA_RESULTADO_SEGUNDOS)));
        if (pilotos.isEmpty()) {
            return;
        }
        eventos.publicarAutomaticos(agora);
        for (EventoAoVivo evento : eventos.ativos(agora)) {
            if (agora.isBefore(evento.inicio())) {
                if (!transmissoes.containsKey(evento.id())) {
                    transmissoes.put(evento.id(), new Transmissao(evento));
                    Duration falta = Duration.between(agora, evento.inicio());
                    long segundos = falta.toSeconds() + (falta.getNano() == 0 ? 0 : 1);
                    enviarATodos(pilotos, Map.of("tipo", "evento-aviso", "evento", evento.id(),
                            "nome", evento.tipo().name(), "em", segundos));
                }
            } else {
                Transmissao transmissao = transmissoes.computeIfAbsent(evento.id(), id -> new Transmissao(evento));
                if (!transmissao.iniciou && agora.isBefore(evento.fim())) {
                    iniciar(transmissao, pilotos);
                }
                if (evento.vencedor() != null) {
                    anunciarVip(transmissao, evento.vencedor(), pilotos);
                }
            }
        }
    }

    private void iniciar(Transmissao transmissao, Collection<Piloto> pilotos) {
        transmissao.iniciou = true;
        EventoAoVivo evento = transmissao.evento;
        Map<Sala, TextMessage> mensagens = new HashMap<>();
        for (Piloto piloto : pilotos) {
            if (piloto.sessao().isOpen() && !piloto.conectadoEm().isAfter(evento.inicio())) {
                transmissao.participantes.add(piloto.numero());
                CanalDeSombras.enviar(piloto, mensagens.computeIfAbsent(piloto.sala(), sala -> inicio(evento, sala)));
            }
        }
    }

    private TextMessage inicio(EventoAoVivo evento, Sala sala) {
        var mensagem = json.createObjectNode();
        mensagem.put("tipo", "evento-inicio");
        mensagem.put("evento", evento.id());
        mensagem.put("nome", evento.tipo().name());
        mensagem.put("semente", evento.semente());
        mensagem.put("duracao", evento.duracao());
        if (!evento.tipo().perigo()) {
            var sorteio = new SplittableRandom(evento.semente() ^ sala.nome().hashCode());
            var ponto = sala.pontosVip().get(sorteio.nextInt(sala.pontosVip().size()));
            var vip = mensagem.putObject("vip");
            vip.put("x", ponto.x());
            vip.put("superficie", ponto.superficie());
        }
        return new TextMessage(json.writeValueAsString(mensagem));
    }

    void receber(Piloto piloto, String tipo, JsonNode mensagem, Instant agora, Collection<Piloto> pilotos) {
        JsonNode id = mensagem.path("evento");
        if (!id.isString()) {
            return;
        }
        Transmissao transmissao = transmissoes.get(id.asString());
        if (transmissao == null || !transmissao.participantes.contains(piloto.numero())) {
            return;
        }
        if (tipo.equals("vip")) {
            eventos.pegarVip(transmissao.evento, piloto.jogadorId(), agora)
                    .ifPresent(nome -> anunciarVip(transmissao, nome, pilotos));
        } else if (mensagem.path("sobreviveu").isBoolean() && mensagem.path("sobreviveu").asBoolean()) {
            eventos.sobreviver(transmissao.evento, piloto.jogadorId(), agora);
        }
    }

    void registrarExplosao(Piloto piloto, Instant agora) {
        for (Transmissao transmissao : transmissoes.values()) {
            EventoAoVivo evento = transmissao.evento;
            if (evento.tipo().perigo() && !agora.isBefore(evento.inicio()) && agora.isBefore(evento.fim())) {
                transmissao.participantes.remove(piloto.numero());
            }
        }
    }

    private void anunciarVip(Transmissao transmissao, String nome, Collection<Piloto> pilotos) {
        if (transmissao.vipAnunciado.compareAndSet(false, true)) {
            enviarATodos(pilotos, Map.of("tipo", "vip-pego", "evento", transmissao.evento.id(), "nome", nome));
        }
    }

    private void enviarATodos(Collection<Piloto> pilotos, Map<String, ?> dados) {
        var mensagem = new TextMessage(json.writeValueAsString(dados));
        for (Piloto piloto : pilotos) {
            CanalDeSombras.enviar(piloto, mensagem);
        }
    }
}
