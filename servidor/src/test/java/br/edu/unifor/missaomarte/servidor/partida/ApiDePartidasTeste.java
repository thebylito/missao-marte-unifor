package br.edu.unifor.missaomarte.servidor.partida;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Registro de partidas e histórico isolado por jogador (issue #8). */
class ApiDePartidasTeste extends ApiTeste {

    @Test
    void registrarExigeToken() throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON)
                        .content(partida("VITORIA", "FACIL", 60)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void partidaRegistradaVoltaComIdEData() throws Exception {
        String ana = novoJogador("Ana");
        registrar(ana, partida("VITORIA", "MEDIO", 45))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.resultado").value("VITORIA"))
                .andExpect(jsonPath("$.dificuldade").value("MEDIO"))
                .andExpect(jsonPath("$.pontos").value(45))
                .andExpect(jsonPath("$.registradaEm").isString());
    }

    @Test
    void cadaUmVeSoOProprioHistorico() throws Exception {
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        registrar(ana, partida("VITORIA", "FACIL", 60));
        registrar(ana, partida("DERROTA", "DIFICIL", 0));
        registrar(bia, partida("ABORTADA", "MEDIO", 19));

        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + bia))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].resultado").value("ABORTADA"));
    }

    @Test
    void oCorpoNaoEscolheODonoDaPartida() throws Exception {
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        // um campo "jogador" no corpo é ignorado: o dono é sempre quem está no token
        registrar(ana, "{\"jogador\":\"Bia\",\"jogadorId\":2,\"resultado\":\"VITORIA\",\"dificuldade\":\"FACIL\","
                + "\"pontos\":99,\"passageiros\":4,\"movimentos\":10,\"tempoSegundos\":30}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + bia))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$[0].pontos").value(99));
    }

    @Test
    void historicoVemDaMaisRecenteParaAMaisAntiga() throws Exception {
        String ana = novoJogador("Ana");
        for (int pontos : new int[] {10, 20, 30}) {
            registrar(ana, partida("VITORIA", "FACIL", pontos));
            relogio.avancar(Duration.ofMinutes(1));
        }
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$[0].pontos").value(30))
                .andExpect(jsonPath("$[2].pontos").value(10));
    }

    @Test
    void historicoDevolveNoMaximoAs50MaisRecentes() throws Exception {
        String ana = novoJogador("Ana");
        for (int i = 1; i <= 52; i++) {
            registrar(ana, partida("DERROTA", "FACIL", i));
            relogio.avancar(Duration.ofSeconds(1));
        }
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.length()").value(ServicoDePartidas.LIMITE_DO_HISTORICO))
                .andExpect(jsonPath("$[0].pontos").value(52));
    }

    @Test
    void dadosInvalidosDevolvem400() throws Exception {
        String ana = novoJogador("Ana");
        registrar(ana, partida("VITORIA", "FACIL", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pontos").value("pontos não podem ser negativos"));
        registrar(ana, "{\"dificuldade\":\"FACIL\",\"pontos\":1,\"passageiros\":0,\"movimentos\":0,\"tempoSegundos\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.resultado").exists());
        registrar(ana, partida("GANHOU", "FACIL", 10))
                .andExpect(status().isBadRequest());
        registrar(ana, partida("VITORIA", "FACIL", 10_001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pontos").value("no máximo 10000 pontos"));
    }
}
