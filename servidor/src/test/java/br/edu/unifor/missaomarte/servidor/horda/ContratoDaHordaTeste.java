package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class ContratoDaHordaTeste {

    @Test
    void exemploPublicadoCorrespondeAoSorteioDaHorda42() throws IOException {
        var esperada = SorteioDaHorda.sortear(42, 20261271L, List.of(CavernasEmJsonTeste.ler("marciana")), null);
        try (var entrada = new ClassPathResource("exemplos/horda.json").getInputStream()) {
            assertThat(new JsonMapper().readValue(entrada, DescricaoDaHorda.class)).isEqualTo(esperada);
        }
    }
}
