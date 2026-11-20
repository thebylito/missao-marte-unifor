package br.edu.unifor.missaomarte.servidor.partida;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import org.junit.jupiter.api.Test;

/**
 * Fase 7 (CT-26 a CT-28): o modo de jogo. É opcional ao registrar (sem ele, clássico, como os clientes das Fases 5
 * e 6 mandam), cada modo tem o seu ranking e o histórico diz o modo de cada partida.
 */
class ApiDeModosTeste extends ApiTeste {

    private static String pilotagem(String resultado, int pontos) {
        return "{\"resultado\":\"" + resultado + "\",\"dificuldade\":\"MEDIO\",\"pontos\":" + pontos
                + ",\"passageiros\":3,\"movimentos\":6,\"tempoSegundos\":140,\"modo\":\"PILOTAGEM\"}";
    }

    @Test
    void semOModoAPartidaEhClassicaEComOModoEhDaPilotagem() throws Exception {      // CT-26
        String ana = novoJogador("Ana");
        registrar(ana, partida("VITORIA", "FACIL", 60))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modo").value("CLASSICO"));
        registrar(ana, pilotagem("VITORIA", 61))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modo").value("PILOTAGEM"));
    }

    @Test
    void modoInvalidoEhRecusado() throws Exception {                                // CT-26
        String ana = novoJogador("Ana");
        registrar(ana, pilotagem("VITORIA", 61).replace("PILOTAGEM", "CORRIDA"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ranking").param("modo", "CORRIDA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadaModoTemOSeuRanking() throws Exception {                                // CT-27
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        registrar(ana, partida("VITORIA", "FACIL", 60));
        registrar(bia, pilotagem("VITORIA", 75));

        mvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"));
        mvc.perform(get("/api/ranking").param("modo", "CLASSICO"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"));
        mvc.perform(get("/api/ranking").param("modo", "PILOTAGEM"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bia"))
                .andExpect(jsonPath("$[0].pontos").value(75));
    }

    @Test
    void oRankingDaPilotagemSegueAsMesmasRegras() throws Exception {                 // CT-27
        String ana = novoJogador("Ana");
        registrar(ana, pilotagem("DERROTA", 90));
        registrar(ana, pilotagem("VITORIA", 50));
        registrar(ana, pilotagem("VITORIA", 70));
        mvc.perform(get("/api/ranking").param("modo", "PILOTAGEM"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pontos").value(70));
    }

    @Test
    void oHistoricoDizOModoDeCadaPartida() throws Exception {                        // CT-28
        String ana = novoJogador("Ana");
        registrar(ana, partida("DERROTA", "DIFICIL", 0));
        relogio.avancar(java.time.Duration.ofMinutes(1));
        registrar(ana, pilotagem("ABORTADA", 0));
        mvc.perform(get("/api/partidas/minhas").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modo").value("PILOTAGEM"))
                .andExpect(jsonPath("$[1].modo").value("CLASSICO"));
    }
}
