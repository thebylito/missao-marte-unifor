package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class ApiDaHordaTeste extends ApiTeste {

    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean CatalogoDeCavernas catalogo;

    @Test
    void trocaNaHoraParDeFortalezaEUsaOFormatoDoContrato() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T22:59:59Z");
        String antes = mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-11-26T21:00:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-11-26T23:00:00Z"))
                .andExpect(jsonPath("$.segundosParaAcabar").value(1))
                .andExpect(jsonPath("$.segundosParaProxima").value(1))
                .andExpect(jsonPath("$.proximaAs").value("20:00"))
                .andExpect(jsonPath("$.caverna.id").isString())
                .andExpect(jsonPath("$.caverna.nome").isString())
                .andExpect(jsonPath("$.modificadores").isArray())
                .andExpect(jsonPath("$.descricao.caverna.pontosDePouso[0].superficie").isNumber())
                .andExpect(jsonPath("$.descricao.passageiros.length()").value(3))
                .andExpect(jsonPath("$.descricao.fasesDosAsteroides").isArray())
                .andReturn().getResponse().getContentAsString();
        relogio.avancar(Duration.ofSeconds(1));
        String depois = atual();
        assertThat(numero(depois)).isEqualTo(numero(antes) + 1);
        assertThat((String) JsonPath.read(depois, "$.proximaAs")).isEqualTo("22:00");
        assertThat((Number) JsonPath.read(depois, "$.segundosParaAcabar")).isEqualTo(7200);
    }

    @Test
    void releDescricaoGravadaMesmoSemCavernasLiberadasNoCatalogoNovo() throws Exception {
        String primeira = atual();
        doReturn(List.of()).when(catalogo).liberadas();

        assertThat((Object) JsonPath.read(atual(), "$.descricao")).isEqualTo(JsonPath.read(primeira, "$.descricao"));
        assertThat(jdbc.queryForObject("select count(*) from hordas where numero = ?", Integer.class, numero(primeira)))
                .isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void duasGeracoesSimultaneasPublicamUmaUnicaDescricao() throws Exception {
        relogio.agora = Instant.parse("2026-10-03T10:00:00Z");
        var barreira = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return atual(); });
            var segunda = executor.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return atual(); });
            String resposta = primeira.get(10, TimeUnit.SECONDS);
            assertThat(segunda.get(10, TimeUnit.SECONDS)).isEqualTo(resposta);
            assertThat(jdbc.update("delete from hordas where numero = ?", numero(resposta))).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void bilheteObrigatorioVinculadoAHordaComLimiteEPremio() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T22:00:00Z");
        String atual = atual();
        long numero = numero(atual);
        String dificuldade = JsonPath.read(atual, "$.dificuldade");
        String ana = novoJogador("Ana");
        emitir(ana, dificuldade, "").andExpect(status().isBadRequest());
        registrar(ana, partidaHorda(dificuldade, 75, numero, null)).andExpect(status().isBadRequest());

        String bilhete = bilhete(ana, dificuldade, numero);
        assertThat(jdbc.queryForObject("select horda_numero from bilhetes where codigo = cast(? as uuid)",
                Long.class, bilhete)).isEqualTo(numero);
        relogio.avancar(Duration.ofSeconds(45));
        registrar(ana, partidaHorda(dificuldade, 76, numero, bilhete)).andExpect(status().isBadRequest());
        registrar(ana, partidaHorda(dificuldade, 75, numero, bilhete)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.colocacao").value(1))
                .andExpect(jsonPath("$.moedasGanhas").value(5))
                .andExpect(jsonPath("$.saldo").value(5));
        registrar(ana, partidaHorda(dificuldade, 75, numero, bilhete)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select origem from moedas", String.class)).isEqualTo("VITORIA_HORDA");
        assertThat(jdbc.queryForObject("select horda_numero from partidas", Long.class)).isEqualTo(numero);
    }

    @Test
    void aceitaDuranteAToleranciaERecusaDepoisERankingFicaNaHordaCerta() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T22:59:00Z");
        String atual = atual();
        long numero = numero(atual);
        String dificuldade = JsonPath.read(atual, "$.dificuldade");
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        String bilhete = bilhete(ana, dificuldade, numero);
        relogio.agora = Instant.parse("2026-11-26T23:27:00Z");
        registrar(ana, partidaHorda(dificuldade, 70, numero, bilhete)).andExpect(status().isCreated());
        String segundo = bilhete(bia, dificuldade, numero);
        relogio.avancar(Duration.ofSeconds(45));
        registrar(bia, partidaHorda(dificuldade, 70, numero, segundo)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.colocacao").value(2));
        String terceiro = bilhete(ana, dificuldade, numero);

        mvc.perform(get("/api/ranking").param("modo", "HORDA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/ranking").param("modo", "HORDA").param("horda", Long.toString(numero)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[1].nome").value("Bia"));
        relogio.agora = Instant.parse("2026-11-26T23:30:01Z");
        registrar(ana, partidaHorda(dificuldade, 75, numero, terceiro)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("HORDA_ENCERRADA"));
    }

    private String atual() throws Exception {
        return mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static long numero(String corpo) {
        return ((Number) JsonPath.read(corpo, "$.numero")).longValue();
    }

    private ResultActions emitir(String token, String dificuldade, String horda) throws Exception {
        return mvc.perform(post("/api/partidas/inicio").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modo\":\"HORDA\",\"dificuldade\":\"" + dificuldade + "\"" + horda + "}"));
    }

    private String bilhete(String token, String dificuldade, long numero) throws Exception {
        String corpo = emitir(token, dificuldade, ",\"horda\":" + numero).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.bilhete");
    }

    private static String partidaHorda(String dificuldade, int pontos, long horda, String bilhete) {
        String base = partida("VITORIA", dificuldade, pontos);
        return base.substring(0, base.length() - 1) + ",\"modo\":\"HORDA\",\"horda\":" + horda
                + (bilhete == null ? "" : ",\"bilhete\":\"" + bilhete + "\"") + "}";
    }
}
