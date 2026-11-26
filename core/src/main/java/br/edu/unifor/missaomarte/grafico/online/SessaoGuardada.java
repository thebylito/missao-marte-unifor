package br.edu.unifor.missaomarte.grafico.online;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A sessão do piloto guardada no aparelho (DA-38). O Android mata o jogo em segundo plano quando falta memória, e
 * a sessão que ficava só na memória sumia junto: o jogador voltava e caía na tela de entrada. Agora o nome, o
 * token, a validade e o servidor que emitiu o token ficam nas preferências do app (no Android, as SharedPreferences
 * privadas; no desktop, ~/.prefs), e o jogo os recupera ao abrir.
 *
 * <p>Só volta uma sessão do mesmo servidor e ainda dentro da validade. O resto é apagado na hora, para não deixar
 * token sem uso no aparelho. A senha nunca é guardada.
 */
public final class SessaoGuardada {

    static final String NOME = "sessao.nome";
    static final String TOKEN = "sessao.token";
    static final String EXPIRA_EM = "sessao.expiraEm";
    static final String SKIN = "sessao.skin";
    static final String SERVIDOR = "sessao.servidor";

    private final ConfiguracaoDoServidor.Preferencias preferencias;
    private final Clock relogio;

    public SessaoGuardada(ConfiguracaoDoServidor.Preferencias preferencias) {
        this(preferencias, Clock.systemUTC());
    }

    public SessaoGuardada(ConfiguracaoDoServidor.Preferencias preferencias, Clock relogio) {
        this.preferencias = preferencias;
        this.relogio = relogio;
    }

    /** Só na memória (testes e o piloto da janela): nada vai para o disco. */
    public static SessaoGuardada emMemoria() {
        Map<String, String> mapa = new HashMap<>();
        return new SessaoGuardada(new ConfiguracaoDoServidor.Preferencias() {
            @Override public String ler(String chave) { return mapa.get(chave); }
            @Override public void gravar(String chave, String valor) { mapa.put(chave, valor); }
        });
    }

    /** Guarda a sessão recém-aberta e o servidor que emitiu o token. */
    public void guardar(Sessao sessao, String servidor) {
        preferencias.gravar(NOME, sessao.nome());
        preferencias.gravar(TOKEN, sessao.token());
        preferencias.gravar(EXPIRA_EM, sessao.expiraEm() == null ? "" : sessao.expiraEm().toString());
        preferencias.gravar(SKIN, sessao.skin());
        preferencias.gravar(SERVIDOR, servidor);
    }

    /**
     * A sessão guardada, se for do servidor atual e o token ainda valer. Senão, apaga o que havia e devolve vazio.
     */
    public Optional<Sessao> recuperar(String servidorAtual) {
        String token = preferencias.ler(TOKEN);
        if (vazio(token)) {
            return Optional.empty();   // o caso comum (convidado ou nunca entrou): nada a apagar
        }
        String nome = preferencias.ler(NOME);
        String servidor = preferencias.ler(SERVIDOR);
        if (vazio(nome) || vazio(servidor)) {
            esquecer();
            return Optional.empty();
        }
        Instant expiraEm;
        try {
            String texto = preferencias.ler(EXPIRA_EM);
            expiraEm = vazio(texto) ? null : Instant.parse(texto);
        } catch (DateTimeParseException e) {
            esquecer();
            return Optional.empty();
        }
        boolean vencida = expiraEm != null && !relogio.instant().isBefore(expiraEm);
        if (!servidor.equals(servidorAtual) || vencida) {
            esquecer();
            return Optional.empty();
        }
        return Optional.of(new Sessao(nome, token, expiraEm, preferencias.ler(SKIN)));
    }

    /** "Trocar de piloto" ou token recusado pelo servidor: o aparelho esquece a sessão. */
    public void esquecer() {
        preferencias.gravar(NOME, "");
        preferencias.gravar(TOKEN, "");
        preferencias.gravar(EXPIRA_EM, "");
        preferencias.gravar(SKIN, "");
        preferencias.gravar(SERVIDOR, "");
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
