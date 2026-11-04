package br.edu.unifor.missaomarte.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;

class RankingTeste {

    static RegistroRanking reg(String nome, int pontos) {
        return new RegistroRanking(nome, pontos, Dificuldade.MEDIO, 5, "2026-11-04 10:00:00", 30);
    }

    private static List<String> nomes(Ranking r) {
        return r.getRegistros().stream().map(RegistroRanking::nome).collect(Collectors.toList());
    }

    @Test
    void comVagaQualquerPontuacaoEntra() {
        assertTrue(Ranking.vazio().entraNoTop(1), "ranking vazio");
        Ranking quatro = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20)));
        assertTrue(quatro.entraNoTop(1), "ainda há vaga");
    }

    @Test
    void cheioSoEntraQuemSuperaOUltimo() {
        Ranking cheio = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20), reg("e", 10)));
        assertTrue(cheio.entraNoTop(11), "supera o 5º");
        assertFalse(cheio.entraNoTop(10), "empatar com o 5º não basta");
        assertFalse(cheio.entraNoTop(5), "abaixo do 5º");
    }

    @Test
    void incluirOrdenaELimitaACinco() {
        Ranking cheio = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20), reg("e", 10)));
        Ranking novo = cheio.com(reg("novo", 35));
        assertEquals(List.of("a", "b", "novo", "c", "d"), nomes(novo));
        assertEquals(5, cheio.getRegistros().size(), "o original não muda");
    }

    @Test
    void empateMantemQuemChegouPrimeiro() {
        Ranking r = new Ranking(List.of(reg("antigo", 30))).com(reg("novo", 30));
        assertEquals(List.of("antigo", "novo"), nomes(r));
    }

    @Test
    void liderEhOPrimeiro() {
        assertFalse(Ranking.vazio().getLider().isPresent(), "vazio não tem líder");
        Ranking r = Ranking.vazio().com(reg("b", 10)).com(reg("a", 90));
        assertEquals("a", r.getLider().orElseThrow().nome());
    }

    @Test
    void ordenarColocaAMaiorPontuacaoPrimeiro() {
        List<RegistroRanking> ordenados = Ranking.ordenar(List.of(reg("x", 1), reg("y", 9), reg("z", 5)));
        assertEquals(List.of(9, 5, 1), ordenados.stream().map(RegistroRanking::pontos).collect(Collectors.toList()));
    }
}
