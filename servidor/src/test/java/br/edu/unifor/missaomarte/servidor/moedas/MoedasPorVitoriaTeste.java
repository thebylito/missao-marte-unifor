package br.edu.unifor.missaomarte.servidor.moedas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.servidor.ApiComMoedasTeste;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fase 8 (CT-14): quanto vale cada vitória. Clássico 1 (até 20 por dia de Fortaleza), Livre 3 e Livre Difícil 4;
 * derrota e abortada não dão nada, e as partidas já gravadas não contam (sem retroativo).
 */
class MoedasPorVitoriaTeste extends ApiComMoedasTeste {

    @Autowired
    private ServicoDeMoedas moedas;

    @Autowired
    private RepositorioDeMoedas extrato;

    @Test
    void aVitoriaNoClassicoDa1() throws Exception {                                 // CT-14
        String ana = novoJogador("Ana");
        for (String dificuldade : List.of("FACIL", "MEDIO", "DIFICIL")) {
            assertEquals(1, vencer(ana, "CLASSICO", dificuldade), dificuldade);
        }
        assertEquals(3, moedas.saldo(idDe(ana)));
    }

    @Test
    void aVitoriaNoLivreDa3() throws Exception {                                    // CT-14
        String ana = novoJogador("Ana");
        assertEquals(3, vencer(ana, "PILOTAGEM", "FACIL"));
        assertEquals(3, vencer(ana, "PILOTAGEM", "MEDIO"));
        assertEquals(6, moedas.saldo(idDe(ana)));
    }

    @Test
    void aVitoriaNoLivreDificilDa4() throws Exception {                             // CT-14
        String ana = novoJogador("Ana");
        assertEquals(4, vencer(ana, "PILOTAGEM", "DIFICIL"));
    }

    @Test
    void derrotaEAbortadaNaoDaoMoedas() throws Exception {                          // CT-14
        String ana = novoJogador("Ana");
        for (String modo : List.of("CLASSICO", "PILOTAGEM")) {
            assertEquals(0, jogar(ana, "DERROTA", modo, "DIFICIL"), modo);
            assertEquals(0, jogar(ana, "ABORTADA", modo, "DIFICIL"), modo);
        }
        assertEquals(0, moedas.saldo(idDe(ana)));
    }

    @Test
    void oClassicoTemTetoDe20PorDiaDeFortaleza() throws Exception {                 // CT-14
        relogio.agora = Instant.parse("2026-11-26T12:00:00Z");   // 09:00 em Fortaleza (UTC-3)
        String ana = novoJogador("Ana");
        for (int i = 1; i <= 20; i++) {
            assertEquals(1, vencer(ana, "CLASSICO", "FACIL"), "vitória " + i);
        }
        assertEquals(0, vencer(ana, "CLASSICO", "FACIL"), "a 21ª passa do teto");

        // 00:30 UTC já é outro dia em Greenwich, mas em Fortaleza ainda são 21:30 do mesmo dia
        relogio.agora = Instant.parse("2026-11-27T00:30:00Z");
        assertEquals(0, vencer(ana, "CLASSICO", "FACIL"));
        relogio.agora = Instant.parse("2026-11-27T02:59:50Z");   // 23:59:55 em Fortaleza, depois dos 5 s de voo
        assertEquals(0, vencer(ana, "CLASSICO", "FACIL"));

        relogio.agora = Instant.parse("2026-11-27T03:00:00Z");   // meia-noite em Fortaleza: o teto volta a zero
        assertEquals(1, vencer(ana, "CLASSICO", "FACIL"));
        assertEquals(21, moedas.saldo(idDe(ana)));
    }

    @Test
    void oTetoDoClassicoNaoLimitaOLivre() throws Exception {                        // CT-14
        relogio.agora = Instant.parse("2026-11-26T12:00:00Z");
        String ana = novoJogador("Ana");
        for (int i = 1; i <= 20; i++) {
            vencer(ana, "CLASSICO", "FACIL");
        }
        assertEquals(0, vencer(ana, "CLASSICO", "FACIL"));
        assertEquals(3, vencer(ana, "PILOTAGEM", "MEDIO"));
    }

    @Test
    void oTetoEhDeCadaJogador() throws Exception {                                  // CT-14
        relogio.agora = Instant.parse("2026-11-26T12:00:00Z");
        String ana = novoJogador("Ana");
        String bia = novoJogador("Bia");
        for (int i = 1; i <= 20; i++) {
            vencer(ana, "CLASSICO", "FACIL");
        }
        assertEquals(1, vencer(bia, "CLASSICO", "FACIL"));
    }

    @Test
    void semRetroativo() throws Exception {                                         // CT-14
        String ana = novoJogador("Ana");
        // partidas de um cliente 7.x, sem bilhete: entram, mas não valem moedas nem depois
        for (int i = 0; i < 3; i++) {
            registrar(ana, partida("VITORIA", "PILOTAGEM", "DIFICIL", 140, null)).andExpect(status().isCreated());
        }
        assertEquals(0, moedas.saldo(idDe(ana)));

        assertEquals(4, vencer(ana, "PILOTAGEM", "DIFICIL"));
        assertEquals(4, moedas.saldo(idDe(ana)));
    }

    @Test
    void oCreditoDaVitoriaReferenciaAPartida() throws Exception {                   // CT-14
        String ana = novoJogador("Ana");
        String codigo = bilhete(ana, "PILOTAGEM", "MEDIO");
        relogio.avancar(Duration.ofSeconds(45));
        String corpo = registrar(ana, partida("VITORIA", "PILOTAGEM", "MEDIO", 40, codigo))
                .andExpect(jsonPath("$.saldo").value(3)).andReturn().getResponse().getContentAsString();
        long partida = ((Number) JsonPath.read(corpo, "$.id")).longValue();

        List<MovimentoDeMoedas> movimentos = extrato.findAll().stream()
                .filter(m -> m.getJogador().getNome().equals("Ana")).toList();
        assertEquals(1, movimentos.size());
        assertEquals(Origem.VITORIA_LIVRE, movimentos.get(0).getOrigem());
        assertEquals("partida:" + partida, movimentos.get(0).getReferencia());
        assertEquals(3, movimentos.get(0).getValor());
    }
}
