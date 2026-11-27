package br.edu.unifor.missaomarte.pilotagem;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import org.junit.jupiter.api.Test;
class AlertaDaSentinelaTeste {
 @Test void miraDaSentinelaNaoMarcaPatrulhaForaDoCone() {
  var c=new Caverna(2000,1000).base(100,128);
  var p=new PartidaDePilotagem("Teste",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL));
  var n=p.getNave();n.x=600;n.y=400;n.angulo=-90;
  var e=new EncontroDeResgate(p).comSentinela(800,400);
  assertTrue(e.sentinelaNoCone());assertFalse(e.patrulheiroNoCone());assertTrue(e.alvoNoCone());
 }
 @Test void bateriaNaoRegeneraEnquantoSentinelaMantemCombate() {
  var c=new Caverna(2000,1000).base(100,128).instalacao(500,128);
  var p=new PartidaDePilotagem("Teste",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL));
  var n=p.getNave();n.x=600;n.y=168;n.angulo=90;n.estado=NaveEmVoo.Estado.POUSADA;n.plataforma=c.getPlataformas().get(1);
  var e=new EncontroDeResgate(p).semPatrulha().comSentinela(800,168);
  e.atualizar(PartidaDePilotagem.PASSO,Comandos.NENHUM,true,false);
  assertTrue(e.emAlerta());assertEquals(3,e.bateria());
  for(int i=0;i<360;i++)e.atualizar(PartidaDePilotagem.PASSO,Comandos.NENHUM,false,false);
  assertEquals(3,e.bateria());
 }
}
