package br.edu.unifor.missaomarte.servidor.ajustes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.evento.EventoAoVivo;
import br.edu.unifor.missaomarte.servidor.evento.ServicoDeEventos;
import br.edu.unifor.missaomarte.servidor.horda.CatalogoDeCavernas;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Fase 8 (DA-48): o servidor sobe com algumas variáveis MISSAOMARTE_AJUSTE_* definidas (aqui, como propriedades do
 * teste, com os mesmos nomes). A Pilotagem Livre lê os ajustes em /api/ajustes; a horda guarda os dela.
 */
@TestPropertySource(properties = {
        "MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO=30",
        "MISSAOMARTE_AJUSTE_DIFICIL_TANQUE=120",
        "MISSAOMARTE_AJUSTE_METEOROS_MAX=300",
        "MISSAOMARTE_AJUSTE_DURACAO_TEMPESTADE=40"})
class ApiDeAjustesTeste extends ApiTeste {

    private static final Instant MEIO_DA_HORDA = Instant.parse("2026-11-26T21:30:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired ServicoDeEventos eventos;
    @Autowired CatalogoDeCavernas catalogo;

    @BeforeEach
    void noMeioDeUmaHorda() {
        relogio.agora = MEIO_DA_HORDA;
    }

    @Test
    void publicaOsAjustesSemLogin() throws Exception {
        mvc.perform(get("/api/ajustes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.medio.inclinacaoMaxima").value(30.0))
                .andExpect(jsonPath("$.facil.inclinacaoMaxima").value(25.0))
                .andExpect(jsonPath("$.dificil.tanque").value(120.0))
                .andExpect(jsonPath("$.dificil.quedaMaxima").value(65.0))
                .andExpect(jsonPath("$.eventos.velocidadeMinimaDosMeteoros").value(150.0))
                .andExpect(jsonPath("$.eventos.velocidadeMaximaDosMeteoros").value(300.0))
                .andExpect(jsonPath("$.eventos.duracaoDaTempestade").value(40.0))
                .andExpect(jsonPath("$.eventos.duracaoDaChuva").value(20.0));
    }

    @Test
    void aHordaSorteadaGuardaOsAjustesDoServidor() throws Exception {
        mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao.ajustes.medio.inclinacaoMaxima").value(30.0))
                .andExpect(jsonPath("$.descricao.ajustes.eventos.duracaoDaTempestade").value(40.0));

        String gravada = jdbc.queryForObject("select descricao from hordas where numero = ?", String.class,
                ServicoDeHordas.numeroEm(MEIO_DA_HORDA));
        assertThat(json.readValue(gravada, DescricaoDaHorda.class).ajustes().medio().inclinacaoMaxima())
                .isEqualTo(30);
    }

    /** Um deploy com ajustes novos no meio da horda não muda a horda: ela foi sorteada (e gravada) antes. */
    @Test
    void aHordaGravadaAntesDosAjustesContinuaComOPadrao() throws Exception {
        gravarHordaSemAjustes();

        mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao.ajustes.medio.inclinacaoMaxima").value(20.0))
                .andExpect(jsonPath("$.descricao.ajustes.eventos.duracaoDaTempestade").value(25.0));
        mvc.perform(get("/api/ajustes")).andExpect(jsonPath("$.medio.inclinacaoMaxima").value(30.0));
    }

    @Test
    void oEventoPedidoPeloPainelDuraOQueAHordaAtualDiz() {
        eventos.solicitar(TipoDeEvento.TEMPESTADE, relogio.instant());

        assertThat(duracaoDoEvento("TEMPESTADE")).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void naHordaGravadaAntesDosAjustesOEventoDoPainelDuraOPadrao() {
        gravarHordaSemAjustes();

        eventos.solicitar(TipoDeEvento.TEMPESTADE, relogio.instant());

        assertThat(duracaoDoEvento("TEMPESTADE")).isEqualTo(Duration.ofSeconds(25));
    }

    /**
     * Revisão do PR #33: o jogo publicado antes da DA-48 joga a horda com o padrão. Numa horda com ajustes diferentes,
     * ele não recebe o bilhete, e sem bilhete não entra no Top 10 da horda.
     */
    @Test
    void oJogoAntigoNaoRecebeBilheteDeUmaHordaComAjustes() throws Exception {
        String token = novoJogador("Antiga");
        String dificuldade = JsonPath.read(atual(), "$.dificuldade");
        long numero = ServicoDeHordas.numeroEm(MEIO_DA_HORDA);

        pedirBilhete(token, numero, dificuldade, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("JOGO_DESATUALIZADO"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Atualize o jogo")));
        pedirBilhete(token, numero, dificuldade, ",\"comAjustes\":true").andExpect(status().isCreated());
    }

    @Test
    void numaHordaComOPadraoOJogoAntigoRecebeOBilhete() throws Exception {
        gravarHordaSemAjustes();
        String token = novoJogador("Antiga");
        String dificuldade = JsonPath.read(atual(), "$.dificuldade");

        pedirBilhete(token, ServicoDeHordas.numeroEm(MEIO_DA_HORDA), dificuldade, "").andExpect(status().isCreated());
    }

    /** Revisão do PR #33: um evento encurtado continua ativo para o resultado do jogo antigo, que dura o padrão. */
    @Test
    void umEventoEncurtadoContinuaAtivoAteOPrazoDoJogoAntigo() {
        Instant inicio = MEIO_DA_HORDA.minusSeconds(30);
        jdbc.update("insert into eventos_ao_vivo (evento, tipo, semente, inicio, fim) values (?, ?, ?, ?, ?)",
                "e-curto", "TEMPESTADE", 7L, inicio.atOffset(ZoneOffset.UTC), inicio.plusSeconds(10).atOffset(ZoneOffset.UTC));

        assertThat(eventos.ativos(inicio.plusSeconds(39))).extracting(EventoAoVivo::id).contains("e-curto");
        assertThat(eventos.ativos(inicio.plusSeconds(41))).extracting(EventoAoVivo::id).doesNotContain("e-curto");
    }

    private String atual() throws Exception {
        return mvc.perform(get("/api/hordas/atual")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    private ResultActions pedirBilhete(String token, long numero, String dificuldade, String extra) throws Exception {
        return mvc.perform(post("/api/partidas/inicio").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"HORDA\",\"horda\":" + numero
                        + ",\"dificuldade\":\"" + dificuldade + "\"" + extra + "}"));
    }

    private Duration duracaoDoEvento(String tipo) {
        OffsetDateTime inicio = jdbc.queryForObject("select inicio from eventos_ao_vivo where tipo = ?",
                OffsetDateTime.class, tipo);
        OffsetDateTime fim = jdbc.queryForObject("select fim from eventos_ao_vivo where tipo = ?",
                OffsetDateTime.class, tipo);
        return Duration.between(inicio, fim);
    }

    /** Como um servidor de antes da DA-48 gravava a horda: a descrição sem o campo "ajustes". */
    private void gravarHordaSemAjustes() {
        long numero = ServicoDeHordas.numeroEm(MEIO_DA_HORDA);
        Instant inicio = ServicoDeHordas.inicioDe(numero);
        DescricaoDaHorda sorteada = SorteioDaHorda.sortear(numero, 7,
                List.of(catalogo.porId("marciana").orElseThrow()), null);
        ObjectNode descricao = (ObjectNode) json.valueToTree(sorteada);
        descricao.remove("ajustes");
        jdbc.update("""
                insert into hordas (numero, inicio, fim, semente, caverna, dificuldade, descricao, semente_agenda)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, numero, inicio.atOffset(ZoneOffset.UTC), inicio.plus(ServicoDeHordas.DURACAO).atOffset(ZoneOffset.UTC),
                7L, sorteada.caverna().id(), sorteada.dificuldade().name(), json.writeValueAsString(descricao), null);
    }
}
