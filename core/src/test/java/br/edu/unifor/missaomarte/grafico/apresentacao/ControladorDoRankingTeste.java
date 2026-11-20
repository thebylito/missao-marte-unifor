package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking.Aba;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoRanking.Acao;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import com.badlogic.gdx.Input.Keys;
import java.util.List;
import org.junit.jupiter.api.Test;

/** As abas local e global do ranking; o servidor só é consultado quando a aba global abre. */
class ControladorDoRankingTeste {

    private final Apoio.ServidorFalso servidor = new Apoio.ServidorFalso();
    private final Ranking local = new Ranking(List.of(
            new RegistroRanking("Ana", 60, Dificuldade.FACIL, 4, "2026-11-18 01:00:00", 5)));
    private final ControladorDoRanking ranking = new ControladorDoRanking(local, servidor);

    @Test
    void comecaNoLocalSemConsultarOServidor() {
        assertEquals(Aba.LOCAL, ranking.getAba());
        assertEquals(List.of("1. Ana - 60 pts | Fácil | Coletados: 4 | Tempo: 5s | 2026-11-18 01:00:00"), ranking.linhas());
        assertTrue(servidor.pedidos.isEmpty());
    }

    @Test
    void abaGlobalConsultaUmaVezEMostraOResultado() {
        ranking.teclaPressionada(Keys.RIGHT);
        assertEquals(Aba.GLOBAL, ranking.getAba());
        assertEquals(List.of("Consultando o servidor..."), ranking.linhas());
        servidor.ranking.sucesso(List.of(new PosicaoGlobal(1, "Bia", 80, "DIFICIL", 5, 90, "nao-eh-data")));
        assertEquals(List.of("1. Bia - 80 pts | Difícil | Coletados: 5 | Tempo: 90s | nao-eh-data"), ranking.linhas());

        ranking.teclaPressionada(Keys.LEFT);
        ranking.teclaPressionada(Keys.G);
        assertEquals(List.of("ranking"), servidor.pedidos, "volta à aba global sem nova consulta");
    }

    @Test
    void servidorForaDoArMostraOMotivo() {
        ranking.teclaPressionada(Keys.TAB);
        servidor.ranking.falha(Falha.semConexao());
        assertEquals(List.of("Servidor indisponível. Você pode jogar como convidado."), ranking.linhas());
        ranking.teclaPressionada(Keys.L);
        assertEquals(Aba.LOCAL, ranking.getAba());
    }

    @Test
    void rankingGlobalVazio() {
        ranking.teclaPressionada(Keys.G);
        servidor.ranking.sucesso(List.of());
        assertEquals(List.of("Nenhuma vitória registrada no servidor ainda."), ranking.linhas());
    }

    @Test
    void enterOuEscVoltam() {
        assertEquals(Acao.VOLTAR, ranking.teclaPressionada(Keys.ENTER));
        assertEquals(Acao.VOLTAR, ranking.teclaPressionada(Keys.ESCAPE));
        assertEquals(Acao.NENHUMA, ranking.teclaPressionada(Keys.X));
    }

    // ------------------------------------------------------------------ CT-25 (Fase 7): um ranking por modo

    private final Ranking daPilotagem = new Ranking(List.of(
            new RegistroRanking("Bia", 61, Dificuldade.MEDIO, 3, "2026-11-18 02:00:00", 140)));
    private final ControladorDoRanking porModo = new ControladorDoRanking(local, daPilotagem, servidor);

    @Test
    void cimaEBaixoTrocamOModoEOLocalMostraOTop5DeCadaUm() {
        assertEquals(Modo.CLASSICO, porModo.getModo());
        porModo.teclaPressionada(Keys.DOWN);
        assertEquals(Modo.PILOTAGEM, porModo.getModo());
        assertEquals(List.of("1. Bia - 61 pts | Médio | Coletados: 3 | Tempo: 140s | 2026-11-18 02:00:00"),
                porModo.linhas());
        porModo.teclaPressionada(Keys.UP);
        assertEquals(Modo.CLASSICO, porModo.getModo());
        porModo.teclaPressionada(Keys.P);
        assertEquals(Modo.PILOTAGEM, porModo.getModo());
        porModo.teclaPressionada(Keys.C);
        assertEquals(Modo.CLASSICO, porModo.getModo());
        assertTrue(servidor.pedidos.isEmpty(), "o local não consulta o servidor");
    }

    @Test
    void naAbaGlobalCadaModoConsultaOSeuRankingUmaVez() {
        porModo.teclaPressionada(Keys.G);
        porModo.teclaPressionada(Keys.M);
        porModo.teclaPressionada(Keys.M);
        porModo.teclaPressionada(Keys.M);
        assertEquals(List.of("ranking", "ranking PILOTAGEM"), servidor.pedidos);
        assertEquals(Modo.PILOTAGEM, porModo.getModo());
        assertEquals(List.of("Consultando o servidor..."), porModo.linhas());
    }
}
