package br.edu.unifor.missaomarte.servidor.partida;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiComMoedasTeste;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fase 8 (CT-13): o bilhete de partida. Uma vitória só rende moedas se a partida começou com um bilhete do
 * servidor, do mesmo jogador, modo e dificuldade, ainda ativo, dentro das 2 h e com tempo real suficiente. Sem
 * bilhete (clientes 7.x) ou com um bilhete inválido, a partida entra como sempre, sem moedas e sem erro.
 */
class ApiDeBilhetesTeste extends ApiComMoedasTeste {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void oBilheteTemCodigoEValeDuasHoras() throws Exception {                       // CT-13
        String ana = novoJogador("Ana");
        String corpo = iniciar(ana, "{\"modo\":\"PILOTAGEM\",\"dificuldade\":\"MEDIO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bilhete").isString())
                .andExpect(jsonPath("$.validoAte").isString())
                .andReturn().getResponse().getContentAsString();
        assertDoesNotThrow(() -> UUID.fromString(JsonPath.read(corpo, "$.bilhete")));
        assertEquals(relogio.agora.plus(Duration.ofHours(2)), Instant.parse(JsonPath.read(corpo, "$.validoAte")));
    }

    @Test
    void pedirOBilheteExigeTokenEDificuldade() throws Exception {
        mvc.perform(post("/api/partidas/inicio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"PILOTAGEM\",\"dificuldade\":\"MEDIO\"}"))
                .andExpect(status().isUnauthorized());
        String ana = novoJogador("Ana");
        iniciar(ana, "{\"modo\":\"PILOTAGEM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dificuldade").exists());
        iniciar(ana, "{\"modo\":\"CORRIDA\",\"dificuldade\":\"MEDIO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void semModoOBilheteEhDoClassico() throws Exception {                           // CT-13
        String ana = novoJogador("Ana");
        String corpo = iniciar(ana, "{\"dificuldade\":\"FACIL\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(corpo, "$.bilhete");
        relogio.avancar(Duration.ofSeconds(5));

        assertEquals(1, moedasGanhas(ana, partida("VITORIA", "CLASSICO", "FACIL", 8, codigo)));
    }

    @Test
    void semBilheteAPartidaEntraComoSempreESemMoedas() throws Exception {           // CT-13
        String ana = novoJogador("Ana");
        registrar(ana, partida("VITORIA", "PILOTAGEM", "DIFICIL", 140, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moedasGanhas").value(0))
                .andExpect(jsonPath("$.saldo").value(0));

        mvc.perform(get("/api/ranking").param("modo", "PILOTAGEM"))
                .andExpect(jsonPath("$[0].nome").value("Ana"));
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aRespostaDoRegistroMantemOsCamposEGanhaMoedasESaldo() throws Exception {
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(130));
        registrar(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 140, codigo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.resultado").value("VITORIA"))
                .andExpect(jsonPath("$.dificuldade").value("MEDIO"))
                .andExpect(jsonPath("$.pontos").value(40))
                .andExpect(jsonPath("$.passageiros").value(3))
                .andExpect(jsonPath("$.movimentos").value(12))
                .andExpect(jsonPath("$.tempoSegundos").value(140))
                .andExpect(jsonPath("$.registradaEm").isString())
                .andExpect(jsonPath("$.modo").value("PILOTAGEM"))
                .andExpect(jsonPath("$.moedasGanhas").value(3))
                .andExpect(jsonPath("$.saldo").value(3));

        // o histórico não muda: nem o bilhete nem as moedas aparecem nele
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$[0].modo").value("PILOTAGEM"))
                .andExpect(jsonPath("$[0].moedasGanhas").doesNotExist())
                .andExpect(jsonPath("$[0].saldo").doesNotExist())
                .andExpect(jsonPath("$[0].bilhete").doesNotExist());
    }

    @Test
    void oBilheteSoValeUmaVez() throws Exception {                                  // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(3, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo)));

        relogio.avancar(Duration.ofSeconds(45));
        registrar(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo))
                .andExpect(status().isCreated())   // a partida entra, mas o bilhete já foi usado
                .andExpect(jsonPath("$.moedasGanhas").value(0))
                .andExpect(jsonPath("$.saldo").value(3));
    }

    @Test
    void aDerrotaTambemGastaOBilhete() throws Exception {                           // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(0, moedasGanhas(ana, partida("DERROTA", "PILOTAGEM", "MEDIO", 40, codigo)));

        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo)));
    }

    @Test
    void oBilheteDeOutroJogadorNaoRendeENaoEhGasto() throws Exception {             // CT-13
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        String daBia = bilhete(bia, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));

        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, daBia)));
        // ninguém queima o bilhete do outro: a dona ainda pode usá-lo
        assertEquals(3, moedasGanhas(bia, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, daBia)));
    }

    @Test
    void oBilheteValeAteDuasHoras() throws Exception {                              // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofHours(2));
        assertEquals(3, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo)));
    }

    @Test
    void oBilheteVencidoNaoRende() throws Exception {                               // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofHours(2).plusSeconds(1));
        registrar(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moedasGanhas").value(0));
    }

    @Test
    void oModoTrocadoNaoRendeEGastaOBilhete() throws Exception {                    // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "CLASSICO", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo)));

        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "CLASSICO", "MEDIO", 8, codigo)));
    }

    @Test
    void aDificuldadeTrocadaNaoRende() throws Exception {                           // CT-13
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "DIFICIL", 40, codigo)));
    }

    @Test
    void oClassicoPrecisaDePeloMenos5SegundosReais() throws Exception {             // CT-13
        String ana = novoJogador("Ana");
        String curto = bilhete(ana, "CLASSICO", "FACIL");
        relogio.avancar(Duration.ofSeconds(4));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "CLASSICO", "FACIL", 8, curto)));

        String certo = bilhete(ana, "CLASSICO", "FACIL");
        relogio.avancar(Duration.ofSeconds(5));
        assertEquals(1, moedasGanhas(ana, partida("VITORIA", "CLASSICO", "FACIL", 8, certo)));
    }

    @Test
    void aPilotagemPrecisaDePeloMenos45SegundosReais() throws Exception {           // CT-13
        String ana = novoJogador("Ana");
        String curto = bilhete(ana, "PILOTAGEM", "FACIL");
        relogio.avancar(Duration.ofSeconds(44));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "FACIL", 40, curto)));

        String certo = bilhete(ana, "PILOTAGEM", "FACIL");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(3, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "FACIL", 40, certo)));
    }

    @Test
    void oTempoRealAcompanhaOTempoDaPartidaMenos10Segundos() throws Exception {     // CT-13
        String ana = novoJogador("Ana");
        String curto = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(129));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 140, curto)));

        String certo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(130));
        assertEquals(3, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 140, certo)));
    }

    @Test
    void pedirOutroBilheteAnulaOAnterior() throws Exception {                       // CT-13
        String ana = novoJogador("Ana");
        String primeiro = bilhete(ana, "PILOTAGEM", "MEDIO");
        String segundo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));

        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, primeiro)));
        assertEquals(3, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, segundo)));
    }

    @Test
    void umBilheteQueNaoExisteNaoRendeENaoDaErro() throws Exception {               // CT-13
        String ana = novoJogador("Ana");
        relogio.avancar(Duration.ofSeconds(45));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40,
                UUID.randomUUID().toString())));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, "nao-e-um-bilhete")));
        assertEquals(0, moedasGanhas(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, "")));
    }

    @Test
    void asColunasDeEnumDoBilheteSaoTextoSemListaFechada() {                       // DA-40
        for (String coluna : List.of("MODO", "DIFICULDADE", "ESTADO")) {
            assertEquals("CHARACTER VARYING", jdbc.queryForObject("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'BILHETES' AND COLUMN_NAME = ?",
                    String.class, coluna), coluna);
        }
        Integer restricoes = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'BILHETES' AND CONSTRAINT_TYPE = 'CHECK'",
                Integer.class);
        assertEquals(0, restricoes);
    }
}
