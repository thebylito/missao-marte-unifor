package br.edu.unifor.missaomarte.servidor.sombras;

import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo;
import br.edu.unifor.missaomarte.servidor.evento.ServicoDeEventos;
import br.edu.unifor.missaomarte.servidor.seguranca.Sementes;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras.Piloto;
import br.edu.unifor.missaomarte.servidor.sombras.CanalDeSombras.Sala;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

    static final long ATIVIDADE_SEGUNDOS = 3;
    /** Cobre o perigo mais longo que os ajustes aceitam (DA-48). */
    static final long HISTORICO_SEGUNDOS = ATIVIDADE_SEGUNDOS + (long) AjustesDosEventos.DURACAO_MAXIMA;

    private static final class Transmissao {
        final EventoAoVivo evento;
        final Set<Integer> avisados = new HashSet<>();
        final Set<Long> respostas = ConcurrentHashMap.newKeySet();
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
        transmissoes.values().removeIf(t -> agora.isAfter(t.evento.fimDosResultados()));
        if (pilotos.isEmpty()) {
            return;
        }
        eventos.publicarAutomaticos(agora);
        for (EventoAoVivo evento : eventos.ativos(agora)) {
            Transmissao transmissao = transmissoes.computeIfAbsent(evento.id(), id -> new Transmissao(evento));
            if (agora.isBefore(evento.inicio())) {
                for (Piloto piloto : pilotos) {
                    avisar(transmissao, piloto, agora);
                }
            } else {
                if (!transmissao.iniciou && agora.isBefore(evento.fim())) {
                    iniciar(transmissao, pilotos);
                }
                if (evento.vencedor() != null) {
                    anunciarVip(transmissao, evento.vencedor(), pilotos);
                }
            }
        }
    }

    synchronized void avisar(Piloto piloto, Instant agora) {
        for (Transmissao transmissao : transmissoes.values()) {
            avisar(transmissao, piloto, agora);
        }
    }

    private void avisar(Transmissao transmissao, Piloto piloto, Instant agora) {
        EventoAoVivo evento = transmissao.evento;
        if (!agora.isBefore(evento.aviso()) && agora.isBefore(evento.inicio())
                && transmissao.avisados.add(piloto.numero())) {
            Duration falta = Duration.between(agora, evento.inicio());
            long segundos = falta.toSeconds() + (falta.getNano() == 0 ? 0 : 1);
            var dados = Map.of("tipo", "evento-aviso", "evento", evento.id(), "nome", evento.tipo().name(),
                    "em", segundos);
            CanalDeSombras.enviar(piloto, new TextMessage(json.writeValueAsString(dados)));
        }
    }

    private void iniciar(Transmissao transmissao, Collection<Piloto> pilotos) {
        transmissao.iniciou = true;
        EventoAoVivo evento = transmissao.evento;
        Map<Sala, TextMessage> mensagens = new HashMap<>();
        for (Piloto piloto : pilotos) {
            if (piloto.sessao().isOpen() && !piloto.conectadoEm().isAfter(evento.inicio())) {
                if (!evento.tipo().perigo() || piloto.estavaAtivoEm(evento.inicio())) {
                    transmissao.participantes.add(piloto.numero());
                }
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
            var ponto = sala.pontosVip().get(indiceVip(evento, sala));
            var vip = mensagem.putObject("vip");
            vip.put("x", ponto.x());
            vip.put("superficie", ponto.superficie());
        }
        return new TextMessage(json.writeValueAsString(mensagem));
    }

    private static int indiceVip(EventoAoVivo evento, Sala sala) {
        int quantidade = sala.pontosVip().size();
        if (evento.sementeVip() == null) {
            return new SplittableRandom(evento.semente() ^ sala.nome().hashCode()).nextInt(quantidade);
        }
        byte[] chave = Sementes.deHex(evento.sementeVip());
        long sorteio = Sementes.comoLong(Sementes.derivar(chave, "sala:" + sala.nome()));
        return (int) Long.remainderUnsigned(sorteio, quantidade);
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
        EventoAoVivo evento = transmissao.evento;
        if (tipo.equals("vip")) {
            if (!evento.podePegarVip(agora) || transmissao.vipAnunciado.get() || !liberarEvento(piloto, agora)) {
                return;
            }
            synchronized (transmissao) {
                if (!transmissao.vipAnunciado.get()) {
                    eventos.pegarVip(evento, piloto.jogadorId(), agora)
                            .ifPresent(nome -> anunciarVip(transmissao, nome, pilotos));
                }
            }
        } else {
            JsonNode sobreviveu = mensagem.path("sobreviveu");
            if (evento.podeReceberResultado(agora) && sobreviveu.isBoolean()
                    && !transmissao.respostas.contains(piloto.jogadorId()) && liberarEvento(piloto, agora)
                    && transmissao.respostas.add(piloto.jogadorId()) && sobreviveu.asBoolean()) {
                eventos.sobreviver(evento, piloto.jogadorId(), agora);
            }
        }
    }

    private static boolean liberarEvento(Piloto piloto, Instant agora) {
        return piloto.liberarRepasse("evento", agora, CanalDeSombras.INTERVALO_MINIMO_ENTRE_EVENTOS_MS);
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
