package br.edu.unifor.missaomarte.servidor.ajustes;

import br.edu.unifor.missaomarte.pilotagem.AjustesDaDificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fase 8 (DA-48): lê os ajustes do jogo das variáveis {@code MISSAOMARTE_AJUSTE_*}. Todas são opcionais: sem elas,
 * vale {@link AjustesDeVoo#PADRAO}. Um nome desconhecido (erro de digitação), um valor que não é número, uma duração
 * que não é inteira ou um valor fora da faixa impedem o servidor de subir, com o nome da variável na mensagem. Assim
 * um ajuste errado não chega aos jogadores: no deploy start-first, o servidor anterior continua no ar.
 *
 * <p>Os nomes estão em {@code servidor/PUBLICACAO.md}, com o padrão e a faixa de cada um.
 */
public final class AjustesDoAmbiente {

    public static final String PREFIXO = "MISSAOMARTE_AJUSTE_";

    private static final List<String> DIFICULDADES = List.of("FACIL", "MEDIO", "DIFICIL");

    /** O fim do nome da variável e o campo de {@link AjustesDaDificuldade} que ela muda. */
    private static final Map<String, String> DA_DIFICULDADE = ordem(
            "TANQUE", "tanque",
            "QUEDA", "quedaMaxima",
            "DESLIZE", "deslizeMaximo",
            "INCLINACAO", "inclinacaoMaxima",
            "ASTEROIDES", "velocidadeDosAsteroides");

    /** O fim do nome da variável e o campo de {@link AjustesDosEventos} que ela muda. */
    private static final Map<String, String> DOS_EVENTOS = ordem(
            "METEOROS_MIN", "velocidadeMinimaDosMeteoros",
            "METEOROS_MAX", "velocidadeMaximaDosMeteoros",
            "DURACAO_CHUVA", "duracaoDaChuva",
            "DURACAO_TEMPESTADE", "duracaoDaTempestade",
            "DURACAO_PANE", "duracaoDaPane",
            "DURACAO_VIP", "duracaoDoVip");

    /** Os 21 nomes aceitos, na ordem da tabela da publicação. */
    public static final List<String> NOMES = nomes();

    private AjustesDoAmbiente() {
    }

    /** @throws IllegalStateException com o nome da variável, se alguma estiver errada */
    public static AjustesDeVoo ler(Map<String, String> ambiente) {
        Map<String, Double> valores = new HashMap<>();
        for (Map.Entry<String, String> variavel : ambiente.entrySet()) {
            String nome = variavel.getKey();
            if (!pareceAjuste(nome)) {
                continue;
            }
            if (!NOMES.contains(nome)) {
                throw new IllegalStateException("Variável de ajuste desconhecida: " + nome + ". As aceitas são "
                        + String.join(", ", NOMES) + " (servidor/PUBLICACAO.md)");
            }
            String texto = variavel.getValue() == null ? "" : variavel.getValue().strip();
            if (!texto.isEmpty()) {
                valores.put(nome, numero(nome, texto));
            }
        }
        AjustesDeVoo padrao = AjustesDeVoo.PADRAO;
        return new AjustesDeVoo(dificuldade("FACIL", padrao.facil(), valores),
                dificuldade("MEDIO", padrao.medio(), valores), dificuldade("DIFICIL", padrao.dificil(), valores),
                eventos(padrao.eventos(), valores));
    }

    /**
     * O nome tenta ser um ajuste: começa com "MISSAOMARTE_AJUST", sem contar maiúsculas e separadores. Assim um erro
     * no começo do nome ({@code MISSAOMARTE_AJUSTES_...}, {@code missaomarte_ajuste_...}) também impede a subida.
     */
    public static boolean pareceAjuste(String nome) {
        return nome.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "").startsWith("MISSAOMARTEAJUST");
    }

    private static AjustesDaDificuldade dificuldade(String nome, AjustesDaDificuldade padrao,
                                                    Map<String, Double> valores) {
        String grupo = PREFIXO + nome + "_";
        return criar(grupo, DA_DIFICULDADE, () -> new AjustesDaDificuldade(
                valores.getOrDefault(grupo + "TANQUE", padrao.tanque()),
                valores.getOrDefault(grupo + "QUEDA", padrao.quedaMaxima()),
                valores.getOrDefault(grupo + "DESLIZE", padrao.deslizeMaximo()),
                valores.getOrDefault(grupo + "INCLINACAO", padrao.inclinacaoMaxima()),
                valores.getOrDefault(grupo + "ASTEROIDES", padrao.velocidadeDosAsteroides())));
    }

    private static AjustesDosEventos eventos(AjustesDosEventos padrao, Map<String, Double> valores) {
        return criar(PREFIXO, DOS_EVENTOS, () -> new AjustesDosEventos(
                valores.getOrDefault(PREFIXO + "METEOROS_MIN", padrao.velocidadeMinimaDosMeteoros()),
                valores.getOrDefault(PREFIXO + "METEOROS_MAX", padrao.velocidadeMaximaDosMeteoros()),
                valores.getOrDefault(PREFIXO + "DURACAO_CHUVA", padrao.duracaoDaChuva()),
                valores.getOrDefault(PREFIXO + "DURACAO_TEMPESTADE", padrao.duracaoDaTempestade()),
                valores.getOrDefault(PREFIXO + "DURACAO_PANE", padrao.duracaoDaPane()),
                valores.getOrDefault(PREFIXO + "DURACAO_VIP", padrao.duracaoDoVip())));
    }

    /** A faixa é conferida no nucleo; aqui a mensagem ganha o nome da variável do campo recusado. */
    private static <T> T criar(String grupo, Map<String, String> campos, Supplier<T> criar) {
        try {
            return criar.get();
        } catch (IllegalArgumentException e) {
            String nome = campos.entrySet().stream()
                    .filter(campo -> e.getMessage().startsWith(campo.getValue() + " "))
                    .map(campo -> grupo + campo.getKey()).findFirst().orElse(grupo + "*");
            throw new IllegalStateException("Ajuste do jogo inválido em " + nome + ": " + e.getMessage()
                    + " (servidor/PUBLICACAO.md)", e);
        }
    }

    private static double numero(String nome, String texto) {
        double valor;
        try {
            valor = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalStateException(nome + " não é um número: '" + texto + "'", e);
        }
        // O canal manda a duração do evento em segundos inteiros (o formato da mensagem desde a etapa 6).
        if (nome.startsWith(PREFIXO + "DURACAO_") && valor != Math.rint(valor)) {
            throw new IllegalStateException(nome + " precisa ser um número inteiro de segundos (veio " + texto + ")");
        }
        return valor;
    }

    private static List<String> nomes() {
        List<String> nomes = new ArrayList<>();
        for (String dificuldade : DIFICULDADES) {
            DA_DIFICULDADE.keySet().forEach(fim -> nomes.add(PREFIXO + dificuldade + "_" + fim));
        }
        DOS_EVENTOS.keySet().forEach(fim -> nomes.add(PREFIXO + fim));
        return List.copyOf(nomes);
    }

    private static Map<String, String> ordem(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(pares[i], pares[i + 1]);
        }
        return mapa;
    }
}
