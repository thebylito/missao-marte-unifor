package br.edu.unifor.missaomarte.grafico.historia;

import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.pilotagem.*;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class EncontroEntradaTeste {
    @Test void toqueBreveEntreQuadrosAindaDisparaUmPulso() throws Exception {
        var encontro=EncontroDeResgate.novo();
        for(Object[] item:new Object[][]{{"x",650.0},{"y",800.0},{"estado",NaveEmVoo.Estado.VOANDO},{"plataforma",null}}) {
            Field f=NaveEmVoo.class.getDeclaredField((String)item[0]); f.setAccessible(true); f.set(encontro.partida().getNave(),item[1]);
        }
        var tela=new TelaEncontro(null,encontro);
        var entrada=tela.entrada();
        entrada.keyDown(Keys.F);
        tela.getControlador().atualizar(.001f); // ainda não houve passo físico
        entrada.keyUp(Keys.F);
        tela.getControlador().atualizar(.01f);
        assertEquals(3,encontro.bateria());
    }

    @Test void pausaLimpaMotorEGiroDeTecladoEToque() {
        var tela=new TelaEncontro(null,EncontroDeResgate.novo());
        var c=tela.getControlador();
        var v=new br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo(0,0,800,600);
        var layout=br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.calcular(v,true,.5);
        c.usarLayout(layout);
        var motor=layout.botoes().get(br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao.MOTOR);
        c.tocou(2,motor.centroX(),motor.centroY());
        c.teclaPressionada(Keys.LEFT);
        assertNotEquals(Comandos.NENHUM,c.comandos());
        tela.pause();
        assertEquals(Comandos.NENHUM,c.comandos());
    }

    @Test void tecladoMantemPulsoEAssistenciaEPausaSoltaODisparo() throws Exception {
        var encontro=EncontroDeResgate.novo();
        for(Object[] item:new Object[][]{{"x",650.0},{"y",800.0},{"estado",NaveEmVoo.Estado.VOANDO},{"plataforma",null}}) {
            Field f=NaveEmVoo.class.getDeclaredField((String)item[0]); f.setAccessible(true); f.set(encontro.partida().getNave(),item[1]);
        }
        var tela=new TelaEncontro(null,encontro);
        var controle=(ControladorDoEncontro)tela.getControlador();
        var entrada=tela.entrada();
        entrada.keyDown(Keys.G);
        assertTrue(controle.assistido());
        entrada.keyDown(Keys.F);
        controle.atualizar(.1f);
        assertEquals(3,encontro.bateria());
        tela.pause();
        // Sem alvo no cone, nem a assistência dispara depois da pausa.
        for(int i=0;i<4;i++) controle.atualizar(.25f);
        assertEquals(3,encontro.bateria());
    }

    @Test void conclusaoUsaResultadoProprioEPermiteRepetirSemServidor() {
        var jogo=new MissaoMarteGame(null) {
            @Override public void trocarTela(Screen nova) { screen=nova; }
        };
        var tela=new TelaEncontro(jogo,EncontroDeResgate.novo());
        tela.concluirPartida(null);
        var resultado=assertInstanceOf(TelaResultadoDoEncontro.class,jogo.getScreen());
        resultado.entrada().keyDown(Keys.ENTER);
        assertInstanceOf(TelaEncontro.class,jogo.getScreen());
        assertNotSame(tela,jogo.getScreen());
    }
}
