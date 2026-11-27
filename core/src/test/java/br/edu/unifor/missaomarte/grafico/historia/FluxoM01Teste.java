package br.edu.unifor.missaomarte.grafico.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.historia.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class FluxoM01Teste {
    @TempDir Path pasta;
    @Test void recuperacaoDeCorrupcaoExigeConfirmacaoEArquivaOriginais() throws Exception {
        Path arquivo=pasta.resolve("p.properties");java.nio.file.Files.writeString(arquivo,"danificado");
        java.nio.file.Files.writeString(pasta.resolve("p.properties.bak"),"copia danificada");
        try(var s=new ServicoHistoria(new HistoriaArquivo(arquivo))) {
            var tela=new TelaCampanha(jogo(),s);s.carregar().get(3,TimeUnit.SECONDS);tela.atualizar(0);
            tela.entrada().keyDown(Keys.N);
            assertEquals("danificado",java.nio.file.Files.readString(arquivo));
            tela.entrada().keyDown(Keys.ESCAPE);
            assertEquals("danificado",java.nio.file.Files.readString(arquivo));
            tela.entrada().keyDown(Keys.N);tela.entrada().keyDown(Keys.ENTER);
            s.carregar().get(3,TimeUnit.SECONDS);tela.atualizar(0);
            assertTrue(new HistoriaArquivo(arquivo).carregar().gravavel());
            try(var itens=java.nio.file.Files.list(pasta)) {
                var arquivos=itens.filter(f->f.getFileName().toString().contains(".corrompido-")).toList();
                assertEquals(2,arquivos.size());
                var conteudos=new java.util.HashSet<String>();
                for(var f:arquivos)conteudos.add(java.nio.file.Files.readString(f));
                assertEquals(java.util.Set.of("danificado","copia danificada"),conteudos);
            }
        }
    }
    @Test void objetivoNaoPedeNovoResgateDepoisDaEntrega() throws Exception {
        try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
            var tela=new TelaMissaoHistoria(jogo(),s);
            var voo=tela.getEncontro().partida();
            var estado=voo.getClass().getDeclaredField("estado");estado.setAccessible(true);estado.set(voo,Partida.Estado.VITORIA);
            assertFalse(tela.objetivoPrincipal().contains("resgatar"));
        }
    }

    private MissaoMarteGame jogo() {
        return new MissaoMarteGame(null) {
            @Override public void trocarTela(Screen nova) { screen=nova; }
        };
    }
    @Test void briefingSalvaRetomadaAntesDeIniciarVoo() throws Exception {
        try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
            var jogo=jogo();var tela=new TelaBriefingHistoria(jogo,s);
            tela.entrada().keyDown(Keys.ENTER);
            s.carregar().get(3,TimeUnit.SECONDS);tela.atualizar(0);
            assertInstanceOf(TelaMissaoHistoria.class,jogo.getScreen());
            assertEquals("M01",s.carregar().get(3,TimeUnit.SECONDS).progresso().retomada());
        }
    }
    @Test void vitoriaSalvaConclusaoMasAbandonoNao() throws Exception {
        try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
            s.iniciarM01().get(3,TimeUnit.SECONDS);
            var tela=new TelaResultadoHistoria(jogo(),s,Partida.Estado.ABORTADA,0);
            tela.atualizar(0);
            assertFalse(s.carregar().get(3,TimeUnit.SECONDS).progresso().m01Concluida());
            var vitoria=new TelaResultadoHistoria(jogo(),s,Partida.Estado.VITORIA,81);
            s.carregar().get(3,TimeUnit.SECONDS);vitoria.atualizar(0);
            assertEquals("Progresso salvo",vitoria.situacao());
            assertTrue(s.carregar().get(3,TimeUnit.SECONDS).progresso().m01Concluida());
        }
    }
    @Test void falhaDeGravacaoFicaVisivelENaoIniciaVoo() throws Exception {
        var r=new RepositorioHistoria() {
            public Leitura carregar() { return new Leitura(ProgressoDaHistoria.novo(),"",true); }
            public void salvar(ProgressoDaHistoria p) throws java.io.IOException {throw new java.io.IOException("sem espaço");}
        };
        try(var s=new ServicoHistoria(r)) {
            var jogo=jogo();var tela=new TelaBriefingHistoria(jogo,s);
            tela.entrada().keyDown(Keys.ENTER);
            s.carregar().get(3,TimeUnit.SECONDS);tela.atualizar(0);
            assertNull(jogo.getScreen());
            assertTrue(tela.situacao().contains("Não foi possível"));
        }
    }
}
