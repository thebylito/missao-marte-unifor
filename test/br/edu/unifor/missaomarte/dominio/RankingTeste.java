package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.teste.Teste;
import java.util.List;
import java.util.stream.Collectors;

class RankingTeste {

    static RegistroRanking reg(String nome, int pontos) {
        return new RegistroRanking(nome, pontos, Dificuldade.MEDIO, 5, "2026-11-04 10:00:00", 30);
    }

    private static List<String> nomes(Ranking r) {
        return r.getRegistros().stream().map(RegistroRanking::nome).collect(Collectors.toList());
    }

    @Teste
    void comVagaQualquerPontuacaoEntra() {
        verdadeiro(Ranking.vazio().entraNoTop(1), "ranking vazio");
        Ranking quatro = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20)));
        verdadeiro(quatro.entraNoTop(1), "ainda há vaga");
    }

    @Teste
    void cheioSoEntraQuemSuperaOUltimo() {
        Ranking cheio = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20), reg("e", 10)));
        verdadeiro(cheio.entraNoTop(11), "supera o 5º");
        falso(cheio.entraNoTop(10), "empatar com o 5º não basta");
        falso(cheio.entraNoTop(5), "abaixo do 5º");
    }

    @Teste
    void incluirOrdenaELimitaACinco() {
        Ranking cheio = new Ranking(List.of(reg("a", 50), reg("b", 40), reg("c", 30), reg("d", 20), reg("e", 10)));
        Ranking novo = cheio.com(reg("novo", 35));
        igual(List.of("a", "b", "novo", "c", "d"), nomes(novo));
        igual(5, cheio.getRegistros().size(), "o original não muda");
    }

    @Teste
    void empateMantemQuemChegouPrimeiro() {
        Ranking r = new Ranking(List.of(reg("antigo", 30))).com(reg("novo", 30));
        igual(List.of("antigo", "novo"), nomes(r));
    }

    @Teste
    void liderEhOPrimeiro() {
        falso(Ranking.vazio().getLider().isPresent(), "vazio não tem líder");
        Ranking r = Ranking.vazio().com(reg("b", 10)).com(reg("a", 90));
        igual("a", r.getLider().orElseThrow().nome());
    }

    @Teste
    void ordenarColocaAMaiorPontuacaoPrimeiro() {
        List<RegistroRanking> ordenados = Ranking.ordenar(List.of(reg("x", 1), reg("y", 9), reg("z", 5)));
        igual(List.of(9, 5, 1), ordenados.stream().map(RegistroRanking::pontos).collect(Collectors.toList()));
    }
}
