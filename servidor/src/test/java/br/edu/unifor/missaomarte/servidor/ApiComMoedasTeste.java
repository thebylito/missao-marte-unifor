package br.edu.unifor.missaomarte.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Fase 8: o que os testes de bilhete, moedas e loja têm em comum. Pedir o bilhete, montar uma partida que cita o
 * bilhete e jogar uma vitória "de verdade" (bilhete, o tempo mínimo de voo e o registro).
 */
public abstract class ApiComMoedasTeste extends ApiTeste {

    protected ResultActions iniciar(String token, String json) throws Exception {
        return mvc.perform(post("/api/partidas/inicio").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Pede um bilhete para o modo e a dificuldade; devolve o código. */
    protected String bilhete(String token, String modo, String dificuldade) throws Exception {
        String corpo = iniciar(token, "{\"modo\":\"" + modo + "\",\"dificuldade\":\"" + dificuldade + "\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.bilhete");
    }

    /** Uma partida com modo, tempo simulado e bilhete ({@code null} = sem o campo, como os clientes 7.x). */
    protected static String partida(String resultado, String modo, String dificuldade, long tempoSegundos,
                                    String bilhete) {
        return "{\"resultado\":\"" + resultado + "\",\"dificuldade\":\"" + dificuldade + "\",\"pontos\":40,"
                + "\"passageiros\":3,\"movimentos\":12,\"tempoSegundos\":" + tempoSegundos + ",\"modo\":\"" + modo
                + "\"" + (bilhete == null ? "" : ",\"bilhete\":\"" + bilhete + "\"") + "}";
    }

    /** Registra e devolve as moedas ganhas ({@code moedasGanhas} da resposta). */
    protected int moedasGanhas(String token, String json) throws Exception {
        String corpo = registrar(token, json).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(corpo, "$.moedasGanhas");
    }

    /** O tempo real mínimo de cada modo, para uma partida curta: 5 s no Clássico e 45 s na Pilotagem. */
    protected static Duration minimo(String modo) {
        return Duration.ofSeconds(modo.equals("CLASSICO") ? 5 : 45);
    }

    /** Uma partida inteira: pede o bilhete, "voa" o tempo mínimo e registra. Devolve as moedas ganhas. */
    protected int jogar(String token, String resultado, String modo, String dificuldade) throws Exception {
        String codigo = bilhete(token, modo, dificuldade);
        relogio.avancar(minimo(modo));
        return moedasGanhas(token, partida(resultado, modo, dificuldade, 8, codigo));
    }

    protected int vencer(String token, String modo, String dificuldade) throws Exception {
        return jogar(token, "VITORIA", modo, dificuldade);
    }

    protected long idDe(String token) throws Exception {
        String corpo = mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }
}
