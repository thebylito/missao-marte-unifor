package br.edu.unifor.missaomarte.servidor.loja;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fase 8: o catálogo da loja, lido uma vez de {@code skins.json} na subida do servidor. Os preços mudam sem nova
 * versão do jogo (basta publicar o servidor). Um catálogo com erro impede o servidor de subir.
 *
 * <p>Regras: ids e linhas em {@code [a-z0-9_]} (até {@value #TAMANHO_MAXIMO_DO_ID} caracteres, o tamanho da coluna),
 * ids únicos, um nível por posição em cada linha, preço não negativo, nome preenchido e a skin {@code padrao} de
 * graça. A ordem da loja é a das linhas, como aparecem no arquivo, e dentro de cada linha a dos níveis.
 */
@Component
public class CatalogoDeSkins {

    public static final String ARQUIVO = "skins.json";
    static final int TAMANHO_MAXIMO_DO_ID = 32;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1," + TAMANHO_MAXIMO_DO_ID + "}");

    private final List<Skin> skins;
    private final Map<String, Skin> porId;

    /** O catálogo do servidor ({@value #ARQUIVO}, no classpath). */
    public CatalogoDeSkins() {
        this(lerArquivo());
    }

    private CatalogoDeSkins(List<Skin> lidas) {
        this.skins = List.copyOf(validarEOrdenar(lidas));
        this.porId = skins.stream().collect(Collectors.toUnmodifiableMap(Skin::id, Function.identity()));
    }

    /** Um catálogo a partir do texto JSON (para os testes). */
    static CatalogoDeSkins de(String json) {
        try {
            return new CatalogoDeSkins(List.of(JsonMapper.builder().build().readValue(json, Skin[].class)));
        } catch (JacksonException e) {
            throw new IllegalStateException("catálogo de skins com JSON inválido: " + e.getOriginalMessage(), e);
        }
    }

    private static List<Skin> lerArquivo() {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return de(new String(entrada.readAllBytes(), StandardCharsets.UTF_8)).skins;
        } catch (IOException e) {
            throw new UncheckedIOException("não deu para ler o catálogo de skins (" + ARQUIVO + ")", e);
        }
    }

    private static List<Skin> validarEOrdenar(List<Skin> lidas) {
        Set<String> ids = new HashSet<>();
        Set<String> niveis = new HashSet<>();
        Map<String, Integer> ordemDasLinhas = new LinkedHashMap<>();
        for (Skin s : lidas) {
            if (s == null || s.id() == null || !ID.matcher(s.id()).matches()) {
                throw invalido("id de skin inválido (use a-z, 0-9 e _): " + (s == null ? null : s.id()));
            }
            if (!ids.add(s.id())) {
                throw invalido("id de skin repetido: " + s.id());
            }
            if (s.linha() == null || !ID.matcher(s.linha()).matches()) {
                throw invalido("linha inválida na skin " + s.id() + ": " + s.linha());
            }
            if (s.nivel() < 1 || !niveis.add(s.linha() + "#" + s.nivel())) {
                throw invalido("nível inválido ou repetido na linha " + s.linha() + ": skin " + s.id());
            }
            if (s.nome() == null || s.nome().isBlank()) {
                throw invalido("skin sem nome: " + s.id());
            }
            if (s.preco() < 0) {
                throw invalido("preço negativo na skin " + s.id());
            }
            ordemDasLinhas.putIfAbsent(s.linha(), ordemDasLinhas.size());
        }
        Skin padrao = lidas.stream().filter(s -> s.id().equals(Skin.PADRAO)).findFirst()
                .orElseThrow(() -> invalido("o catálogo precisa da skin " + Skin.PADRAO));
        if (padrao.preco() != 0) {
            throw invalido("a skin " + Skin.PADRAO + " precisa ser de graça");
        }
        List<Skin> ordenadas = new ArrayList<>(lidas);
        ordenadas.sort(Comparator.comparing((Skin s) -> ordemDasLinhas.get(s.linha())).thenComparing(Skin::nivel));
        return ordenadas;
    }

    private static IllegalStateException invalido(String motivo) {
        return new IllegalStateException("catálogo de skins inválido: " + motivo);
    }

    /** Todas as skins, na ordem da loja. */
    public List<Skin> todas() {
        return skins;
    }

    public Optional<Skin> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }
}
