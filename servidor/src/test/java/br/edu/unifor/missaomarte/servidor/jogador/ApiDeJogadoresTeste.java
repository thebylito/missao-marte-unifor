package br.edu.unifor.missaomarte.servidor.jogador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiTeste;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Cadastro, login e token: fluxos válidos e inválidos (issue #7). */
@ExtendWith(OutputCaptureExtension.class)
class ApiDeJogadoresTeste extends ApiTeste {

    @Autowired
    RepositorioDeJogadores jogadores;

    @Autowired
    PasswordEncoder senhas;

    @Test
    void cadastroValidoDevolve201ESemNadaDaSenha() throws Exception {
        cadastrar("Ana", "segredo1")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/jogadores/")))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void aSenhaEhGuardadaComoHashBcryptNuncaEmTextoPuro() throws Exception {
        cadastrar("Ana", "segredo1").andExpect(status().isCreated());
        Jogador ana = jogadores.findByNomeIgnoreCase("Ana").orElseThrow();
        assertThat(ana.getSenhaHash()).doesNotContain("segredo1").startsWith("$2");
        assertThat(senhas.matches("segredo1", ana.getSenhaHash())).isTrue();
    }

    @Test
    void nomeRepetidoMesmoComOutrasMaiusculasDevolve409() throws Exception {
        cadastrar("Ana", "segredo1").andExpect(status().isCreated());
        cadastrar("ANA", "outra-senha")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("o nome \"ANA\" já está em uso"));
    }

    @Test
    void dadosInvalidosDevolvem400ComOProblemaDeCadaCampo() throws Exception {
        cadastrar("x", "1")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("dados inválidos"))
                .andExpect(jsonPath("$.campos.nome").value("o nome deve ter de 3 a 20 caracteres"))
                .andExpect(jsonPath("$.campos.senha").value("a senha deve ter de 6 a 72 caracteres"));
        cadastrar("<script>", "segredo1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
        postJson("/api/jogadores", "{\"nome\":\"Ana\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("corpo do pedido ausente ou com JSON inválido"));
        postJson("/api/jogadores", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value("informe o nome"));
    }

    @Test
    void loginCertoDevolveUmTokenQueAbreOPerfil() throws Exception {
        cadastrar("Ana", "segredo1");
        postJson("/api/sessoes", "{\"nome\":\"ana\",\"senha\":\"segredo1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.jogador.nome").value("Ana"));
        String token = entrar("Ana", "segredo1");
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    void senhaErradaENomeInexistenteDaoAMesmaResposta401() throws Exception {
        cadastrar("Ana", "segredo1");
        for (String json : new String[] {"{\"nome\":\"Ana\",\"senha\":\"errada!\"}",
                "{\"nome\":\"Ninguem\",\"senha\":\"segredo1\"}"}) {
            postJson("/api/sessoes", json)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("nome ou senha incorretos"));
        }
    }

    @Test
    void rotaProtegidaSemTokenOuComTokenRuimDevolve401() throws Exception {
        String token = novoJogador("Ana");
        mvc.perform(get("/api/jogadores/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer nao-eh-um-token"))
                .andExpect(status().isUnauthorized());
        String adulterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAindaValeDiasDepoisDoLogin() throws Exception {
        // DA-38: o celular guarda a sessão; com 2 h, ela vencia antes de o jogador voltar ao jogo
        String token = novoJogador("Ana");
        relogio.avancar(Duration.ofDays(29));
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        String token = novoJogador("Ana");
        relogio.avancar(Duration.ofDays(30).plusMinutes(5));      // validade de 30 dias (+ 1 min de tolerância)
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oLogNaoMostraASenhaNemOToken(CapturedOutput saida) throws Exception {
        cadastrar("Ana", "senha-super-secreta");
        String token = entrar("Ana", "senha-super-secreta");
        mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token));
        postJson("/api/sessoes", "{\"nome\":\"Ana\",\"senha\":\"tentativa-errada\"}");
        assertThat(saida.getAll()).doesNotContain("senha-super-secreta").doesNotContain("tentativa-errada")
                .doesNotContain(token);
    }
}
