package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.horda.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class CavernasEmJsonTeste {

    static DescricaoDeCaverna ler(String id) throws IOException {
        try (var entrada = new ClassPathResource("cavernas/" + id + ".json").getInputStream()) {
            return new JsonMapper().readValue(entrada, DescricaoDeCaverna.class);
        }
    }

    @Test
    void marcianaComOsPassageirosOriginaisReproduzACavernaDoJogo() throws IOException {
        var marciana = ler("marciana");
        assertThat(marciana.pontosDePouso()).hasSizeBetween(7, 8)
                .contains(new PontoDePouso(2000, 116), new PontoDePouso(2790, 776));
        var passageiros = List.of(new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 0),
                new PassageiroNaHorda(TipoPassageiro.ENGENHEIRO, 1),
                new PassageiroNaHorda(TipoPassageiro.PROFESSOR, 2));
        var fases = marciana.asteroides().stream().map(AsteroideDescrito::fase).toList();
        var horda = new DescricaoDaHorda(1, Dificuldade.MEDIO, marciana, passageiros, 1, fases, List.of(), List.of());
        var montada = MontadorDaHorda.montar(horda).caverna();
        var original = Caverna.marciana();
        assertThat(montada.getLargura()).isEqualTo(original.getLargura());
        assertThat(montada.getAltura()).isEqualTo(original.getAltura());
        assertThat(montada.getRochas()).isEqualTo(original.getRochas());
        // Inclui as dimensões das plataformas, seus passageiros e todos os parâmetros das órbitas.
        assertThat(montada.getPlataformas()).usingRecursiveComparison().isEqualTo(original.getPlataformas());
        assertThat(montada.getAsteroides()).usingRecursiveComparison().isEqualTo(original.getAsteroides());
    }

    @ParameterizedTest
    @ValueSource(strings = {"poco-polar", "veio-de-ferro"})
    void cavernaNovaValidaParaEnsaioNoDificil(String id) throws IOException {
        var caverna = ler(id);
        assertThat(caverna.id()).isEqualTo(id);
        assertThat(caverna.liberada()).isFalse();
        assertThat(caverna.largura()).isBetween(2400.0, 4800.0);
        assertThat(caverna.altura()).isBetween(1200.0, 2400.0);
        assertThat(caverna.pontosDePouso()).hasSizeBetween(7, 8);
        assertThat(caverna.asteroides()).hasSizeBetween(2, 4);
        assertThat(ValidadorDeCaverna.problemas(caverna)).isEmpty();
    }
}
