package br.edu.unifor.missaomarte.servidor.horda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.evento.AgendaDeEventos;
import br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo;
import br.edu.unifor.missaomarte.servidor.evento.ServicoDeEventos;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@TestPropertySource(properties = "missaomarte.hordas.segredo=horas-pares-segredo-de-teste-com-32-bytes")
class ApiDaHordaTeste extends ApiTeste {

    @Autowired JdbcTemplate jdbc;
    @Autowired ServicoDeEventos eventos;
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
        assertThat(jdbc.queryForObject("select semente from hordas where numero = ?", Long.class, numero(antes)))
                .isEqualTo(-8087644188306387133L);
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

        Object descricaoOriginal = JsonPath.read(primeira, "$.descricao");
        assertThat(JsonPath.<Object>read(atual(), "$.descricao")).isEqualTo(descricaoOriginal);
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
        registrar(ana, partidaHorda(dificuldade, 74, numero, bilhete)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select origem from moedas", String.class)).isEqualTo("VITORIA_HORDA");
        assertThat(jdbc.queryForObject("select horda_numero from partidas", Long.class)).isEqualTo(numero);
    }

    @Test
    void agendaAutomaticaNaoSegueOSorteioDaDescricaoPublicada() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T21:00:00Z");
        String resposta = atual();
        long numero = numero(resposta);
        Instant inicio = Instant.parse(JsonPath.read(resposta, "$.inicio"));
        assertThat(JsonPath.<List<Number>>read(resposta, "$.descricao.roteiro[*].semente")).isNotEmpty();
        assertThat(resposta).doesNotContain("sementeAgenda", "semente_agenda");
        // Semente conhecida da descrição pública desta fixture.
        var previsao = AgendaDeEventos.daHorda(numero, inicio, -8087644188306387133L);
        for (int segundo = 0; segundo < 7200; segundo += 10) {
            eventos.publicarAutomaticos(inicio.plusSeconds(segundo));
        }

