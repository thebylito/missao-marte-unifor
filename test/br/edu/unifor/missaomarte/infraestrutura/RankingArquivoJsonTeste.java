package br.edu.unifor.missaomarte.infraestrutura;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.teste.Teste;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class RankingArquivoJsonTeste {

    private static final RegistroRanking ANA =
            new RegistroRanking("Ana", 42, Dificuldade.FACIL, 4, "2026-11-01 10:00:00", 35);
    private static final RegistroRanking BRUNO =
            new RegistroRanking("Bruno \"Foguete\"", 57, Dificuldade.DIFICIL, 5, "2026-11-02 11:30:00", 80);

    private final Path pasta;
    private final Path arquivo;

    RankingArquivoJsonTeste() throws IOException {
        pasta = Files.createTempDirectory("ranking-teste");
        arquivo = pasta.resolve("ranking.json");
    }

    @Teste
    void arquivoAusenteEhRankingVazio() {
        igual(List.of(), new RankingArquivoJson(arquivo).carregar());
    }

    @Teste
    void gravaNoMesmoFormatoDaVersaoOriginal() throws IOException {
        new RankingArquivoJson(arquivo).salvar(List.of(BRUNO, ANA));
        igual("[{\"name\":\"Bruno \\\"Foguete\\\"\",\"score\":57,\"dificuldade\":\"DIFICIL\",\"passageirosColetados\":5,"
                        + "\"dataHora\":\"2026-11-02 11:30:00\",\"tempoJogo\":80},"
                        + "{\"name\":\"Ana\",\"score\":42,\"dificuldade\":\"FACIL\",\"passageirosColetados\":4,"
                        + "\"dataHora\":\"2026-11-01 10:00:00\",\"tempoJogo\":35}]",
                Files.readString(arquivo, StandardCharsets.UTF_8));
    }

    @Teste
    void leDeVoltaOrdenadoPelaMaiorPontuacao() throws IOException {
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        repo.salvar(List.of(ANA, BRUNO));
        igual(List.of(BRUNO, ANA), repo.carregar());
    }

    @Teste
    void nomeComVirgulaEhDescartadoAoLer() throws IOException {
        // comportamento herdado da Fase 0 (problema 2 em docs/fase-00-comportamento-referencia.md)
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        repo.salvar(List.of(new RegistroRanking("Silva, Ana", 30, Dificuldade.FACIL, 4, "2026-11-01 10:00:00", 35), ANA));
        igual(List.of(ANA), repo.carregar());
    }

    @Teste
    void apagarRemoveOArquivo() throws IOException {
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        repo.salvar(List.of(ANA));
        verdadeiro(Files.exists(arquivo), "gravou");
        repo.apagar();
        falso(Files.exists(arquivo), "apagou");
        repo.apagar(); // apagar de novo não é erro
    }
}
