package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class HistoriaArquivoTeste {
    @TempDir Path pasta;
    @Test void erroDeLeituraNaoOfereceReinicioDestrutivo() throws Exception {
        Path arquivo=java.nio.file.Files.createDirectory(pasta.resolve("progresso.properties"));
        var r=new HistoriaArquivo(arquivo);
        assertFalse(r.carregar().permiteReiniciar());
        assertThrows(IOException.class,r::reiniciarAposCorrupcao);
        assertTrue(java.nio.file.Files.isDirectory(arquivo));
    }

    @Test void copiaDeVersaoFuturaTambemEProtegida() throws Exception {
        Path f=pasta.resolve("p.properties");var r=new HistoriaArquivo(f);
        r.salvar(ProgressoDaHistoria.novo().concluirM01(32));
        String principal=Files.readString(f);
        Path copia=pasta.resolve("p.properties.bak");String futura="versao=99\ncampanha=missao-marte\nperfil=expedicao\n";
        Files.writeString(copia,futura);
        assertFalse(r.carregar().gravavel());
        assertThrows(IOException.class,()->r.salvar(ProgressoDaHistoria.novo()));
        assertEquals(principal,Files.readString(f));assertEquals(futura,Files.readString(copia));
    }

    @Test void salvaCarregaEReabreSemTocarNoRanking() throws Exception {
        Path f=pasta.resolve("historia/expedicao.properties");
        Path ranking=pasta.resolve("ranking.json");Files.writeString(ranking,"ranking intocado");
        var repositorio=new HistoriaArquivo(f);
        assertEquals(ProgressoDaHistoria.novo(),repositorio.carregar().progresso());
        var esperado=ProgressoDaHistoria.novo().iniciarM01().concluirM01(73);
        repositorio.salvar(esperado);
        assertEquals(esperado,new HistoriaArquivo(f).carregar().progresso());
        assertEquals("ranking intocado",Files.readString(ranking));
    }
    @Test void arquivoCorrompidoRecuperaCopiaValidaSemSilenciarAviso() throws Exception {
        Path f=pasta.resolve("expedicao.properties");var r=new HistoriaArquivo(f);
        var salvo=ProgressoDaHistoria.novo().concluirM01(60);
        r.salvar(salvo);Files.writeString(f,"invalido");
        var leitura=new HistoriaArquivo(f).carregar();
        assertEquals(salvo,leitura.progresso());assertFalse(leitura.aviso().isEmpty());assertTrue(leitura.gravavel());
        r.salvar(salvo.iniciarM01());Files.writeString(f,"outra falha");
        assertEquals(salvo,new HistoriaArquivo(f).carregar().progresso());
    }
    @Test void versaoFuturaNaoERebaixadaNemSobrescrita() throws Exception {
        Path f=pasta.resolve("expedicao.properties");
        String futuro="versao=99\ncampanha=missao-marte\nperfil=expedicao\n";
        Files.writeString(f,futuro);
        var r=new HistoriaArquivo(f);
        assertFalse(r.carregar().gravavel());
        assertThrows(IOException.class,()->r.salvar(ProgressoDaHistoria.novo()));
        assertEquals(futuro,Files.readString(f));
    }
    @Test void corrupcaoSemCopiaNaoApagaDados() throws Exception {
        Path f=pasta.resolve("expedicao.properties");Files.writeString(f,"dados danificados");
        var r=new HistoriaArquivo(f);
        assertFalse(r.carregar().gravavel());
        assertThrows(IOException.class,()->r.salvar(ProgressoDaHistoria.novo()));
        assertEquals("dados danificados",Files.readString(f));
    }
}
