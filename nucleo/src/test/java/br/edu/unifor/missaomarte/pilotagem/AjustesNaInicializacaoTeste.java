package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DA-48: os padrões dos ajustes se referem uns aos outros. Cada classe carregada primeiro, num carregador novo (como
 * acontece no jogo, que começa pela {@code ConfigDeVoo} e não pelos ajustes), precisa inicializar sem erro. O jogo
 * achou o problema: carregar {@code AjustesDosEventos} antes de {@code AjustesDeVoo} deixava o padrão nulo.
 */
class AjustesNaInicializacaoTeste {

    @Test
    void qualquerClasseCarregadaPrimeiroInicializaOsPadroes() throws Exception {
        URL classes = AjustesDeVoo.class.getProtectionDomain().getCodeSource().getLocation();
        for (String primeira : List.of("AjustesDosEventos", "AjustesDaDificuldade", "ConfigDeVoo", "AjustesDeVoo",
                "PartidaDePilotagem")) {
            try (URLClassLoader isolado = new URLClassLoader(new URL[] {classes},
                    ClassLoader.getPlatformClassLoader())) {
                Class.forName("br.edu.unifor.missaomarte.pilotagem." + primeira, true, isolado);
                Class<?> ajustes = Class.forName("br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo", true, isolado);
                Class<?> eventos = Class.forName("br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos", true, isolado);
                assertNotNull(ajustes.getField("PADRAO").get(null), primeira);
                assertNotNull(eventos.getField("PADRAO").get(null), primeira);
            }
        }
    }
}