        var gravados = jdbc.query("""
                select evento, tipo, semente, inicio, fim from eventos_ao_vivo order by inicio
                """, (r, i) -> new EventoAoVivo(r.getString("evento"),
                TipoDeEvento.valueOf(r.getString("tipo")),
                r.getLong("semente"), r.getObject("inicio", OffsetDateTime.class).toInstant(),
                r.getObject("fim", OffsetDateTime.class).toInstant(), null, null));
        assertThat(gravados).hasSizeBetween(2, 3);
        assertThat(gravados.stream().map(e -> List.of(e.inicio(), e.tipo())).toList())
                .isNotEqualTo(previsao.stream().map(e -> List.of(e.inicio(), e.tipo())).toList());
        assertThat(gravados.stream().map(e -> e.semente()).toList())
                .doesNotContainAnyElementsOf(previsao.stream().map(e -> e.semente()).toList());
    }

    @Test
    void hordaPublicadaPelaVersaoAnteriorConservaAgendaEEventos() throws Exception {
        relogio.agora = Instant.parse("2026-11-28T15:00:00Z");
        String resposta = atual();
        long numero = numero(resposta);
        long semente = jdbc.queryForObject("select semente from hordas where numero = ?", Long.class, numero);
        jdbc.update("update hordas set semente_agenda = null where numero = ?", numero);
        var esperado = AgendaDeEventos.daHorda(numero, relogio.instant(), semente).get(0);

        eventos.publicarAutomaticos(esperado.aviso());
        assertThat(eventos.ativos(esperado.inicio())).containsExactly(esperado);
        assertThat(jdbc.queryForObject("select semente_agenda from hordas where numero = ?", String.class, numero))
                .isNull();
    }

    @Test
    void repetirOMesmoBilheteDevolveARespostaOriginalMesmoDepoisDaHorda() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T22:00:00Z");
        String atual = atual();
        long numero = numero(atual);
        String dificuldade = JsonPath.read(atual, "$.dificuldade");
        String ana = novoJogador("Ana");
        String registro = partidaHorda(dificuldade, 70, numero, bilhete(ana, dificuldade, numero));
        relogio.avancar(Duration.ofSeconds(45));
        String original = respostaAoRegistrar(ana, registro);
        assertThat(respostaAoRegistrar(ana, registro)).isEqualTo(original);

        String bia = novoJogador("Bia");
        String melhor = partidaHorda(dificuldade, 75, numero, bilhete(bia, dificuldade, numero));
        registrar(bia, registro).andExpect(status().isBadRequest());
        String segundaDaAna = partidaHorda(dificuldade, 69, numero, bilhete(ana, dificuldade, numero));
        relogio.avancar(Duration.ofSeconds(45));
        respostaAoRegistrar(bia, melhor);
        registrar(ana, segundaDaAna).andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(10)).andExpect(jsonPath("$.colocacao").value(2));
        relogio.avancar(Duration.ofHours(3));
        assertThat(respostaAoRegistrar(ana, registro)).isEqualTo(original);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void duasChamadasSimultaneasComMesmoBilheteGravamUmaPartidaECreditamUmaVez() throws Exception {
        relogio.agora = Instant.parse("2026-10-04T10:00:00Z");
        String atual = atual();
        long numero = numero(atual);
        String dificuldade = JsonPath.read(atual, "$.dificuldade");
        String nome = "PilotoSimultaneo";
        String token = novoJogador(nome);
        String registro = partidaHorda(dificuldade, 75, numero, bilhete(token, dificuldade, numero));
        relogio.avancar(Duration.ofSeconds(45));
        var barreira = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        Callable<String> chamada = () -> {
            barreira.await(5, TimeUnit.SECONDS);
            return respostaAoRegistrar(token, registro);
        };
        try {
            var primeira = executor.submit(chamada);
            var segunda = executor.submit(chamada);
            assertThat(primeira.get(10, TimeUnit.SECONDS)).isEqualTo(segunda.get(10, TimeUnit.SECONDS));
            assertThat(jdbc.queryForObject("select count(*) from partidas where horda_numero = ?",
                    Integer.class, numero)).isEqualTo(1);
            assertThat(jdbc.queryForList("select valor from moedas m join jogadores j on j.id = m.jogador_id "
                    + "where j.nome = ?", Integer.class, nome)).containsExactly(5);
        } finally {
            executor.shutdownNow();
            jdbc.update("delete from moedas where jogador_id in (select id from jogadores where nome = ?)", nome);
            jdbc.update("delete from bilhetes where jogador_id in (select id from jogadores where nome = ?)", nome);
            jdbc.update("delete from partidas where horda_numero = ?", numero);
            jdbc.update("delete from jogadores where nome = ?", nome);
            jdbc.update("delete from hordas where numero = ?", numero);
        }
    }

    private String respostaAoRegistrar(String token, String registro) throws Exception {
        return registrar(token, registro).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
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
        String nova = atual();
        registrar(ana, partidaHorda(JsonPath.read(nova, "$.dificuldade"), 70, numero(nova), bilhete))
                .andExpect(status().isBadRequest());
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

    @Test
    void registraNoLimiteDaJanelaEDevolveColocacaoNulaSemVitoria() throws Exception {
        relogio.agora = Instant.parse("2026-11-26T22:59:00Z");
        String atual = atual();
        long numero = numero(atual);
        String dificuldade = JsonPath.read(atual, "$.dificuldade");
        String ana = novoJogador("Ana");
        String bilhete = bilhete(ana, dificuldade, numero);
        relogio.agora = Instant.parse("2026-11-26T23:30:00Z");

        String corpo = registrar(ana, partidaHorda(dificuldade, 0, numero, bilhete).replace("VITORIA", "DERROTA"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.moedasGanhas").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<java.util.Map<String, Object>>read(corpo, "$")).containsEntry("colocacao", null);
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
