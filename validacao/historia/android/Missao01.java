package ensaio;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import br.edu.unifor.missaomarte.grafico.*;
import br.edu.unifor.missaomarte.grafico.historia.*;
import br.edu.unifor.missaomarte.grafico.telas.*;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import br.edu.unifor.missaomarte.pilotagem.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.android.AndroidApplication;
import java.util.concurrent.*;

/** Jornada real por entradas normais. Não altera posição, física, recursos ou progresso. */
public class Missao01 extends Instrumentation {
    MissaoMarteGame jogo;
    TelaMissaoHistoria missao;
    final PilotoM01 piloto=new PilotoM01();
    final CountDownLatch fim=new CountDownLatch(1);
    volatile String resultado;
    String modo;
    int etapa, quadros, lentos;
    long ultimo, maximo;
    boolean decolou, patrulha, embarcou, retorno;
    long resultadoDesde, briefingDesde;
    int melhorAntes;
    double proximaAmostra=5;
    void marco(String texto) { Bundle b=new Bundle();b.putString("etapa",texto);sendStatus(1,b); }
    @Override public void onCreate(Bundle b) { super.onCreate(b);modo=b.getString("modo","voo");start(); }
    @Override public void onStart() {
        try {
            if(modo.equals("reabrir")) {
                var p=new HistoriaArquivo(getTargetContext().getFilesDir().toPath().resolve("historia/expedicao.properties")).carregar();
                if(!p.progresso().m01Concluida()) throw new AssertionError("conclusão ausente após reiniciar processo");
                melhorAntes=p.progresso().melhorPontuacao();
            }
            Intent i=getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            AndroidApplication app=(AndroidApplication)startActivitySync(i);
            runOnMainSync(()->{app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);app.getWindow().addFlags(128);});
            jogo=(MissaoMarteGame)app.getApplicationListener();
            Gdx.app.postRunnable(this::quadro);
            if(!fim.await(170,TimeUnit.SECONDS)) throw new AssertionError("tempo esgotado");
        } catch(Throwable e) {resultado=e.toString();}
        Bundle b=new Bundle();b.putString("resultado",resultado==null?"PASS":resultado);
        finish(resultado==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,b);
    }
    void exigir(boolean ok,String msg) {if(!ok)throw new AssertionError(msg);}
    void tecla(int key,boolean pressionada) {
        if(pressionada)Gdx.input.getInputProcessor().keyDown(key);
        else Gdx.input.getInputProcessor().keyUp(key);
    }
    void quadro() {
        try {
            long agora=System.nanoTime();
            if(etapa==4 && ultimo!=0){long d=agora-ultimo;maximo=Math.max(maximo,d);quadros++;if(d>50_000_000)lentos++;}
            ultimo=agora;
            switch(etapa) {
                case 0:
                    if(jogo.getScreen() instanceof TelaEntrada){tecla(Input.Keys.NUM_3,true);etapa=1;}
                    else if(jogo.getScreen() instanceof TelaMenu)etapa=1;
                    break;
                case 1:
                    if(jogo.getScreen() instanceof TelaMenu){tecla(Input.Keys.NUM_9,true);etapa=2;}
                    break;
                case 2:
                    if(jogo.getScreen() instanceof TelaCampanha) {
                        if(modo.equals("reabrir")) {
                            var campanha=(TelaCampanha)jogo.getScreen();
                            var p=campanha.progressoCarregado();
                            if(p!=null) {
                                exigir(p.m01Concluida() && p.melhorPontuacao()==melhorAntes,"mapa não carregou o progresso anterior");
                                marco("Processo reaberto: mapa carregou conclusão e melhor pontuação. Iniciando replay.");
                                tecla(Input.Keys.ENTER,true);etapa=7;
                            }
                        } else tecla(Input.Keys.ENTER,true);
                    } else if(jogo.getScreen() instanceof TelaBriefingHistoria){etapa=3;briefingDesde=agora;marco("Briefing e rádio da M01 pela entrada normal.");}
                    break;
                case 3:
                    if(jogo.getScreen() instanceof TelaBriefingHistoria && agora-briefingDesde>2_000_000_000L)tecla(Input.Keys.ENTER,true);
                    else if(jogo.getScreen() instanceof TelaMissaoHistoria){missao=(TelaMissaoHistoria)jogo.getScreen();etapa=4;marco("M01 iniciada na base. Voo contínuo sem reposicionamento.");}
                    break;
                case 4:
                    if(jogo.getScreen() instanceof TelaResultadoHistoria){etapa=5;break;}
                    var encontro=missao.getEncontro();var voo=encontro.partida();var n=voo.getNave();
                    exigir(n.getEstado()!=NaveEmVoo.Estado.DESTRUIDA,"nave destruída no trajeto");
                    if(voo.getTempo()>=proximaAmostra) {
                        Bundle b=new Bundle();b.putString("voo","t="+voo.getTempo()+" x="+n.getX()+" y="+n.getY()+" estado="+n.getEstado());sendStatus(2,b);proximaAmostra+=5;
                    }
                    if(!decolou && n.getY()>400){decolou=true;marco("Decolagem e subida para a rota aberta.");}
                    if(!patrulha && Math.abs(n.getX()-encontro.inimigoX())<350){patrulha=true;marco("Patrulheiro avistado; passagem por evasão.");}
                    if(!embarcou && !n.getABordo().isEmpty()){embarcou=true;marco("Pouso real e embarque de Maria.");}
                    if(embarcou && !retorno && n.getX()<850){retorno=true;marco("Retorno contínuo à base com Maria.");}
                    Comandos c=piloto.decidir(voo);
                    tecla(Input.Keys.LEFT,c.girarEsquerda());tecla(Input.Keys.RIGHT,c.girarDireita());tecla(Input.Keys.UP,c.motor());
                    break;
                case 5:
                    var tela=(TelaResultadoHistoria)jogo.getScreen();
                    if(tela.situacao().equals("Progresso salvo")) {
                        exigir(embarcou && retorno,"trajeto incompleto");
                        var progresso=new HistoriaArquivo(getTargetContext().getFilesDir().toPath().resolve("historia/expedicao.properties")).carregar();
                        exigir(progresso.progresso().m01Concluida(),"save não contém conclusão");
                        exigir(progresso.progresso().retomada().isEmpty(),"retomada pendente após concluir");
                        marco("M01 concluída e salva. "+quadros+" quadros; "+lentos+" acima de 50 ms; máximo "+(maximo/1_000_000)+" ms.");
                        resultadoDesde=agora;etapa=6;
                    } else if(tela.situacao().contains("Não foi possível"))throw new AssertionError(tela.situacao());
                    break;
                case 6:
                    if(agora-resultadoDesde>2_000_000_000L){fim.countDown();return;}
                    break;
                case 7:
                    if(jogo.getScreen() instanceof TelaBriefingHistoria){tecla(Input.Keys.ENTER,true);etapa=8;}
                    break;
                case 8:
                    if(jogo.getScreen() instanceof TelaMissaoHistoria) {
                        marco("Replay iniciado pela interface. Abortando a repetição.");
                        tecla(Input.Keys.ESCAPE,true);etapa=9;
                    }
                    break;
                case 9:
                    if(jogo.getScreen() instanceof TelaResultadoHistoria) {tecla(Input.Keys.ESCAPE,true);etapa=10;}
                    break;
                case 10:
                    if(jogo.getScreen() instanceof TelaCampanha) {
                        var p=((TelaCampanha)jogo.getScreen()).progressoCarregado();
                        if(p!=null) {
                            exigir(p.m01Concluida() && p.melhorPontuacao()==melhorAntes,"replay apagou ou alterou o resultado anterior");
                            marco("Replay abortado: conclusão e melhor pontuação preservadas no mapa.");
                            resultadoDesde=agora;etapa=6;
                        }
                    }
                    break;
            }
            Gdx.app.postRunnable(this::quadro);
        } catch(Throwable e){resultado=e.toString();fim.countDown();}
    }
}
