package br.edu.unifor.missaomarte.grafico.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.historia.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class ServicoHistoriaTeste {
    @TempDir Path pasta;
    @Test void escritasEmSequenciaPreservamConclusaoAoReabrir() throws Exception {
        var arquivo=pasta.resolve("historia.properties");
        try(var s=new ServicoHistoria(new HistoriaArquivo(arquivo))) {
            var inicio=s.iniciarM01();
            var conclusao=s.concluirM01(72);
            inicio.get(3,TimeUnit.SECONDS);conclusao.get(3,TimeUnit.SECONDS);
            assertTrue(s.carregar().get(3,TimeUnit.SECONDS).progresso().m01Concluida());
        }
        try(var s=new ServicoHistoria(new HistoriaArquivo(arquivo))) {
            assertEquals(72,s.carregar().get(3,TimeUnit.SECONDS).progresso().melhorPontuacao());
        }
    }
    @Test void falhaAoSalvarNaoConfirmaProgressoEmMemoria() throws Exception {
        var r=new RepositorioHistoria() {
            public Leitura carregar() { return new Leitura(ProgressoDaHistoria.novo(),"",true); }
            public void salvar(ProgressoDaHistoria p) throws java.io.IOException { throw new java.io.IOException("disco indisponível"); }
        };
        try(var s=new ServicoHistoria(r)) {
            assertThrows(java.util.concurrent.ExecutionException.class,()->s.iniciarM01().get(3,TimeUnit.SECONDS));
            assertFalse(s.carregar().get(3,TimeUnit.SECONDS).progresso().m01Concluida());
        }
    }
    @Test void conclusaoSemMissaoIniciadaERecusada() {
        try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("historia.properties")))) {
            assertThrows(java.util.concurrent.ExecutionException.class,()->s.concluirM01(99).get(3,TimeUnit.SECONDS));
        }
    }
}
