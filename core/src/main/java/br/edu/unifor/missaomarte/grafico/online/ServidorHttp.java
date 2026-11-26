package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adaptador HTTP para a API do servidor de ranking (padrão Adapter). Usa o {@code Gdx.net} da libGDX, que é
 * assíncrono e funciona igual no desktop e no Android (Fase 6), e o JSON da própria libGDX. A resposta é
 * entregue na thread do jogo (por padrão, {@code Gdx.app.postRunnable}).
 *
 * <p>Nunca registra em log o corpo dos pedidos (que leva a senha) nem o token.
 */
public final class ServidorHttp implements ServidorDoJogo {

    /** Tempo máximo de espera por resposta, em milissegundos. */
    static final int TEMPO_LIMITE = 5000;

    private final Supplier<String> endereco;
    private final Supplier<Net> rede;
    private final Executor naThreadDoJogo;

    /**
     * @param endereco       por exemplo {@code http://localhost:8080} (sem barra no fim)
     * @param rede           de onde vem o {@code Net} (no jogo, {@code Gdx.net}, que só existe depois de a janela abrir)
     * @param naThreadDoJogo onde entregar as respostas (no jogo, {@code Gdx.app::postRunnable}; nos testes, na hora)
     */
    public ServidorHttp(String endereco, Supplier<Net> rede, Executor naThreadDoJogo) {
        this(() -> endereco, rede, naThreadDoJogo);
    }

    /** Fase 6: o endereço é lido a cada pedido, então trocá-lo na tela de entrada vale na hora. */
    public ServidorHttp(Supplier<String> endereco, Supplier<Net> rede, Executor naThreadDoJogo) {
        this.endereco = endereco;
        this.rede = rede;
        this.naThreadDoJogo = naThreadDoJogo;
    }

    /** Versão do jogo: usa o {@code Gdx.net} e entrega as respostas com {@code Gdx.app.postRunnable}. */
    public static ServidorHttp daLibgdx(String endereco) {
        return daLibgdx(() -> endereco);
    }

    public static ServidorHttp daLibgdx(Supplier<String> endereco) {
        return new ServidorHttp(endereco, () -> Gdx.net, r -> Gdx.app.postRunnable(r));
    }

    public String getEndereco() {
        String e = endereco.get();
        return e.endsWith("/") ? e.substring(0, e.length() - 1) : e;
    }

    // ------------------------------------------------------------------ operações

    @Override
    public void cadastrar(String nome, String senha, Resposta<Void> resposta) {
        JsonValue corpo = objeto();
        corpo.addChild("nome", new JsonValue(nome));
        corpo.addChild("senha", new JsonValue(senha));
        enviar(HttpMethods.POST, "/api/jogadores", null, corpo, json -> null, resposta);
    }

    @Override
    public void entrar(String nome, String senha, Resposta<Sessao> resposta) {
        JsonValue corpo = objeto();
        corpo.addChild("nome", new JsonValue(nome));
        corpo.addChild("senha", new JsonValue(senha));
        enviar(HttpMethods.POST, "/api/sessoes", null, corpo,
                json -> new Sessao(json.get("jogador").getString("nome"), json.getString("token"),
                        instante(json.getString("expiraEm", null))), resposta);
    }

