package br.edu.unifor.missaomarte.grafico.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.historia.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class FluxoCapituloUmTeste {
 @TempDir Path pasta;
 private MissaoMarteGame jogo() {return new MissaoMarteGame(null){@Override public void trocarTela(Screen s){screen=s;}};}
 @Test void mapaImpedePularM01ELiberaM02DepoisDeSalvar() throws Exception {
  try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
   var j=jogo();var t=new TelaCampanha(j,s);s.carregar().get(3,TimeUnit.SECONDS);t.atualizar(0);
   t.entrada().keyDown(Keys.NUM_2);assertNull(j.getScreen());
   s.iniciarM01().get(3,TimeUnit.SECONDS);s.concluirM01(10).get(3,TimeUnit.SECONDS);
   t=new TelaCampanha(j,s);s.carregar().get(3,TimeUnit.SECONDS);t.atualizar(0);
   t.entrada().keyDown(Keys.NUM_2);
   assertInstanceOf(TelaBriefingHistoria.class,j.getScreen());
   assertEquals("M02",((TelaBriefingHistoria)j.getScreen()).missao().id());
  }
 }
 @Test void resultadoSoAbreProximaMissaoDepoisDoSave() throws Exception {
  try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
   s.iniciarM01().get(3,TimeUnit.SECONDS);var j=jogo();
   var r=new TelaResultadoHistoria(j,s,Partida.Estado.VITORIA,40,MissaoHistoria.porId("M01"));
   r.entrada().keyDown(Keys.N);assertNull(j.getScreen());
   s.carregar().get(3,TimeUnit.SECONDS);r.atualizar(0);r.entrada().keyDown(Keys.N);
   assertEquals("M02",((TelaBriefingHistoria)j.getScreen()).missao().id());
  }
 }
 @Test void retomarResgateAposAtivarAntenaAindaPedeRosa() throws Exception {
  try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
   var t=new TelaMissaoHistoria(jogo(),s,MissaoHistoria.porId("M02"));
   var a=t.getEncontro().antena();var campo=a.getClass().getDeclaredField("ativa");campo.setAccessible(true);campo.setBoolean(a,true);
   assertTrue(t.objetivoPrincipal().contains("Rosa"));
   assertTrue(t.mensagemDoRadio().contains("resgatar"));
  }
 }
 @Test void briefingM02IniciaM02ESalvaIdentificadorCorreto() throws Exception {
  try(var s=new ServicoHistoria(new HistoriaArquivo(pasta.resolve("p.properties")))) {
   s.iniciarM01().get(3,TimeUnit.SECONDS);s.concluirM01(10).get(3,TimeUnit.SECONDS);var j=jogo();
   var t=new TelaBriefingHistoria(j,s,MissaoHistoria.porId("M02"));t.entrada().keyDown(Keys.ENTER);
   s.carregar().get(3,TimeUnit.SECONDS);t.atualizar(0);
   assertEquals("M02",((TelaMissaoHistoria)j.getScreen()).missao().id());
   assertEquals("M02",s.carregar().get(3,TimeUnit.SECONDS).progresso().retomada());
  }
 }
}
