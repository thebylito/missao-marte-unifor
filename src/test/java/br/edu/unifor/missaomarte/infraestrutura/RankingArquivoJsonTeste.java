package br.edu.unifor.missaomarte.infraestrutura;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    @TempDir
    Path pasta; // o JUnit cria uma pasta temporária por teste e apaga depois

    private Path arquivo;

    @BeforeEach
    void prepararArquivo() {
        arquivo = pasta.resolve("ranking.json");
    }

    @Test
    void arquivoAusenteEhRankingVazio() {
        assertEquals(List.of(), new RankingArquivoJson(arquivo).carregar());
    }

    @Test
    void gravaNoMesmoFormatoDaVersaoOriginal() throws IOException {
        new RankingArquivoJson(arquivo).salvar(List.of(BRUNO, ANA));
        assertEquals("[{\"name\":\"Bruno \\\"Foguete\\\"\",\"score\":57,\"dificuldade\":\"DIFICIL\",\"passageirosColetados\":5,"
                        + "\"dataHora\":\"2026-11-02 11:30:00\",\"tempoJogo\":80},"
                        + "{\"name\":\"Ana\",\"score\":42,\"dificuldade\":\"FACIL\",\"passageirosColetados\":4,"
                        + "\"dataHora\":\"2026-11-01 10:00:00\",\"tempoJogo\":35}]",
                Files.readString(arquivo, StandardCharsets.UTF_8));
    }

    @Test
    void leDeVoltaOrdenadoPelaMaiorPontuacao() throws IOException {
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        repo.salvar(List.of(ANA, BRUNO));
        assertEquals(List.of(BRUNO, ANA), repo.carregar());
    }

    @Test
    void nomesComCaracteresEspeciaisSobrevivemAIdaEVolta() throws IOException {
        // corrigido na Fase 1: o leitor original descartava nomes com vírgula
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        List<RegistroRanking> especiais = List.of(
                new RegistroRanking("Silva, Ana", 90, Dificuldade.FACIL, 4, "2026-11-01 10:00:00", 35),
                new RegistroRanking("chave } e colchete ]", 80, Dificuldade.MEDIO, 5, "2026-11-01 10:00:00", 35),
                new RegistroRanking("barra \\ e \"aspas\"", 70, Dificuldade.DIFICIL, 5, "2026-11-01 10:00:00", 35),
                new RegistroRanking("dois:pontos {x}", 60, Dificuldade.MEDIO, 5, "2026-11-01 10:00:00", 35),
                new RegistroRanking("quebra\nde linha\te tab", 50, Dificuldade.MEDIO, 5, "2026-11-01 10:00:00", 35));
        repo.salvar(especiais);
        assertEquals(especiais, repo.carregar());
    }

    @Test
    void leArquivosGravadosPelaVersaoOriginal() {
        String original = "[{\"name\":\"Ana\",\"score\":42,\"dificuldade\":\"FACIL\",\"passageirosColetados\":4,"
                + "\"dataHora\":\"2026-11-01 10:00:00\",\"tempoJogo\":35},"
                + "{\"name\":\"Bruno \\\"Foguete\\\"\",\"score\":57,\"dificuldade\":\"DIFICIL\",\"passageirosColetados\":5,"
                + "\"dataHora\":\"2026-11-02 11:30:00\",\"tempoJogo\":80}]";
        assertEquals(List.of(ANA, BRUNO), RankingArquivoJson.ler(original));
    }

    @Test
    void toleraEspacosCamposExtrasEFaltantes() {
        String json = " [ { \"score\" : 10 , \"name\" : \"Só nome e pontos\" , \"extra\" : true } ,\n"
                + "  { \"name\": \"sem pontos\" } ] ";
        assertEquals(List.of(new RegistroRanking("Só nome e pontos", 10, Dificuldade.MEDIO, 0, "", 0)), RankingArquivoJson.ler(json));
    }

    @Test
    void arquivoMalformadoMantemOQueFoiLido() {
        String json = "[{\"name\":\"Ana\",\"score\":42},{\"name\":\"corta";
        assertEquals(List.of(new RegistroRanking("Ana", 42, Dificuldade.MEDIO, 0, "", 0)), RankingArquivoJson.ler(json));
        assertEquals(List.of(), RankingArquivoJson.ler("isto não é json"));
        assertEquals(List.of(), RankingArquivoJson.ler("[]"));
        assertEquals(List.of(), RankingArquivoJson.ler(""));
    }

    @Test
    void apagarRemoveOArquivo() throws IOException {
        RankingArquivoJson repo = new RankingArquivoJson(arquivo);
        repo.salvar(List.of(ANA));
        assertTrue(Files.exists(arquivo), "gravou");
        repo.apagar();
        assertFalse(Files.exists(arquivo), "apagou");
        repo.apagar(); // apagar de novo não é erro
    }
}
