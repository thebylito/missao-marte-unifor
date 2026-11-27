package ensaio;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import br.edu.unifor.missaomarte.grafico.*;
import br.edu.unifor.missaomarte.grafico.apresentacao.*;
import br.edu.unifor.missaomarte.grafico.online.*;
import br.edu.unifor.missaomarte.grafico.telas.*;
import br.edu.unifor.missaomarte.pilotagem.*;
import br.edu.unifor.missaomarte.pilotagem.horda.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.utils.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import java.util.Scanner;

public class Polar extends Instrumentation {
    private int passageiro;
    private String skin;
    MissaoMarteGame jogo;
    HordaAtual horda;
    ControladorDaPilotagem controle;
    PlataformaDePouso alvo;
    int perto, cristais, quadros, quadrosDeAceno;
    boolean pousou;
    float maiorAbertura;
    boolean portaMostrada;
    boolean voltando;
    boolean bracoMostrado;
    boolean entregaMostrada;
    double inicioRetorno;
    double instantePouso;
    float maiorBraco;
    float maiorCompressao;
    volatile String resultado;
    final CountDownLatch fim = new CountDownLatch(1);
    @Override public void onCreate(Bundle b) { super.onCreate(b); passageiro = Integer.parseInt(b.getString("passageiro", "1")); skin = b.getString("skin", "padrao"); start(); }
    void marco(String texto) { Bundle b = new Bundle(); b.putString("etapa", texto); sendStatus(1,b); }
    @Override public void onStart() {
        try {
            String json;
            try (Scanner s = new Scanner(getContext().getAssets().open("horda.json"), "UTF-8")) { json = s.useDelimiter("\\A").next(); }
            Method ler=Class.forName("br.edu.unifor.missaomarte.grafico.online.LeitorDaHorda").getDeclaredMethod("atual",JsonValue.class);
            ler.setAccessible(true);
            horda=(HordaAtual)ler.invoke(null,new JsonReader().parse(json));
            Intent i = getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            AndroidApplication app=(AndroidApplication)startActivitySync(i);
            runOnMainSync(()->{app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);app.getWindow().addFlags(128);});
            jogo=(MissaoMarteGame)app.getApplicationListener();
            Gdx.app.postRunnable(this::quadro);
            if(!fim.await(70,TimeUnit.SECONDS)) throw new IllegalStateException("tempo esgotado");
        } catch(Throwable e) { resultado=e.toString(); }
        Bundle b=new Bundle(); b.putString("resultado",resultado==null?"PASS":resultado);
        finish(resultado==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,b);
    }
    void campo(NaveEmVoo nave,String nome,Object valor) throws Exception {
        Field f=NaveEmVoo.class.getDeclaredField(nome); f.setAccessible(true); f.set(nave,valor);
    }
    void iniciar() throws Exception {
        var desc=horda.descricao(); var montagem=MontadorDaHorda.montar(desc);
        var voo=new PartidaDePilotagem("Ensaio polar",desc.dificuldade(),montagem.caverna(),montagem.config());
        var ctor=ControladorDaPilotagem.class.getDeclaredConstructor(PartidaDePilotagem.class,long.class,String.class,String.class);
        ctor.setAccessible(true);
        controle=ctor.newInstance(voo,desc.numero(),null,desc.caverna().id());
        alvo=voo.getCaverna().getPlataformas().get(passageiro);
        var nave=voo.getNave(); campo(nave,"x",alvo.getCentroX()); campo(nave,"y",alvo.getSuperficie()+285);
        campo(nave,"estado",NaveEmVoo.Estado.VOANDO); campo(nave,"plataforma",null);
        if(!controle.noPocoPolar() || voo.getCaverna().getAsteroides().size()!=2) throw new AssertionError("caverna");
        jogo.trocarTela(new TelaPilotagem(jogo,controle));
        jogo.getNaves().selecionar(skin);
        marco("Poço Polar carregado. Passageiro " + passageiro + ". Skin " + skin + ". Início da aproximação.");
    }
    void quadro() {
        try {
            if(controle==null) {
                if(jogo.getScreen() instanceof TelaEntrada) iniciar();
            } else {
                var voo=controle.getPartida(); var n=voo.getNave(); double h=n.getY()-alvo.getSuperficie();
                Field campoPorta=TelaPilotagem.class.getDeclaredField("portaDaNave");
                campoPorta.setAccessible(true);
                PortaDaNave porta=(PortaDaNave)campoPorta.get(jogo.getScreen());
                Field campoAnimacao=TelaPilotagem.class.getDeclaredField("animacaoDaNave");
                campoAnimacao.setAccessible(true);
                AnimacaoDaNave animacao=(AnimacaoDaNave)campoAnimacao.get(jogo.getScreen());
                maiorBraco=Math.max(maiorBraco,animacao.braco());
                maiorCompressao=Math.max(maiorCompressao,animacao.compressao());
                if (voltando && !bracoMostrado && animacao.braco()>=.99f) {
                    bracoMostrado=true;
                    marco("Braço conectado e reabastecendo.");
                }
                maiorAbertura=Math.max(maiorAbertura,porta.abertura());
                if (!portaMostrada && porta.abertura()>=.99f) {
                    portaMostrada=true;
                    marco("Porta aberta para o passageiro.");
                }
                quadros++;
                if (!voltando && voo.getTempo()>5 && h>50 && h<240 && n.getEstado()==NaveEmVoo.Estado.VOANDO) {
                    Field campoAnimacoes=TelaPilotagem.class.getDeclaredField("animacoes");
                    campoAnimacoes.setAccessible(true);
                    var animacoes=(java.util.Map<?,?>)campoAnimacoes.get(jogo.getScreen());
                    var passageiro=(AnimacaoDoPassageiro)animacoes.get(alvo);
                    if (passageiro==null || passageiro.fase()!=AnimacaoDoPassageiro.Fase.SINALIZAR)
                        throw new AssertionError("aceno interrompido durante a aproximação");
                    quadrosDeAceno++;
                }
                if(n.getEstado()==NaveEmVoo.Estado.DESTRUIDA) throw new AssertionError("colisão");
                if(n.getEstado()==NaveEmVoo.Estado.VOANDO) {
                    double velocidade=voo.getTempo()<3?0:(h>140?-25:-12);
                    if(n.getVelocidadeY()<velocidade) controle.teclaPressionada(Input.Keys.UP);
                    else controle.teclaSolta(Input.Keys.UP);
                    if(Math.hypot(44,h)<270 && perto++==0) marco("Passageiro ao alcance do aceno.");
                    if(n.isMotorLigado() && h>=28 && h<138 && cristais++==0) marco("Jato ao alcance da plataforma.");
                } else {
                    controle.teclaSolta(Input.Keys.UP);
                    if(!pousou) { if(n.getPlataforma()!=alvo) throw new AssertionError("plataforma"); pousou=true; instantePouso=voo.getTempo(); marco("Pouso confirmado."); }
                    if(!voltando && !n.getABordo().isEmpty() && voo.getTempo()>instantePouso+1.2) {
                        if(cristais<2 || perto<2 || quadrosDeAceno<10) throw new AssertionError("efeitos ou aceno");
                        marco("Aceno mantido em " + quadrosDeAceno + " quadros da aproximação.");
                        if(maiorAbertura<.99f || porta.abertura()!=0) throw new AssertionError("porta");
                        marco("Porta abriu completamente e fechou após o embarque.");
                        marco("Embarque confirmado. "+quadros+" atualizações; "+cristais+" quadros com motor na faixa dos cristais.");
                        voltando=true;
                        inicioRetorno=voo.getTempo();
                        alvo=voo.getCaverna().getBase();
                        pousou=false;
                        campo(n,"x",alvo.getCentroX());
                        campo(n,"y",alvo.getSuperficie()+100);
                        campo(n,"velocidadeX",0.0);
                        campo(n,"velocidadeY",-20.0);
                        campo(n,"combustivel",45.0);
                        campo(n,"estado",NaveEmVoo.Estado.VOANDO);
                        campo(n,"plataforma",null);
                        marco("Aproximação da base para entrega e abastecimento.");
                    }
                    if (voltando && n.getPlataforma()==voo.getCaverna().getBase() && n.getABordo().isEmpty()) {
                        if (!entregaMostrada) {
                            entregaMostrada=true;
                            marco("Passageiro entregue. Caminhada até o abrigo.");
                        }
                        if (voo.getTempo()>inicioRetorno+11) {
                            if (maiorBraco<.99f || maiorCompressao<=0) throw new AssertionError("base ou suspensão");
                            marco("Entrega, suspensão e reabastecimento confirmados.");
                            fim.countDown(); return;
                        }
                    }
                }
            }
            Gdx.app.postRunnable(this::quadro);
        } catch(Throwable e) { resultado=e.toString(); fim.countDown(); }
    }
}
