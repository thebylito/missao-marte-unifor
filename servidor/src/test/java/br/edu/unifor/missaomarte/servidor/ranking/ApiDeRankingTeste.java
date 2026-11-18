package br.edu.unifor.missaomarte.servidor.ranking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Top 10: ordenação, melhor vitória de cada jogador, empate e limite (issue #8). */
class ApiDeRankingTeste extends ApiTeste {

    @Test
    void ehPublicoEComecaVazio() throws Exception {
        mvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void soVitoriasContam() throws Exception {
        String ana = novoJogador("Ana");
        registrar(ana, partida("DERROTA", "DIFICIL", 90));
        registrar(ana, partida("ABORTADA", "DIFICIL", 80));
        registrar(ana, partida("VITORIA", "FACIL", 40));
        String bia = novoJogador("Bia");
        registrar(bia, partida("DERROTA", "FACIL", 70));

        mvc.perform(get("/api/ranking"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].pontos").value(40));
    }

    @Test
    void cadaJogadorApareceUmaVezComAMelhorPontuacao() throws Exception {
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        registrar(ana, partida("VITORIA", "FACIL", 30));
        registrar(ana, partida("VITORIA", "DIFICIL", 50));
        registrar(bia, partida("VITORIA", "MEDIO", 45));
        registrar(ana, partida("VITORIA", "MEDIO", 20));

        mvc.perform(get("/api/ranking"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].posicao").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].pontos").value(50))
                .andExpect(jsonPath("$[0].dificuldade").value("DIFICIL"))
                .andExpect(jsonPath("$[1].posicao").value(2))
                .andExpect(jsonPath("$[1].nome").value("Bia"))
                .andExpect(jsonPath("$[1].pontos").value(45));
    }

    @Test
    void noEmpateFicaNaFrenteQuemFezAPontuacaoPrimeiro() throws Exception {
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        registrar(bia, partida("VITORIA", "FACIL", 50));
        relogio.avancar(Duration.ofMinutes(5));
        registrar(ana, partida("VITORIA", "FACIL", 50));

        mvc.perform(get("/api/ranking"))
                .andExpect(jsonPath("$[0].nome").value("Bia"))
                .andExpect(jsonPath("$[1].nome").value("Ana"));
    }

    @Test
    void mostraNoMaximo10JogadoresDaMaiorParaAMenor() throws Exception {
        for (int i = 1; i <= 12; i++) {
            String token = novoJogador(String.format("Piloto%02d", i));
            registrar(token, partida("VITORIA", "MEDIO", i * 10));
        }
        mvc.perform(get("/api/ranking"))
                .andExpect(jsonPath("$.length()").value(ServicoDeRanking.TAMANHO))
                .andExpect(jsonPath("$[0].nome").value("Piloto12"))
                .andExpect(jsonPath("$[0].pontos").value(120))
                .andExpect(jsonPath("$[9].posicao").value(10))
                .andExpect(jsonPath("$[9].nome").value("Piloto03"))
                .andExpect(jsonPath("$[9].pontos").value(30));
    }
}
