package br.edu.unifor.missaomarte.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base dos testes da API: o servidor inteiro (controladores, segurança, JPA) com H2 em memória e um relógio
 * controlado. Cada teste roda numa transação desfeita no fim, então um não vê os dados do outro.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:missaomarte-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais"})
@AutoConfigureMockMvc
@Transactional
@Import(ApiTeste.Configuracao.class)
public abstract class ApiTeste {

    /** Relógio que só anda quando o teste manda. */
    public static final class RelogioDeTeste extends Clock {
        public Instant agora;

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return agora; }

        public void avancar(Duration d) { agora = agora.plus(d); }
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected RelogioDeTeste relogio;

    @BeforeEach
    void zerarRelogio() {
        relogio.agora = Instant.now();
    }

    protected ResultActions postJson(String rota, String json) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions cadastrar(String nome, String senha) throws Exception {
        return postJson("/api/jogadores", "{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}");
    }

    protected String entrar(String nome, String senha) throws Exception {
        String corpo = postJson("/api/sessoes", "{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    /** Cadastra e entra; devolve o token. */
    protected String novoJogador(String nome) throws Exception {
        cadastrar(nome, "senha-" + nome).andExpect(status().isCreated());
        return entrar(nome, "senha-" + nome);
    }

    protected static String partida(String resultado, String dificuldade, int pontos) {
        return "{\"resultado\":\"" + resultado + "\",\"dificuldade\":\"" + dificuldade + "\",\"pontos\":" + pontos
                + ",\"passageiros\":4,\"movimentos\":20,\"tempoSegundos\":35}";
    }

    protected ResultActions registrar(String token, String json) throws Exception {
        return mvc.perform(post("/api/partidas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
