package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.pilotagem.historia.SentinelaDeVarredura;
import org.junit.jupiter.api.Test;
class SentinelaEAntenaTeste {
 @Test void anunciaAntesDeAtirarERespeitaCobertura() {
  var s=new SentinelaDeVarredura(1200,450);
  assertFalse(s.atualizar(.01,1000,450,true));
  assertEquals(SentinelaDeVarredura.Estado.AVISO,s.estado());
  assertFalse(s.atualizar(.99,1000,450,true));
  assertTrue(s.atualizar(.02,1000,450,true));
  var oculta=new SentinelaDeVarredura(1200,450);
  for(int i=0;i<500;i++) assertFalse(oculta.atualizar(.01,1000,450,false));
 }
 @Test void feixeDeAvisoApontaNaDirecaoDoDisparo() {
  var s=new SentinelaDeVarredura(1200,450);s.atualizar(.01,1000,450,true);
  s.atualizar(.8,1000,450,true);assertEquals(180,s.angulo(),.01);
  s.atualizar(.1,1000,550,true);assertEquals(Math.toDegrees(Math.atan2(100,-200)),s.angulo(),.01);
 }
 @Test void pulsoDesativaComRecuperacaoAntesDeNovoAtaque() {
  var s=new SentinelaDeVarredura(1200,450);s.desativar();
  for(int i=0;i<399;i++)assertFalse(s.atualizar(.01,1000,450,true));
  assertEquals(SentinelaDeVarredura.Estado.DESATIVADO,s.estado());
  s.atualizar(.02,1000,450,true);assertEquals(SentinelaDeVarredura.Estado.RECUPERANDO,s.estado());
  for(int i=0;i<99;i++)assertFalse(s.atualizar(.01,1000,450,true));
 }
}
