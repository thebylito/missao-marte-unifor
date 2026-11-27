package ensaio;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import br.edu.unifor.missaomarte.grafico.*;
import br.edu.unifor.missaomarte.grafico.historia.*;
import br.edu.unifor.missaomarte.grafico.telas.*;
import br.edu.unifor.missaomarte.pilotagem.*;
import br.edu.unifor.missaomarte.pilotagem.historia.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.android.AndroidApplication;
import java.util.concurrent.*;
import java.lang.reflect.*;

/** Ensaio instrumentado: reposiciona a nave entre cenários, mas avança voo e combate reais. */
public class Encontro extends Instrumentation {
    MissaoMarteGame jogo;
    AndroidApplication app;
    TelaEncontro tela;
    EncontroDeResgate encontro;
    ControladorDoEncontro controle;
    final CountDownLatch fim = new CountDownLatch(1);
    volatile String resultado;
    int etapa, quadros;
    int[] toquePulso, toqueMotor;
    long resultadoDesde;
    double inicio;
    long ultimo, maiorIntervalo;
    int lentos, medidos;
    void marco(String texto) { Bundle b=new Bundle(); b.putString("etapa",texto); sendStatus(1,b); }
    @Override public void onCreate(Bundle b) { super.onCreate(b); start(); }
    @Override public void onStart() {
        try {
            Intent i=getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app=(AndroidApplication)startActivitySync(i);
            runOnMainSync(()->{app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);app.getWindow().addFlags(128);});
            jogo=(MissaoMarteGame)app.getApplicationListener();
            Gdx.app.postRunnable(this::quadro);
            if(!fim.await(65,TimeUnit.SECONDS)) throw new AssertionError("tempo esgotado");
        } catch(Throwable e) { resultado=e.toString(); }
        Bundle b=new Bundle(); b.putString("resultado",resultado==null?"PASS":resultado);
        finish(resultado==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,b);
    }
    void posicionar(double x,double y,double angulo) throws Exception {
        var n=encontro.partida().getNave();
        for(Object[] c:new Object[][]{{"x",x},{"y",y},{"angulo",angulo},{"velocidadeX",0.0},{"velocidadeY",0.0},
                {"estado",NaveEmVoo.Estado.VOANDO},{"plataforma",null}}) {
            Field f=NaveEmVoo.class.getDeclaredField((String)c[0]);f.setAccessible(true);f.set(n,c[1]);
        }
    }
    void mudar(int nova,String texto) { etapa=nova;inicio=encontro.partida().getTempo();marco(texto); }
    void exigir(boolean ok,String mensagem) { if(!ok) throw new AssertionError(mensagem); }
    void quadro() {
        try {
            long agora=System.nanoTime();
            if(tela!=null && ultimo!=0) {
                long intervalo=agora-ultimo;maiorIntervalo=Math.max(maiorIntervalo,intervalo);
                medidos++;if(intervalo>50_000_000)lentos++;
            }
            ultimo=agora;
            if(etapa==0) {
                if(jogo.getScreen() instanceof TelaEntrada) {
                    Gdx.input.getInputProcessor().keyDown(Input.Keys.NUM_3); etapa=1;
                }
            } else if(etapa==1) {
                exigir(jogo.getScreen() instanceof TelaMenu,"menu convidado");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.NUM_8);
                exigir(jogo.getScreen() instanceof TelaEncontro,"entrada do protótipo");
                tela=(TelaEncontro)jogo.getScreen();encontro=tela.getEncontro();
                controle=(ControladorDoEncontro)tela.getControlador();
                mudar(2,"Encontro carregado pela entrada normal do menu.");
            } else {
                var voo=encontro.partida();var n=voo.getNave();double t=voo.getTempo()-inicio;
                switch(etapa) {
                    case 2:
                        if(t>2) {
                            posicionar(650,800,0);
                            toquePulso=toque(tela.botaoPulso());
                            var v=new br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo(400-Gdx.graphics.getWidth()*300f/Gdx.graphics.getHeight(),0,Gdx.graphics.getWidth()*600f/Gdx.graphics.getHeight(),600);
                            toqueMotor=toque(br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.calcular(v,true,.5).botoes().get(br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao.MOTOR));
                            Gdx.input.getInputProcessor().touchDown(toquePulso[0],toquePulso[1],1,0);
                            Gdx.input.getInputProcessor().touchDown(toqueMotor[0],toqueMotor[1],2,0);
                            mudar(11,"Teste de pulso e motor por dois toques simultâneos.");
                        }
                        break;
                    case 11:
                        if(t>.12) {
                            exigir(encontro.bateria()==3,"pulso por toque");
                            exigir(n.isMotorLigado(),"motor simultâneo ao pulso");
                            Gdx.input.getInputProcessor().touchUp(toquePulso[0],toquePulso[1],1,0);
                            mudar(12,"Soltar o pulso preserva o motor do outro dedo.");
                        }
                        break;
                    case 12:
                        if(t>.12) {
                            exigir(n.isMotorLigado(),"soltar um dedo cancelou o outro");
                            tela.pause();
                            exigir(controle.comandos().equals(Comandos.NENHUM),"pausa reteve motor");
                            Gdx.input.getInputProcessor().touchDown(toquePulso[0],toquePulso[1],1,0);
                            Gdx.input.getInputProcessor().touchDown(toqueMotor[0],toqueMotor[1],2,0);
                            runOnMainSync(()->app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                            mudar(14,"Pausa limpa entradas; redimensionamento para retrato com botões pressionados.");
                        }
                        break;
                    case 14:
                        if(t>1) {
                            exigir(controle.comandos().equals(Comandos.NENHUM),"resize reteve motor");
                            exigir(Gdx.graphics.getHeight()>Gdx.graphics.getWidth(),"orientação retrato");
                            runOnMainSync(()->app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                            mudar(15,"Retrato validado; retorno à orientação paisagem.");
                        }
                        break;
                    case 15:
                        if(t>.7) {
                            exigir(controle.comandos().equals(Comandos.NENHUM),"retorno reteve motor");
                            var base=voo.getCaverna().getBase();
                            posicionar(base.getCentroX(),base.getSuperficie()-voo.getConfig().peY()+5,0);
                            mudar(13,"Pouso na base para reposição da bateria.");
                        }
                        break;
                    case 13:
                        if(n.getEstado()==NaveEmVoo.Estado.POUSADA) {
                            exigir(encontro.bateria()==4,"recarga na base");
                            posicionar(encontro.inimigoX()-170,540,0);
                            mudar(3,"Aviso de ataque antes do disparo.");
                        } else if(t>2) throw new AssertionError("pouso de recarga não ocorreu");
                        break;
                    case 3:
                        // Mantém apenas o motor; o projétil e o escudo são os reais da simulação.
                        if(encontro.escudo()<2) {
                            exigir(encontro.tiros()>0,"projétil não disparado");
                            exigir(voo.getVidas()==3,"escudo não protegeu");
                            controle.teclaSolta(Input.Keys.UP);
                            posicionar(encontro.inimigoX()-170,540,-90);
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.F);
                            mudar(4,"Impacto absorvido pelo escudo; pulso EMP acionado.");
                        } else if(t>10) throw new AssertionError("projétil não atingiu: tiros="+encontro.tiros()+" estado="+encontro.estado()+" inimigoX="+encontro.inimigoX()+" nave="+n.getX()+","+n.getY());
                        else {
                            // Estabiliza a nave sem alterar o combate: motor corrige a queda.
                            if(n.getVelocidadeY()<-2) controle.teclaPressionada(Input.Keys.UP);
                            else controle.teclaSolta(Input.Keys.UP);
                        }
                        break;
                    case 4:
                        if(t>.12) {
                            Gdx.input.getInputProcessor().keyUp(Input.Keys.F);
                            exigir(encontro.estado()==EncontroDeResgate.Estado.DESATIVADO,"pulso não desativou");
                            exigir(encontro.bateria()==3,"carga do pulso");
                            mudar(5,"Patrulheiro desativado por quatro segundos.");
                        }
                        break;
                    case 5:
                        if(t>1.5) {
                            var p=voo.getCaverna().getPlataformas().get(1);
                            posicionar(p.getCentroX(),p.getSuperficie()+100,0);
                            mudar(6,"Aproximação real da plataforma de resgate.");
                        }
                        break;
                    case 6:
                        descer(n,voo.getCaverna().getPlataformas().get(1));
                        if(!n.getABordo().isEmpty()) {
                            controle.teclaSolta(Input.Keys.UP);
                            mudar(7,"Maria embarcou após pouso real.");
                        }
                        break;
                    case 7:
                        if(t>1.2) {
                            var base=voo.getCaverna().getBase();
                            posicionar(base.getCentroX(),base.getSuperficie()+90,0);
                            mudar(8,"Retorno à base para entrega.");
                        }
                        break;
                    case 8:
                        descer(n,voo.getCaverna().getBase());
                        if(voo.getEstado()==br.edu.unifor.missaomarte.aplicacao.Partida.Estado.VITORIA) {
                            controle.teclaSolta(Input.Keys.UP);
                            mudar(9,"Resgate concluído. Resultado local, sem submissão.");
                        }
                        break;
                    case 9:
                        if(jogo.getScreen()!=tela) {
                            exigir(jogo.getScreen().getClass().getSimpleName().equals("TelaResultadoDoEncontro"),"resultado próprio");
                            marco("Fluidez: "+medidos+" quadros, "+lentos+" intervalos acima de 50ms, máximo "+(maiorIntervalo/1_000_000)+"ms.");
                            resultadoDesde=System.nanoTime();
                            etapa=10;
                        }
                        break;
                    case 10:
                        if(System.nanoTime()-resultadoDesde>2_000_000_000L) {
                            fim.countDown();return;
                        }
                        break;
                }
                exigir(n.getEstado()!=NaveEmVoo.Estado.DESTRUIDA,"nave destruída na etapa "+etapa);
            }
            Gdx.app.postRunnable(this::quadro);
        } catch(Throwable e) { resultado=e.toString();fim.countDown(); }
    }
    int[] toque(br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo r) {
        float escala=Gdx.graphics.getHeight()/600f;
        float esquerda=400-Gdx.graphics.getWidth()/escala/2;
        return new int[]{Math.round((r.centroX()-esquerda)*escala),Math.round((600-r.centroY())*escala)};
    }
    void descer(NaveEmVoo n,PlataformaDePouso p) {
        if(n.getEstado()!=NaveEmVoo.Estado.VOANDO) { controle.teclaSolta(Input.Keys.UP);return; }
        double desejada=n.getY()-p.getSuperficie()>65?-23:-12;
        if(n.getVelocidadeY()<desejada) controle.teclaPressionada(Input.Keys.UP);
        else controle.teclaSolta(Input.Keys.UP);
    }
}
