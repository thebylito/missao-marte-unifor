package br.edu.unifor.missaomarte.servidor.ajustes;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Fase 8 (DA-48): os ajustes do jogo, lidos uma vez na subida. As variáveis vêm do ambiente do contêiner (no
 * Dokploy, a aba Environment) ou, nos testes, de propriedades com os mesmos nomes.
 */
@Configuration
class ConfiguracaoDosAjustes {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDosAjustes.class);

    @Bean
    AjustesDeVoo ajustesDeVoo(ConfigurableEnvironment ambiente) {
        Map<String, String> definidas = new TreeMap<>();
        for (PropertySource<?> fonte : ambiente.getPropertySources()) {
            if (fonte instanceof EnumerablePropertySource<?> nomes) {
                for (String nome : nomes.getPropertyNames()) {
                    if (AjustesDoAmbiente.pareceAjuste(nome)) {
                        definidas.putIfAbsent(nome, ambiente.getProperty(nome));
                    }
                }
            }
        }
        AjustesDeVoo ajustes = AjustesDoAmbiente.ler(definidas);
        if (definidas.isEmpty()) {
            LOG.info("Ajustes do jogo: os padrão (nenhuma variável {}* definida)", AjustesDoAmbiente.PREFIXO);
        } else {
            LOG.info("Ajustes do jogo: {}", definidas);
        }
        return ajustes;
    }
}