    /** A validade do token, ou null se o servidor não a mandou num formato legível (a sessão vale até um 401). */
    private static Instant instante(String texto) {
        try {
            return texto == null ? null : Instant.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public void iniciarPartida(Sessao sessao, Modo modo, Dificuldade dificuldade, Resposta<Bilhete> resposta) {
        JsonValue corpo = objeto();
        corpo.addChild("modo", new JsonValue(modo.name()));
        corpo.addChild("dificuldade", new JsonValue(dificuldade.name()));
        enviar(HttpMethods.POST, "/api/partidas/inicio", sessao, corpo,
                json -> new Bilhete(json.getString("bilhete"), Instant.parse(json.getString("validoAte"))), resposta);
    }

    @Override
    public void registrarPartida(Sessao sessao, PartidaParaEnviar p, Resposta<Void> resposta) {
        enviarPartida(sessao, p, json -> null, resposta);
    }

    @Override
    public void registrarPartidaComMoedas(Sessao sessao, PartidaParaEnviar p, Resposta<RegistroDePartida> resposta) {
        enviarPartida(sessao, p, json -> json != null && json.has("moedasGanhas") && json.has("saldo")
                ? new RegistroDePartida(json.getInt("moedasGanhas"), json.getInt("saldo")) : null, resposta);
    }

    private <T> void enviarPartida(Sessao sessao, PartidaParaEnviar p, Function<JsonValue, T> ler, Resposta<T> resposta) {
        JsonValue corpo = objeto();
        corpo.addChild("resultado", new JsonValue(p.resultado()));
        corpo.addChild("dificuldade", new JsonValue(p.dificuldade()));
        corpo.addChild("pontos", new JsonValue(p.pontos()));
        corpo.addChild("passageiros", new JsonValue(p.passageiros()));
        corpo.addChild("movimentos", new JsonValue(p.movimentos()));
        corpo.addChild("tempoSegundos", new JsonValue(p.tempoSegundos()));
        corpo.addChild("modo", new JsonValue(p.modo()));   // Fase 7 (um servidor antigo ignora o campo)
        if (p.bilhete() != null) {
            corpo.addChild("bilhete", new JsonValue(p.bilhete()));
        }
        enviar(HttpMethods.POST, "/api/partidas", sessao, corpo, ler, resposta);
    }

    @Override
    public void loja(Resposta<List<SkinDaLoja>> resposta) {
        enviar(HttpMethods.GET, "/api/loja", null, null, json -> lista(json, item -> new SkinDaLoja(
                item.getString("id"), item.getString("nome"), item.getString("linha"),
                item.getInt("nivel"), item.getInt("preco"))), resposta);
    }

    @Override
    public void hangar(Sessao sessao, Resposta<Hangar> resposta) {
        enviar(HttpMethods.GET, "/api/jogadores/eu/hangar", sessao, null, ServidorHttp::lerHangar, resposta);
    }

    @Override
    public void comprar(Sessao sessao, String skin, Resposta<Hangar> resposta) {
        enviar(HttpMethods.POST, "/api/loja/compras", sessao, corpoDaSkin(skin), ServidorHttp::lerHangar, resposta);
    }

    @Override
    public void equipar(Sessao sessao, String skin, Resposta<String> resposta) {
        enviar(HttpMethods.PUT, "/api/jogadores/eu/skin", sessao, corpoDaSkin(skin),
                json -> json.getString("equipada"), resposta);
    }

    private static Hangar lerHangar(JsonValue json) {
        return new Hangar(json.getInt("saldo"), lista(json.get("skins"), JsonValue::asString),
                json.getString("equipada", null));
    }

    private static JsonValue corpoDaSkin(String skin) {
        JsonValue corpo = objeto();
        corpo.addChild("skin", new JsonValue(skin));
        return corpo;
    }

    @Override
    public void ranking(Modo modo, Resposta<List<PosicaoGlobal>> resposta) {
        enviar(HttpMethods.GET, "/api/ranking?modo=" + modo.name(), null, null, json -> lista(json, item -> new PosicaoGlobal(
                item.getInt("posicao"), item.getString("nome"), item.getInt("pontos"), item.getString("dificuldade"),
                item.getInt("passageiros"), item.getLong("tempoSegundos"), item.getString("registradaEm"))), resposta);
    }

    @Override
    public void historico(Sessao sessao, Resposta<List<PartidaDoHistorico>> resposta) {
        enviar(HttpMethods.GET, "/api/partidas/minhas", sessao, null, json -> lista(json, item -> new PartidaDoHistorico(
                item.getString("resultado"), item.getString("dificuldade"), item.getInt("pontos"),
                item.getInt("passageiros"), item.getInt("movimentos"), item.getLong("tempoSegundos"),
                item.getString("registradaEm"), item.getString("modo", "CLASSICO"))), resposta);
    }

    /** O canal das sombras no mesmo servidor ({@code http} vira {@code ws}, {@code https} vira {@code wss}). */
    @Override
    public Optional<ConexaoDeSombras> conexaoDeSombras() {
        try {
            return Optional.of(new ConexaoWebSocket(ConexaoWebSocket.enderecoDasSombras(getEndereco()),
                    naThreadDoJogo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ HTTP

    private <T> void enviar(String metodo, String rota, Sessao sessao, JsonValue corpo,
                            Function<JsonValue, T> ler, Resposta<T> resposta) {
        Net.HttpRequest pedido = new Net.HttpRequest(metodo);
        pedido.setUrl(getEndereco() + rota);
        pedido.setTimeOut(TEMPO_LIMITE);
        pedido.setHeader("Accept", "application/json");
        if (sessao != null) {
            pedido.setHeader("Authorization", "Bearer " + sessao.token());
        }
        if (corpo != null) {
            pedido.setHeader("Content-Type", "application/json");
            pedido.setContent(corpo.toJson(JsonWriter.OutputType.json));
        }
        Net net;
        try {
            net = rede.get();
        } catch (RuntimeException e) {
            net = null;
        }
        if (net == null) {
            naThreadDoJogo.execute(() -> resposta.falha(Falha.semConexao()));
            return;
        }
        net.sendHttpRequest(pedido, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse r) {
                int codigo = r.getStatus().getStatusCode();
                String texto = r.getResultAsString();
                naThreadDoJogo.execute(() -> tratar(codigo, texto, ler, resposta));
            }

            @Override
            public void failed(Throwable erro) {
                naThreadDoJogo.execute(() -> resposta.falha(Falha.semConexao()));
            }

            @Override
            public void cancelled() {
                naThreadDoJogo.execute(() -> resposta.falha(Falha.semConexao()));
            }
        });
    }

    /** Traduz a resposta: 2xx lê o corpo; os erros viram {@link Falha} com a mensagem do próprio servidor. */
    static <T> void tratar(int codigo, String texto, Function<JsonValue, T> ler, Resposta<T> resposta) {
        if (codigo >= 200 && codigo < 300) {
            T valor;
            try {
                JsonValue json = texto == null || texto.isBlank() ? null : new JsonReader().parse(texto);
                valor = ler.apply(json);
            } catch (RuntimeException e) {
                resposta.falha(new Falha(Falha.Motivo.RESPOSTA_INVALIDA, "Resposta inesperada do servidor."));
                return;
            }
            resposta.sucesso(valor);
            return;
        }
        JsonValue problema = problema(texto);
        String detalhe = detalhe(problema);
        switch (codigo) {
            case 400:
                resposta.falha(new Falha(Falha.Motivo.DADOS_INVALIDOS, detalhe != null ? detalhe : "Dados inválidos."));
                break;
            case 401:
                resposta.falha(new Falha(Falha.Motivo.NAO_AUTORIZADO,
                        detalhe != null ? detalhe : "Não autorizado. Entre novamente."));
                break;
            case 404:
                resposta.falha(new Falha(Falha.Motivo.NAO_ENCONTRADO,
                        detalhe != null ? detalhe : "Recurso indisponível neste servidor."));
                break;
            case 409:
                String motivo = problema == null ? null : problema.getString("motivo", null);
                resposta.falha(new Falha(Falha.Motivo.CONFLITO, detalhe != null ? detalhe : "Conflito.", motivo));
                break;
            default:
                resposta.falha(new Falha(Falha.Motivo.ERRO_DO_SERVIDOR, "Erro no servidor (" + codigo + ")."));
        }
    }

    private static JsonValue problema(String texto) {
        try {
            JsonValue json = new JsonReader().parse(texto);
            return json != null && json.isObject() ? json : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * O "detail" do erro (RFC 9457) e, na validação, as mensagens de cada campo. Devolve null se o corpo não for
     * um erro no formato combinado.
     */
    private static String detalhe(JsonValue json) {
        try {
            if (json == null || !json.has("detail")) {
                return null;
            }
            StringBuilder s = new StringBuilder(json.getString("detail"));
            JsonValue campos = json.get("campos");
            if (campos != null && campos.isObject()) {
                for (JsonValue c = campos.child; c != null; c = c.next) {
                    s.append(c == campos.child ? ": " : "; ").append(c.asString());
                }
            }
            String mensagem = s.toString();
            if (mensagem.isEmpty()) {
                return null;
            }
            String frase = Character.toUpperCase(mensagem.charAt(0)) + mensagem.substring(1);
            return frase.endsWith(".") ? frase : frase + ".";
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonValue objeto() {
        return new JsonValue(JsonValue.ValueType.object);
    }

    private static <T> List<T> lista(JsonValue json, Function<JsonValue, T> item) {
        if (json == null || !json.isArray()) {
            throw new IllegalArgumentException("esperava uma lista");
        }
        List<T> itens = new ArrayList<>();
        for (JsonValue v = json.child; v != null; v = v.next) {
            itens.add(item.apply(v));
        }
        return itens;
    }
}
