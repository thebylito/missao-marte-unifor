package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.pilotagem.*;
import ensaio.PilotoCapituloUm;
class CapituloUmVooTeste {
 private void completar(String id,boolean inverso,int passageiros) {completar(id,inverso,passageiros,PartidaDePilotagem.PASSO);}
 private void completar(String id,boolean inverso,int passageiros,double quadro) {
  var e=MissaoHistoria.porId(id).criarEncontro();var p=e.partida();var piloto=new PilotoCapituloUm(inverso);
  while(p.getEstado()==Partida.Estado.EM_ANDAMENTO && p.getTempo()<360) {
   e.atualizar(quadro,piloto.decidir(e),false,false);
   assertNotEquals(NaveEmVoo.Estado.DESTRUIDA,p.getNave().getEstado(),()->id+" colisão t="+p.getTempo()+" x="+p.getNave().getX()+" y="+p.getNave().getY());
  }
  assertEquals(Partida.Estado.VITORIA,p.getEstado(),()->id+" t="+p.getTempo()+" x="+p.getNave().getX()+" y="+p.getNave().getY()+" fuel="+p.getNave().getCombustivel());
  assertEquals(passageiros,p.getEntregues());assertEquals(3,p.getVidas());
  if(id.equals("M02"))assertTrue(e.antena().ativa());
 }
 @Test void pilotoDoCapituloMantemFolgaDoTetoDaM01A30Fps(){completar("M01",false,1,1./30);}
 @Test void capituloTemRotasViaveisComEntradaA30Fps(){completar("M02",false,1,1./30);completar("M03",true,2,1./30);}
 @Test void m02ResgataRosaAtivaAntenaERetorna(){completar("M02",false,1);}
 @Test void m03PermiteResgatarPrimeiroMaria(){completar("M03",false,2);}
 @Test void m03PermiteResgatarPrimeiroSilva(){completar("M03",true,2);}
}
