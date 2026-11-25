package br.edu.unifor.missaomarte.grafico.online;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Endereço do servidor de ranking (Fase 6), configurável dentro do jogo: no celular não existe "-D" na linha de
 * comando, e "localhost" é o próprio aparelho. Ordem de prioridade:
 * <ol>
 *   <li>o endereço passado ao abrir o jogo ({@code -Dmissaomarte.servidor}, só no desktop);</li>
 *   <li>o endereço que o jogador gravou na tela de entrada (opção 4), guardado nas preferências;</li>
 *   <li>o endereço embutido no build de publicação ({@link #padraoDoBuild(String)}), se houver;</li>
 *   <li>o padrão da plataforma (desktop: localhost; Android: 10.0.2.2, o computador visto pelo emulador).</li>
 * </ol>
 * Nenhum segredo fica aqui: só o endereço.
 */
public final class ConfiguracaoDoServidor {

    /** Onde o endereço fica guardado. No jogo, as Preferences da libGDX; nos testes, um mapa em memória. */
    public interface Preferencias {
        String ler(String chave);

        void gravar(String chave, String valor);
    }

    static final String CHAVE = "servidor";

    private final Preferencias preferencias;
    private final String padrao;
    private final String forcado;

    /**
     * @param padrao  endereço da plataforma quando nada foi configurado
     * @param forcado endereço passado ao abrir o jogo (tem prioridade e não pode ser trocado no jogo); pode ser null
     */
    public ConfiguracaoDoServidor(Preferencias preferencias, String padrao, String forcado) {
        this.preferencias = preferencias;
        this.padrao = padrao;
        this.forcado = forcado == null || forcado.isBlank() ? null : forcado.trim();
    }

    /**
     * Recurso que o build de publicação grava no core ({@code -Pmissaomarte.servidorPadrao=https://...}): o APK e o
     * jogo desktop baixados do site já vêm apontando para o servidor publicado. No build de desenvolvimento ele
     * fica vazio, e vale o padrão da plataforma.
     */
    static final String RECURSO_DO_BUILD = "/missaomarte/servidor-padrao.txt";

    /** O endereço embutido no build, se houver e for válido; senão, o padrão da plataforma. */
    public static String padraoDoBuild(String padraoDaPlataforma) {
        return padraoDoBuild(ConfiguracaoDoServidor.class.getResourceAsStream(RECURSO_DO_BUILD), padraoDaPlataforma);
    }

    static String padraoDoBuild(InputStream recurso, String padraoDaPlataforma) {
        if (recurso == null) {
            return padraoDaPlataforma;
        }
        try (InputStream in = recurso) {
            String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return texto.isEmpty() || problema(texto).isPresent() ? padraoDaPlataforma : normalizar(texto);
        } catch (IOException e) {
            return padraoDaPlataforma;
        }
    }

    /** Só em memória (testes e telas que não precisam gravar nada). */
    public static ConfiguracaoDoServidor emMemoria(String padrao) {
        java.util.Map<String, String> mapa = new java.util.HashMap<>();
        return new ConfiguracaoDoServidor(new Preferencias() {
            @Override public String ler(String chave) { return mapa.get(chave); }
            @Override public void gravar(String chave, String valor) { mapa.put(chave, valor); }
        }, padrao, null);
    }

    public String endereco() {
        if (forcado != null) {
            return forcado;
        }
        String gravado = preferencias.ler(CHAVE);
        return gravado == null || gravado.isBlank() ? padrao : gravado;
    }

    /** O endereço veio da linha de comando: a tela mostra, mas não deixa trocar. */
    public boolean isForcado() {
        return forcado != null;
    }

    /**
     * Grava um novo endereço, se for válido. Devolve o problema encontrado (para mostrar ao jogador) ou vazio se
     * gravou. Vazio volta ao padrão da plataforma.
     */
    public Optional<String> alterar(String novo) {
        if (isForcado()) {
            return Optional.of("Endereço definido ao abrir o jogo (-Dmissaomarte.servidor); não dá para trocar aqui.");
        }
        String texto = novo == null ? "" : novo.trim();
        if (texto.isEmpty()) {
            preferencias.gravar(CHAVE, "");
            return Optional.empty();
        }
        Optional<String> problema = problema(texto);
        if (problema.isEmpty()) {
            preferencias.gravar(CHAVE, normalizar(texto));
        }
        return problema;
    }

    /** O que há de errado com o endereço, ou vazio se ele serve (http ou https, com host; porta opcional). */
    public static Optional<String> problema(String endereco) {
        String texto = endereco.trim();
        if (!(texto.startsWith("http://") || texto.startsWith("https://"))) {
            return Optional.of("Use http:// ou https:// no começo do endereço.");
        }
        try {
            URI uri = new URI(texto);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return Optional.of("Falta o nome ou o IP do servidor (ex.: http://192.168.0.10:8080).");
            }
            if (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535)) {
                return Optional.of("A porta deve ficar entre 1 e 65535.");
            }
            if (uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
                return Optional.of("Informe só o endereço, sem ?, # ou usuário.");
            }
            return Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.of("Endereço inválido.");
        }
    }

    /** Sem espaços nas pontas e sem barra no fim. */
    static String normalizar(String endereco) {
        String t = endereco.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
