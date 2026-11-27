package br.edu.unifor.missaomarte.grafico.telas;

import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.aplicacao.*;
import br.edu.unifor.missaomarte.dominio.*;
import br.edu.unifor.missaomarte.grafico.*;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPilotagem;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import br.edu.unifor.missaomarte.pilotagem.*;
import com.badlogic.gdx.Input.Keys;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesembarqueNoPousoTeste {
    @TempDir Path pasta;

    @Test
    @SuppressWarnings("unchecked")
    void entregaEAberturaDaNaveNoMesmoQuadroUsamAPosicaoDoEvento() throws Exception {
        var passageiro = new Astronauta("Ensaio", Posicao.ORIGEM);
        var caverna = new Caverna(2000, 1200).base(100, 98)
                .passageiro(passageiro, 700, 86)
                .passageiro(new Professor("Outro", Posicao.ORIGEM), 1200, 86);
        var partida = new PartidaDePilotagem("Ensaio", Dificuldade.MEDIO, caverna, ConfigDeVoo.medio());
        var nave = partida.getNave();
        campo(nave, "estado", NaveEmVoo.Estado.VOANDO);
        campo(nave, "plataforma", null);
        campo(nave, "y", 138.01);
        campo(nave, "velocidadeY", -20.0);
        Field passageiros = NaveEmVoo.class.getDeclaredField("aBordo");
        passageiros.setAccessible(true);
        ((List<Passageiro>) passageiros.get(nave)).add(passageiro);
        var ranking = new ServicoRanking(new RankingArquivoJson(pasta.resolve("ranking.json")), Relogio.DO_SISTEMA);
        var controlador = new ControladorDaPilotagem(partida, ranking);
        var jogo = new MissaoMarteGame(new ServicosDoJogo(new GeradorDeMissao(new Random(1)),
                ranking, Relogio.DO_SISTEMA));
        Field recursos = MissaoMarteGame.class.getDeclaredField("naves");
        recursos.setAccessible(true);
        recursos.set(jogo, new br.edu.unifor.missaomarte.grafico.recursos.RecursosDasNaves(
                new com.badlogic.gdx.files.FileHandle(pasta.toFile())));
        var tela = new TelaPilotagem(jogo, controlador);
        controlador.teclaPressionada(Keys.UP);

        assertDoesNotThrow(() -> tela.atualizar(.05f));
        assertEquals(NaveEmVoo.Estado.VOANDO, nave.getEstado());
        assertNull(nave.getPlataforma());
        assertTrue(nave.getABordo().isEmpty());
        assertTrue(partida.getPontos() > 0);
        Field saidas = TelaPilotagem.class.getDeclaredField("saindo");
        saidas.setAccessible(true);
        var registros = (List<?>) saidas.get(tela);
        assertEquals(1, registros.size());
        Object saida = registros.get(0);
        var quem = saida.getClass().getDeclaredMethod("passageiro"); quem.setAccessible(true);
        var x = saida.getClass().getDeclaredMethod("x"); x.setAccessible(true);
        var y = saida.getClass().getDeclaredMethod("y"); y.setAccessible(true);
        assertSame(passageiro, quem.invoke(saida));
        assertEquals((float) caverna.getBase().getCentroX() + jogo.getNaves().deslocamentoDaPorta(), (float) x.invoke(saida), .001f);
        assertEquals((float) caverna.getBase().getSuperficie(), (float) y.invoke(saida), .001f);
    }

    private static void campo(NaveEmVoo nave, String nome, Object valor) throws Exception {
        Field campo = NaveEmVoo.class.getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(nave, valor);
    }
}
