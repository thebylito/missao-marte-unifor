package ensaio;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.historia.*;
import br.edu.unifor.missaomarte.grafico.telas.*;
import br.edu.unifor.missaomarte.historia.ProgressoDaHistoria;
import br.edu.unifor.missaomarte.infraestrutura.HistoriaArquivo;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Percurso pela interface real; só lê o domínio e aplica teclas ao processador da tela. */
public final class CapituloUm extends Instrumentation {
    private final CountDownLatch fim = new CountDownLatch(1);
    private final Set<String> concluidasNoVoo = new LinkedHashSet<>();
    private MissaoMarteGame jogo;
    private AndroidApplication atividade;
    private TelaMissaoHistoria missao;
    private PilotoCapituloUm piloto;
    private volatile String erro;
    private volatile boolean encerrado;
    private Bundle argumentos;
    private String modo, id = "M01";
    private int etapa, indice, quadros, lentos, aBordoAntes, entreguesAntes;
    private long desde, ultimo, maximo;
    private double proximaAmostra;
    private boolean decolou, patrulha, sentinela, ativando, antenaAtiva;
    private Map<String,Integer> anteriores, referencia;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        argumentos = b == null ? new Bundle() : b;
        modo = argumentos.getString("modo", "voo");
        start();
    }

    @Override public void onStart() {
        try {
            exigir(modo.equals("voo") || modo.equals("reabrir"), "modo desconhecido: " + modo);
            anteriores = new LinkedHashMap<>(carregar().resultados());
            if (modo.equals("reabrir")) {
                ProgressoDaHistoria p = carregar();
                exigirCapitulo(p);
                exigir(p.retomada().isEmpty(), "retomada pendente antes do replay");
                referencia = new LinkedHashMap<>(p.resultados());
                for (String m : ProgressoDaHistoria.MISSOES) {
                    String esperado = argumentos.getString("esperado_" + m);
                    if (esperado != null) exigir(referencia.get(m) == Integer.parseInt(esperado),
                            "pontuação mudou ao reabrir: " + m);
                }
                id = "M02";
            }
            Intent intent = getTargetContext().getPackageManager()
                    .getLaunchIntentForPackage(getTargetContext().getPackageName());
            exigir(intent != null, "atividade inicial ausente");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            AndroidApplication app = (AndroidApplication) startActivitySync(intent);
            atividade = app;
            runOnMainSync(() -> {
                app.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                app.getWindow().addFlags(128);
            });
            jogo = (MissaoMarteGame) app.getApplicationListener();
            Gdx.app.postRunnable(this::quadro);
            if (!fim.await(modo.equals("voo") ? 450 : 60, TimeUnit.SECONDS))
                throw new AssertionError("tempo esgotado: modo=" + modo + " etapa=" + etapa + " missão=" + id);
        } catch (Throwable e) {
            erro = e.toString();
        } finally {
            encerrado = true;
        }
        Bundle resposta = new Bundle();
        resposta.putString("resultado", erro == null ? "PASS" : erro);
        finish(erro == null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, resposta);
    }

    private ProgressoDaHistoria carregar() throws Exception {
        return new HistoriaArquivo(getTargetContext().getFilesDir().toPath()
                .resolve("historia/expedicao.properties")).carregar().progresso();
    }
    private void exigir(boolean ok, String mensagem) { if (!ok) throw new AssertionError(mensagem); }
    private void exigirCapitulo(ProgressoDaHistoria p) {
        for (String m : ProgressoDaHistoria.MISSOES) exigir(p.concluida(m), "conclusão ausente: " + m);
        exigir(p.capituloConcluido() && p.resultados().size() == 3, "capítulo incompleto");
    }
    private void preservar(ProgressoDaHistoria p) {
        exigirCapitulo(p);
        exigir(p.resultados().equals(referencia), "replay alterou as três pontuações: " + p.resultados());
    }
    private void marco(String texto) {
        Bundle b = new Bundle();
        b.putString("etapa", id + ": " + texto);
        sendStatus(1, b);
    }
    private void pontuacoes(ProgressoDaHistoria p) {
        Bundle b = new Bundle();
        b.putString("pontuacoes", "M01:" + p.resultados().get("M01") + ",M02:"
                + p.resultados().get("M02") + ",M03:" + p.resultados().get("M03"));
        sendStatus(2, b);
    }
    private void tecla(int codigo, boolean pressionada) {
        if (pressionada) Gdx.input.getInputProcessor().keyDown(codigo);
        else Gdx.input.getInputProcessor().keyUp(codigo);
    }
    private void tocar(int codigo) { tecla(codigo, true); tecla(codigo, false); }
    private void soltarVoo() {
        tecla(Input.Keys.LEFT, false); tecla(Input.Keys.RIGHT, false); tecla(Input.Keys.UP, false);
    }
    private void briefing(long agora) {
        TelaBriefingHistoria t = (TelaBriefingHistoria) jogo.getScreen();
        exigir(t.missao().id().equals(id), "briefing incorreto: " + t.missao().id());
        desde = agora;
        etapa = 3;
        marco("Briefing e rádio pela entrada normal.");
    }
    private void iniciarVoo() {
        missao = (TelaMissaoHistoria) jogo.getScreen();
        exigir(missao.missao().id().equals(id), "missão incorreta: " + missao.missao().id());
        piloto = new PilotoCapituloUm(false);
        quadros = lentos = aBordoAntes = entreguesAntes = 0;
        ultimo = maximo = 0;
        proximaAmostra = 5;
        decolou = patrulha = sentinela = ativando = antenaAtiva = false;
        etapa = modo.equals("voo") ? 4 : 8;
        marco(modo.equals("voo") ? "Voo contínuo iniciado na base." : "Replay M02 iniciado pela interface.");
    }

    private void observarVoo(long agora) {
        if (ultimo != 0) {
            long intervalo = agora - ultimo;
            maximo = Math.max(maximo, intervalo); quadros++;
            if (intervalo > 50_000_000L) lentos++;
        }
        ultimo = agora;
        var encontro = missao.getEncontro();
        var p = encontro.partida();
        var n = p.getNave();
        exigir(n.getEstado() != NaveEmVoo.Estado.DESTRUIDA, id + ": nave destruída no trajeto");
        if (!decolou && n.getY() > 400) { decolou = true; marco("Decolagem e subida para a rota aberta."); }
        if (!patrulha && encontro.patrulhaAtiva() && Math.abs(n.getX() - encontro.inimigoX()) < 400) {
            patrulha = true; marco("Patrulheiro avistado durante o trajeto.");
        }
        if (!sentinela && encontro.sentinela() != null && Math.abs(n.getX() - encontro.sentinela().x()) < 500) {
            sentinela = true; marco("Sentinela de varredura avistada durante o trajeto.");
        }
        if (n.getABordo().size() > aBordoAntes) marco("Pouso e embarque real: " + n.getABordo().size() + " a bordo.");
        aBordoAntes = n.getABordo().size();
        if (p.getEntregues() > entreguesAntes) marco("Entrega real na base: " + p.getEntregues() + " resgatado(s).");
        entreguesAntes = p.getEntregues();
        var antena = encontro.antena();
        if (antena != null) {
            if (!ativando && antena.progresso() > .1) { ativando = true; marco("Antena ativando com Rosa a bordo e nave pousada."); }
            if (!antenaAtiva && antena.ativa()) { antenaAtiva = true; marco("Antena ativa após permanência real na plataforma."); }
        }
        if (p.getTempo() >= proximaAmostra) {
            Bundle b = new Bundle();
            b.putString("voo", id + " t=" + p.getTempo() + " x=" + n.getX() + " y=" + n.getY()
                    + " estado=" + n.getEstado() + " entregues=" + p.getEntregues()
                    + " antena=" + (antena == null ? "ausente" : antena.progresso()));
            sendStatus(2, b); proximaAmostra += 5;
        }
        Comandos c = piloto.decidir(encontro);
        tecla(Input.Keys.LEFT, c.girarEsquerda());
        tecla(Input.Keys.RIGHT, c.girarDireita());
        tecla(Input.Keys.UP, c.motor());
    }

    private void quadro() {
        if (encerrado) return;
        try {
            long agora = System.nanoTime();
            switch (etapa) {
                case 0:
                    if (jogo.getScreen() instanceof TelaEntrada) { tocar(Input.Keys.NUM_3); etapa = 1; }
                    else if (jogo.getScreen() instanceof TelaMenu) etapa = 1;
                    break;
                case 1:
                    if (jogo.getScreen() instanceof TelaMenu) { tocar(Input.Keys.NUM_9); etapa = 2; }
                    break;
                case 2:
                    if (jogo.getScreen() instanceof TelaCampanha) {
                        var p = ((TelaCampanha) jogo.getScreen()).progressoCarregado();
                        if (p != null) {
                            if (modo.equals("reabrir")) { preservar(p); marco("Processo reaberto: capítulo e três pontuações carregados no mapa."); }
                            else marco("Mapa carregado; início explícito pela M01, preservando progresso existente.");
                            tocar(modo.equals("voo") ? Input.Keys.NUM_1 : Input.Keys.NUM_2);
                        }
                    } else if (jogo.getScreen() instanceof TelaBriefingHistoria) briefing(agora);
                    break;
                case 3:
                    if (jogo.getScreen() instanceof TelaBriefingHistoria) {
                        String situacao = ((TelaBriefingHistoria) jogo.getScreen()).situacao();
                        exigir(!situacao.contains("Não foi possível"), situacao);
                        if (agora - desde > 1_200_000_000L) tocar(Input.Keys.ENTER);
                    } else if (jogo.getScreen() instanceof TelaMissaoHistoria) iniciarVoo();
                    break;
                case 4:
                    if (jogo.getScreen() instanceof TelaResultadoHistoria) { soltarVoo(); etapa = 5; }
                    else observarVoo(agora);
                    break;
                case 5:
                    var partida = missao.getEncontro().partida();
                    exigir(partida.getEstado() == Partida.Estado.VITORIA && partida.getResultado().isPresent(), id + ": não houve vitória real");
                    exigir(partida.getEntregues() == (id.equals("M03") ? 2 : 1), id + ": entregas incompletas");
                    if (id.equals("M02")) exigir(ativando && antenaAtiva && missao.getEncontro().antena().ativa(), "M02: ativação não observada");
                    String situacao = ((TelaResultadoHistoria) jogo.getScreen()).situacao();
                    exigir(!situacao.contains("Não foi possível"), situacao);
                    if (situacao.equals("Progresso salvo")) {
                        var p = carregar();
                        exigir(p.concluida(id) && p.retomada().isEmpty(), id + ": conclusão não persistida");
                        exigir(p.resultados().get(id) >= partida.getPontos(), id + ": pontos da vitória não persistidos");
                        for (var entrada : anteriores.entrySet()) exigir(p.resultados().getOrDefault(entrada.getKey(), -1) >= entrada.getValue(), "recorde anterior reduzido");
                        anteriores = new LinkedHashMap<>(p.resultados());
                        concluidasNoVoo.add(id);
                        marco("Vitória e save confirmados. " + quadros + " intervalos; " + lentos + " acima de 50 ms; máximo " + maximo / 1_000_000.0 + " ms.");
                        desde = agora; etapa = 6;
                    }
                    break;
                case 6:
                    if (agora - desde > 1_500_000_000L) {
                        if (indice < 2) { indice++; id = ProgressoDaHistoria.MISSOES.get(indice); tocar(Input.Keys.N); etapa = 7; }
                        else {
                            exigir(concluidasNoVoo.size() == 3, "não percorreu todas as três missões");
                            var p = carregar(); exigirCapitulo(p); referencia = new LinkedHashMap<>(p.resultados()); pontuacoes(p);
                            marco("Capítulo completo; voltando ao mapa com Escape.");
                            tocar(Input.Keys.ESCAPE); etapa = 10;
                        }
                    }
                    break;
                case 7:
                    if (jogo.getScreen() instanceof TelaBriefingHistoria) briefing(agora);
                    break;
                case 8:
                    runOnMainSync(() -> atividade.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                    desde = agora; etapa = 13;
                    break;
                case 13:
                    if (agora - desde > 1_000_000_000L && Gdx.graphics.getHeight() > Gdx.graphics.getWidth()) {
                        exigir(jogo.getScreen() == missao, "replay saiu do voo durante o resize para retrato");
                        marco("Replay M02 em retrato após resize real: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight() + ".");
                        desde = agora; etapa = 14;
                    }
                    break;
                case 14:
                    if (agora - desde > 1_000_000_000L) {
                        // Escape é entrada normal de aborto; nenhuma mutação direta da partida.
                        tocar(Input.Keys.ESCAPE); etapa = 9;
                    }
                    break;
                case 9:
                    if (jogo.getScreen() instanceof TelaResultadoHistoria) {
                        exigir(missao.getEncontro().partida().getEstado() == Partida.Estado.ABORTADA, "replay não abortado");
                        preservar(carregar()); marco("Replay M02 abortado; três recordes preservados no arquivo.");
                        desde = agora; etapa = 11;
                    }
                    break;
                case 11:
                    if (agora - desde > 1_000_000_000L) { tocar(Input.Keys.ESCAPE); etapa = 10; }
                    break;
                case 10:
                    if (jogo.getScreen() instanceof TelaCampanha) {
                        var p = ((TelaCampanha) jogo.getScreen()).progressoCarregado();
                        if (p != null) {
                            preservar(p); preservar(carregar()); pontuacoes(p);
                            if (modo.equals("reabrir")) {
                                runOnMainSync(() -> atividade.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                                desde = agora; etapa = 15;
                            } else {
                                marco("Mapa confirma as três conclusões e capítulo completo.");
                                desde = agora; etapa = 12;
                            }
                        }
                    }
                    break;
                case 15:
                    if (agora - desde > 1_000_000_000L && Gdx.graphics.getWidth() > Gdx.graphics.getHeight()) {
                        exigir(jogo.getScreen() instanceof TelaCampanha, "mapa ausente após voltar à paisagem");
                        var p = ((TelaCampanha) jogo.getScreen()).progressoCarregado();
                        exigir(p != null, "mapa sem progresso após resize");
                        preservar(p); preservar(carregar());
                        marco("Replay abortado: mapa em paisagem e save mantêm capítulo completo e três pontuações idênticas.");
                        desde = agora; etapa = 12;
                    }
                    break;
                case 12:
                    if (agora - desde > 3_000_000_000L) { encerrado = true; fim.countDown(); return; }
                    break;
                default: throw new AssertionError("etapa inválida");
            }
            Gdx.app.postRunnable(this::quadro);
        } catch (Throwable e) { erro = e.toString(); encerrado = true; fim.countDown(); }
    }
}
