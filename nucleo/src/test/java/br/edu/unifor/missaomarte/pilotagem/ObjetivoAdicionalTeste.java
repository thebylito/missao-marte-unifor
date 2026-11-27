package br.edu.unifor.missaomarte.pilotagem;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.*;
import br.edu.unifor.missaomarte.pilotagem.historia.ObjetivoDaAntena;
import org.junit.jupiter.api.Test;
class ObjetivoAdicionalTeste {
 @Test void retornoAntecipadoPreservaPassageiraENaoConclui() {
  var c=new Caverna(2400,1200).base(100,128).passageiro(new Engenheiro("Rosa",Posicao.ORIGEM),1200,128);
  var p=new PartidaDePilotagem("Teste",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL));
  p.exigirObjetivoAdicional();var n=p.getNave();
  n.embarcar(c.getPlataformas().get(1).retirar());
  n.estado=NaveEmVoo.Estado.VOANDO;n.x=c.getBase().getCentroX();n.y=128-p.getConfig().peY()+1;
  n.velocidadeY=-10;p.atualizar(.2,Comandos.NENHUM);
  assertEquals(Partida.Estado.EM_ANDAMENTO,p.getEstado());assertEquals(1,n.getABordo().size());
  p.cumprirObjetivoAdicional();p.atualizar(.1,Comandos.NENHUM);
  assertEquals(Partida.Estado.VITORIA,p.getEstado());assertEquals(1,p.getEntregues());
 }
 @Test void antenaExigeRosaEContinuidadeNoPouso() {
  var c=new Caverna(2400,1200).base(100,128).passageiro(new Engenheiro("Rosa",Posicao.ORIGEM),1200,128).instalacao(2000,328);
  var p=new PartidaDePilotagem("Teste",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL));
  var a=new ObjetivoDaAntena(c.getPlataformas().get(2));var n=p.getNave();
  n.estado=NaveEmVoo.Estado.POUSADA;n.plataforma=c.getPlataformas().get(2);
  a.atualizar(4,p);assertFalse(a.ativa());
  n.embarcar(c.getPlataformas().get(1).retirar());a.atualizar(2,p);assertFalse(a.ativa());
  n.estado=NaveEmVoo.Estado.VOANDO;a.atualizar(.1,p);assertEquals(0,a.progresso());
  n.estado=NaveEmVoo.Estado.POUSADA;a.atualizar(2.99,p);assertFalse(a.ativa());
  a.atualizar(.01,p);assertTrue(a.ativa());assertEquals(1,c.totalDePassageiros());
 }
}
