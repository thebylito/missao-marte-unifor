package br.edu.unifor.missaomarte.grafico.historia;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.Input.Keys;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
class EncontroResizeTeste {
    @Test void redimensionarLimpaEntradasEMantemMensagemAcimaDosBotoes() {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        var glAnterior=com.badlogic.gdx.Gdx.gl;
        var graphicsAnterior=com.badlogic.gdx.Gdx.graphics;
        try {
            com.badlogic.gdx.Gdx.gl=(com.badlogic.gdx.graphics.GL20)java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),new Class<?>[]{com.badlogic.gdx.graphics.GL20.class},
                (proxy,method,args)->null);
            com.badlogic.gdx.Gdx.graphics=(com.badlogic.gdx.Graphics)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.Graphics.class}, (proxy,method,args)->method.getReturnType()==int.class
                    ? (method.getName().contains("Height") ? 600 : 800) : method.getReturnType()==boolean.class ? false : null);
            var jogo=new MissaoMarteGame(null,new br.edu.unifor.missaomarte.grafico.Plataforma(true,
                br.edu.unifor.missaomarte.grafico.Plataforma.padrao().servidor()));
            var tela=new TelaEncontro(jogo,EncontroDeResgate.novo());
            for(int[] tamanho:new int[][]{{800,600},{2340,1080},{1080,2340}}) {
                tela.getControlador().teclaPressionada(Keys.UP);
                ((ControladorDoEncontro)tela.getControlador()).pulso(true);
                tela.resize(tamanho[0],tamanho[1]);
                assertEquals(Comandos.NENHUM,tela.getControlador().comandos());
                tela.getControlador().atualizar(.1f);
                assertEquals(4,tela.getEncontro().bateria());
                assertTrue(tela.yDaMensagem()-24>tela.botaoAssistencia().topo());
            }
        } finally {
            com.badlogic.gdx.Gdx.gl=glAnterior;
            com.badlogic.gdx.Gdx.graphics=graphicsAnterior;
        }
    }

}
