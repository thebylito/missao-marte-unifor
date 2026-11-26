package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.pilotagem.EventoAplicado;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Traduz o protocolo da sala para a simulação da partida e devolve os resultados ao servidor. */
final class EventosAoVivo {
    private final PartidaDePilotagem partida;
    private final Consumer<String> enviar;
    private final Set<String> iniciados = new HashSet<>();
    private final Map<EventoAplicado, Deque<String>> ids = new HashMap<>();
    private final Map<TipoDeEvento, Integer> proximoFim = new EnumMap<>(TipoDeEvento.class);
    private String idAviso;
    private String texto;
    private float restante;
    private boolean alerta;
    private String vip;
    private boolean vipEnviado;

    EventosAoVivo(PartidaDePilotagem partida, Consumer<String> enviar) {
        this.partida = partida;
        this.enviar = enviar;
    }

    void receber(String mensagem, JsonValue m) {
        if (partida.getEstado() != Partida.Estado.EM_ANDAMENTO) return;
        switch (mensagem) {
            case "evento-aviso" -> avisar(m);
            case "evento-inicio" -> iniciar(m);
            case "vip-pego" -> {
                if (m.getString("evento").equals(vip)) {
                    String vencedor = m.getString("nome");
                    partida.encerrarVip();
                    vip = null;
                    mostrar(vencedor + " pegou o VIP", 5);
                }
            }
        }
    }

    private void avisar(JsonValue m) {
        String id = m.getString("evento");
        TipoDeEvento tipo = TipoDeEvento.valueOf(m.getString("nome"));
        int segundos = m.getInt("em");
        if (segundos <= 0 || id.equals(idAviso) || iniciados.contains(id)) return;
        mostrar(TextosDaInterface.nomeDoEvento(tipo), segundos);
        idAviso = id;
        alerta = true;
    }

    private void iniciar(JsonValue m) {
        String id = m.getString("evento");
        TipoDeEvento tipo = TipoDeEvento.valueOf(m.getString("nome"));
        long semente = m.getLong("semente");
        if (id.isBlank() || iniciados.contains(id)) return;
        if (tipo.perigo()) {
            long passo = Math.round(partida.getTempo() / PartidaDePilotagem.PASSO) + 1;
            EventoAplicado aplicacao = new EventoAplicado(tipo, semente, passo);
            ids.computeIfAbsent(aplicacao, k -> new ArrayDeque<>()).addLast(id);
            partida.iniciarEvento(tipo, semente);
        } else {
            JsonValue local = m.require("vip");
            double x = local.getDouble("x");
            double superficie = local.getDouble("superficie");
            if (!Double.isFinite(x) || !Double.isFinite(superficie)) return;
            partida.iniciarVip(x, superficie);
            vip = id;
            vipEnviado = false;
        }
        iniciados.add(id);
        mostrar(TextosDaInterface.nomeDoEvento(tipo), (float) tipo.duracaoSegundos());
    }

    void tratar(List<EventoDeVoo> ocorrencias) {
        for (EventoDeVoo e : ocorrencias) {
            if (e.tipo() == EventoDeVoo.Tipo.PERIGO_TERMINOU) {
                concluir(TipoDeEvento.valueOf(e.mensagem()), e.valor() == 1);
            } else if (e.tipo() == EventoDeVoo.Tipo.VIP_PEGO && vip != null && !vipEnviado) {
                vipEnviado = true;
                enviar.accept(resposta("vip", vip).toJson(JsonWriter.OutputType.json));
            }
        }
    }

    /** Perigos do mesmo tipo duram o mesmo número de passos e terminam na ordem do histórico,
     * inclusive quando um perigo do roteiro coincide com o do servidor. Só o ao vivo tem id. */
    private void concluir(TipoDeEvento tipo, boolean sobreviveu) {
        List<EventoAplicado> historico = partida.eventosAplicados();
        for (int i = proximoFim.getOrDefault(tipo, 0); i < historico.size(); i++) {
            EventoAplicado aplicacao = historico.get(i);
            if (aplicacao.tipo() != tipo) continue;
            proximoFim.put(tipo, i + 1);
            Deque<String> destePasso = ids.get(aplicacao);
            if (destePasso != null && ocorrenciasRestantes(historico, i, aplicacao) <= destePasso.size()) {
                JsonValue resultado = resposta("evento-resultado", destePasso.removeFirst());
                resultado.addChild("sobreviveu", new JsonValue(sobreviveu));
                enviar.accept(resultado.toJson(JsonWriter.OutputType.json));
                if (destePasso.isEmpty()) ids.remove(aplicacao);
            }
            return;
        }
    }

    // Se roteiro e rede trazem exatamente o mesmo perigo no mesmo passo, o núcleo aplica o roteiro primeiro.
    // As últimas ocorrências daquele passo correspondem aos ids do canal, na ordem em que chegaram.
    private int ocorrenciasRestantes(List<EventoAplicado> historico, int indice, EventoAplicado aplicacao) {
        int quantidade = 0;
        for (int i = indice; i < historico.size() && historico.get(i).passo() == aplicacao.passo(); i++) {
            if (aplicacao.equals(historico.get(i))) quantidade++;
        }
        return quantidade;
    }

    private static JsonValue resposta(String tipo, String id) {
        JsonValue m = new JsonValue(JsonValue.ValueType.object);
        m.addChild("tipo", new JsonValue(tipo));
        m.addChild("evento", new JsonValue(id));
        return m;
    }

    private void mostrar(String mensagem, float segundos) {
        idAviso = null;
        texto = mensagem;
        restante = segundos;
    }

    void atualizar(float delta) {
        restante = Math.max(0, restante - delta);
    }

    Optional<String> mensagem() {
        if (restante <= 0) return Optional.empty();
        return Optional.of(idAviso == null ? texto : texto.toUpperCase(Locale.ROOT) + " EM " + (int) Math.ceil(restante) + " s");
    }

    boolean consumirAlerta() {
        boolean tocar = alerta;
        alerta = false;
        return tocar;
    }
}
