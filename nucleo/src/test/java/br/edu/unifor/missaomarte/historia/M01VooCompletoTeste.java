package br.edu.unifor.missaomarte.historia;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import ensaio.PilotoM01;
import org.junit.jupiter.api.Test;
class M01VooCompletoTeste {
    @Test void resgataEVoltaSemReposicionamentoOuPerdaDeVida() {
        var encontro=MissaoHistoria.primeiroSinal();var voo=encontro.partida();var piloto=new PilotoM01();
        while(voo.getEstado()==Partida.Estado.EM_ANDAMENTO && voo.getTempo()<180) {
            encontro.atualizar(PartidaDePilotagem.PASSO,piloto.decidir(voo),false,false);
            assertNotEquals(NaveEmVoo.Estado.DESTRUIDA,voo.getNave().getEstado(),
                ()->"Falha em "+voo.getTempo()+" x="+voo.getNave().getX()+" y="+voo.getNave().getY());
        }
        assertEquals(Partida.Estado.VITORIA,voo.getEstado(),()->"Tempo="+voo.getTempo()+" x="+voo.getNave().getX()+" y="+voo.getNave().getY());
        assertEquals(1,voo.getEntregues());assertEquals(3,voo.getVidas());
    }
}
