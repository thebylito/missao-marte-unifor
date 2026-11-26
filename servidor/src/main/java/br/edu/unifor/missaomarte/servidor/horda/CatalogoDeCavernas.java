package br.edu.unifor.missaomarte.servidor.horda;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import br.edu.unifor.missaomarte.pilotagem.horda.ValidadorDeCaverna;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CatalogoDeCavernas {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogoDeCavernas.class);

    private final List<DescricaoDeCaverna> todas;
    private final List<DescricaoDeCaverna> liberadas;
    private final Map<String, DescricaoDeCaverna> porId;

    public CatalogoDeCavernas(JsonMapper json, @Value("classpath*:cavernas/*.json") Resource[] arquivos) {
        var leitor = json.readerFor(DescricaoDeCaverna.class)
                .with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        Map<String, Resource> origens = new HashMap<>();
        Map<String, DescricaoDeCaverna> validas = new TreeMap<>();
        for (Resource arquivo : arquivos) {
            DescricaoDeCaverna caverna;
            try (var entrada = arquivo.getInputStream()) {
                caverna = leitor.readValue(entrada);
            } catch (IOException | JacksonException e) {
                LOG.warn("Caverna inválida em {}: {}", arquivo, e.getMessage());
                continue;
            }
            if (caverna == null) {
                LOG.warn("Caverna inválida em {}: esperada uma descrição, veio null", arquivo);
                continue;
            }
            String id = caverna.id();
            if (id != null && !id.isBlank()) {
                Resource anterior = origens.putIfAbsent(id, arquivo);
                if (anterior != null) {
                    throw new IllegalStateException("Id de caverna repetido '" + id + "' em " + anterior + " e " + arquivo);
                }
            }
            var problemas = ValidadorDeCaverna.problemas(caverna);
            if (problemas.isEmpty()) {
                validas.put(id, caverna);
            } else {
                LOG.warn("Caverna '{}' inválida em {}: {}", id, arquivo, String.join("; ", problemas));
            }
        }
        todas = List.copyOf(validas.values());
        liberadas = todas.stream().filter(DescricaoDeCaverna::liberada).toList();
        porId = Map.copyOf(validas);
    }

    public List<DescricaoDeCaverna> todas() {
        return todas;
    }

    public List<DescricaoDeCaverna> liberadas() {
        return liberadas;
    }

    public Optional<DescricaoDeCaverna> porId(String id) {
        return Optional.ofNullable(porId.get(id));
    }
}
