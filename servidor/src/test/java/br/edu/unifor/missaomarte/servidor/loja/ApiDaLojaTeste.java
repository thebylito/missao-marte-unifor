package br.edu.unifor.missaomarte.servidor.loja;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiComMoedasTeste;
import br.edu.unifor.missaomarte.servidor.moedas.MovimentoDeMoedas;
import br.edu.unifor.missaomarte.servidor.moedas.Origem;
import br.edu.unifor.missaomarte.servidor.moedas.RepositorioDeMoedas;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Fase 8 (CT-14, CA-13): a loja de skins e o Hangar. O catálogo é público; comprar desconta o saldo, que nunca fica
 * negativo; a {@code padrao} é sempre do jogador; só se equipa uma skin que se tem. A compra é livre: não exige o
 * nível anterior da linha.
 */
class ApiDaLojaTeste extends ApiComMoedasTeste {

    @Autowired
    private ServicoDeMoedas moedas;

    @Autowired
    private RepositorioDeMoedas extrato;

    private ResultActions hangar(String token) throws Exception {
        return mvc.perform(get("/api/jogadores/eu/hangar").header("Authorization", "Bearer " + token));
    }

    private ResultActions comprar(String token, String skin) throws Exception {
        return mvc.perform(post("/api/loja/compras").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"" + skin + "\"}"));
    }

    private ResultActions equipar(String token, String skin) throws Exception {
        return mvc.perform(put("/api/jogadores/eu/skin").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"" + skin + "\"}"));
    }

    /** Um jogador novo com este saldo (um prêmio de semana de mentira, para não jogar dezenas de partidas). */
    private String jogadorCom(String nome, int saldo) throws Exception {
        String token = novoJogador(nome);
        moedas.creditar(idDe(token), Origem.SEMANA, "teste:" + nome, saldo);
        return token;
    }

    @Test
    void oCatalogoEhPublicoEVemEmLinhasENiveis() throws Exception {                 // CA-13
        mvc.perform(get("/api/loja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].id").value(contains("padrao", "padrao_2",
                        "padrao_3", "padrao_4", "padrao_5", "guara", "guara_2", "guara_3", "guara_4")))
                .andExpect(jsonPath("$[*].preco").value(contains(0, 60, 120, 200, 300, 60,
                        120, 200, 300)))
                .andExpect(jsonPath("$[0].nome").value("Padrão"))
                .andExpect(jsonPath("$[0].linha").value("padrao"))
                .andExpect(jsonPath("$[0].nivel").value(1))
                .andExpect(jsonPath("$[5].nome").value("Guará"))
                .andExpect(jsonPath("$[5].linha").value("guara"))
                .andExpect(jsonPath("$[8].nome").value("Guará IV"))
                .andExpect(jsonPath("$[8].nivel").value(4))
                .andExpect(jsonPath("$[0].raridade").doesNotExist());
    }

    @Test
    void oHangarAComprarEEquiparExigemToken() throws Exception {
        mvc.perform(get("/api/jogadores/eu/hangar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/loja/compras").contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"guara\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/jogadores/eu/skin").contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"guara\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oHangarDeUmJogadorNovoTemSoAPadrao() throws Exception {                   // CA-13
        hangar(novoJogador("Ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.skins").value(contains("padrao")))
                .andExpect(jsonPath("$.equipada").value("padrao"));
    }

    @Test
    void asMoedasDasVitoriasCompramUmaSkin() throws Exception {                     // CT-14, CA-13
        String ana = novoJogador("Ana");
        for (int i = 0; i < 15; i++) {
            vencer(ana, "PILOTAGEM", "DIFICIL");   // 15 x 4 = 60
        }
        hangar(ana).andExpect(jsonPath("$.saldo").value(60));

        comprar(ana, "guara")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.skins").value(contains("padrao", "guara")));
        hangar(ana)
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.skins").value(contains("padrao", "guara")))
                .andExpect(jsonPath("$.equipada").value("padrao"));   // comprar não equipa
    }

    @Test
    void aCompraDescontaOPrecoEFicaNoExtrato() throws Exception {                   // CT-14
        String ana = jogadorCom("Ana", 300);
        comprar(ana, "padrao_2").andExpect(status().isCreated()).andExpect(jsonPath("$.saldo").value(240));
        comprar(ana, "guara_2").andExpect(status().isCreated()).andExpect(jsonPath("$.saldo").value(120));

        List<MovimentoDeMoedas> compras = extrato.findAll().stream()
                .filter(m -> m.getJogador().getNome().equals("Ana") && m.getOrigem() == Origem.COMPRA).toList();
        assertEquals(List.of("skin:padrao_2:-60", "skin:guara_2:-120"),
                compras.stream().map(m -> m.getReferencia() + ":" + m.getValor()).toList());
        assertEquals(120, moedas.saldo(idDe(ana)));
    }

    @Test
    void aCompraNaoExigeONivelAnterior() throws Exception {                         // CA-13
        String ana = jogadorCom("Ana", 300);
        comprar(ana, "padrao_5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.skins").value(contains("padrao", "padrao_5")));
    }

    @Test
    void umaSkinDesconhecidaDa404() throws Exception {                              // CA-13
        String ana = jogadorCom("Ana", 300);
        comprar(ana, "bruma")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("bruma")));
        hangar(ana).andExpect(jsonPath("$.saldo").value(300));
    }

    @Test
    void comprarDeNovoDa409JaPossui() throws Exception {                            // CT-14, CA-13
        String ana = jogadorCom("Ana", 300);
        comprar(ana, "guara").andExpect(status().isCreated());

        comprar(ana, "guara")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.motivo").value("JA_POSSUI"))
                .andExpect(jsonPath("$.detail").value(containsString("Guará")));
        comprar(ana, "padrao")   // a padrão é sempre do jogador
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("JA_POSSUI"));
        hangar(ana).andExpect(jsonPath("$.saldo").value(240));
    }

    @Test
    void semSaldoDa409SaldoInsuficiente() throws Exception {                        // CT-14, CA-13
        String ana = jogadorCom("Ana", 299);
        comprar(ana, "padrao_5")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.motivo").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.saldo").value(299))
                .andExpect(jsonPath("$.preco").value(300))
                .andExpect(jsonPath("$.detail").value(containsString("saldo insuficiente")));
        hangar(ana)
                .andExpect(jsonPath("$.saldo").value(299))
                .andExpect(jsonPath("$.skins").value(contains("padrao")));

        hangar(novoJogador("Bia")).andExpect(jsonPath("$.saldo").value(0));
        comprar(novoJogador("Caio"), "guara").andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("SALDO_INSUFICIENTE"));
    }

    @Test
    void pedidoSemSkinDa400() throws Exception {
        String ana = novoJogador("Ana");
        mvc.perform(post("/api/loja/compras").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.skin").exists());
        mvc.perform(put("/api/jogadores/eu/skin").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void equiparUmaSkinQueOJogadorTem() throws Exception {                          // CA-13
        String ana = jogadorCom("Ana", 60);
        comprar(ana, "guara").andExpect(status().isCreated());

        equipar(ana, "guara").andExpect(status().isOk()).andExpect(jsonPath("$.equipada").value("guara"));
        hangar(ana).andExpect(jsonPath("$.equipada").value("guara"));

        equipar(ana, "padrao").andExpect(status().isOk()).andExpect(jsonPath("$.equipada").value("padrao"));
        hangar(ana).andExpect(jsonPath("$.equipada").value("padrao"));
    }

    @Test
    void equiparUmaSkinQueOJogadorNaoTemDa409() throws Exception {                  // CA-13
        String ana = jogadorCom("Ana", 300);
        equipar(ana, "padrao_5")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.motivo").value("NAO_POSSUI"))
                .andExpect(jsonPath("$.detail").value(containsString("Padrão V")));
        hangar(ana).andExpect(jsonPath("$.equipada").value("padrao"));
    }

    @Test
    void equiparUmaSkinDesconhecidaDa404() throws Exception {                       // CA-13
        String ana = novoJogador("Ana");
        equipar(ana, "bruma").andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        hangar(ana).andExpect(jsonPath("$.equipada").value("padrao"));
    }

    @Test
    void oHangarEhDeCadaJogador() throws Exception {
        String ana = jogadorCom("Ana", 60);
        String bia = novoJogador("Bia");
        comprar(ana, "guara").andExpect(status().isCreated());
        equipar(ana, "guara").andExpect(status().isOk());

        hangar(bia)
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.skins").value(contains("padrao")))
                .andExpect(jsonPath("$.equipada").value("padrao"));
        equipar(bia, "guara").andExpect(status().isConflict()).andExpect(jsonPath("$.motivo").value("NAO_POSSUI"));
    }
}
