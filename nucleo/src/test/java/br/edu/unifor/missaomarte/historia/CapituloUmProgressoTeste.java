package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class CapituloUmProgressoTeste {
 @TempDir Path pasta;
 @Test void desbloqueiaEmOrdemESemApagarResultadosNoReplay() {
  var p=ProgressoDaHistoria.novo();
  assertThrows(IllegalStateException.class,()->p.iniciar("M02"));
  var m1=p.iniciar("M01").concluir("M01",42);
  var m2=m1.iniciar("M02").concluir("M02",51);
  var replay=m2.iniciar("M01").concluir("M01",10);
  assertEquals(42,replay.melhorPontuacao("M01"));
  assertEquals(51,replay.melhorPontuacao("M02"));
  assertTrue(replay.disponivel("M03"));
  assertFalse(replay.capituloConcluido());
  assertTrue(replay.iniciar("M03").concluir("M03",60).capituloConcluido());
 }
 @Test void reabreTresMissoesComRetomadaEResultadosIndependentes() throws Exception {
  var p=ProgressoDaHistoria.novo().concluirM01(40).iniciar("M02").concluir("M02",50).iniciar("M03");
  var f=pasta.resolve("p.properties");new HistoriaArquivo(f).salvar(p);
  var l=new HistoriaArquivo(f).carregar();
  assertTrue(l.gravavel());assertEquals(p,l.progresso());
  assertEquals("M03",l.progresso().retomada());
  assertEquals(40,l.progresso().melhorPontuacao());
 }
 @Test void migraSaveV1SemPerderMelhorResultado() throws Exception {
  String conteudo="1|missao-marte|expedicao|true|M01|73";
  var crc=new java.util.zip.CRC32();crc.update(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var f=pasta.resolve("antigo.properties");
  Files.writeString(f,"versao=1\ncampanha=missao-marte\nperfil=expedicao\nm01Concluida=true\nretomada=M01\nmelhorPontuacao=73\nintegridade="+Long.toHexString(crc.getValue())+"\n");
  var r=new HistoriaArquivo(f);var p=r.carregar().progresso();
  assertTrue(p.disponivel("M02"));assertEquals(73,p.melhorPontuacao("M01"));
  r.salvar(p.iniciar("M02"));assertEquals("M02",r.carregar().progresso().retomada());
 }
}
