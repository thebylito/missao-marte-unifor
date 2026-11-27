package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.telas.TelaBase;
import br.edu.unifor.missaomarte.historia.RepositorioHistoria;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Entrada local da campanha. Só missões implementadas podem ser iniciadas. */
public final class TelaCampanha extends TelaBase {
    private final ServicoHistoria servico;
    private CompletableFuture<RepositorioHistoria.Leitura> carga;
    private RepositorioHistoria.Leitura leitura;
    private boolean confirmarReinicio;
    private int selecionada;
    private String aviso = "Carregando progresso...";
    public TelaCampanha(MissaoMarteGame jogo, ServicoHistoria servico) {
        super(jogo); this.servico = servico; recarregar();
    }
    public br.edu.unifor.missaomarte.historia.ProgressoDaHistoria progressoCarregado() {
        return leitura == null ? null : leitura.progresso();
    }
    private void recarregar() { leitura = null; carga = servico.carregar(); aviso = "Carregando progresso..."; }
    @Override protected void atualizar(float delta) {
        if (carga != null && carga.isDone()) {
            try { leitura = carga.join(); aviso = leitura.aviso(); }
            catch (RuntimeException erro) { aviso = "Não foi possível carregar. Tente novamente."; }
            carga = null;
        }
    }
    @Override protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if (confirmarReinicio) {
                    if (tecla == Keys.ESCAPE) { confirmarReinicio = false; aviso = leitura.aviso(); }
                    else if (tecla == Keys.ENTER) {
                        confirmarReinicio = false; leitura = null;
                        aviso = "Arquivando dados danificados..."; carga = servico.reiniciarAposCorrupcao();
                    }
                    return true;
                }
                if (tecla == Keys.N && leitura != null && leitura.permiteReiniciar()) {
                    confirmarReinicio = true;
                    aviso = "Arquivar dados danificados e recomeçar? Enter confirma; Esc cancela.";
                } else if (tecla >= Keys.NUM_1 && tecla <= Keys.NUM_3) {
                    selecionada=tecla-Keys.NUM_1; abrir();
                } else if (tecla == Keys.UP) {
                    selecionada=Math.max(0,selecionada-1);
                } else if (tecla == Keys.DOWN) {
                    selecionada=Math.min(2,selecionada+1);
                } else if (tecla == Keys.ENTER) {
                    abrir();
                } else if (tecla == Keys.ESCAPE) jogo.irParaMenu();
                else if (tecla == Keys.R && carga == null) recarregar();
                return true;
            }
        };
    }
    private void abrir() {
        String id=br.edu.unifor.missaomarte.historia.ProgressoDaHistoria.MISSOES.get(selecionada);
        if(leitura!=null && leitura.gravavel() && leitura.progresso().disponivel(id))
            jogo.trocarTela(new TelaBriefingHistoria(jogo,servico,br.edu.unifor.missaomarte.historia.MissaoHistoria.porId(id)));
    }
    private Retangulo cartao(int indice) {return new Retangulo(70,312-indice*96,660,84);}
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(cartao(0),confirmarReinicio?Keys.ENTER:Keys.NUM_1,(char)0),
                tecla(cartao(1),Keys.NUM_2,(char)0),tecla(cartao(2),Keys.NUM_3,(char)0),
                tecla(new Retangulo(70,20,190,54),Keys.ESCAPE,(char)0),
                tecla(new Retangulo(520,20,210,54),Keys.R,(char)0),
                tecla(new Retangulo(290,20,210,54),Keys.N,(char)0));
    }
    @Override protected void desenhar() {
        var progresso=progressoCarregado();
        desenharBotoes(List.of(cartao(0),cartao(1),cartao(2),new Retangulo(70,20,190,54)),
                List.of("","","",confirmarReinicio?"Cancelar":"Voltar"),1);
        lote().begin();
        escreverTitulo("HISTÓRIA",545,Color.ORANGE,2.3f);
        escreverCentralizado("Capítulo 1 — Sinais perdidos",493,Color.WHITE,1.25f);
        escreverCentralizado(progresso!=null && progresso.capituloConcluido()
            ? "CAPÍTULO CONCLUÍDO · Todos voltaram para casa" : "Três operações de resgate · campanha local",452,Color.CYAN,1f);
        var missoes=br.edu.unifor.missaomarte.historia.MissaoHistoria.todas();
        for(int i=0;i<missoes.size();i++) {
            var m=missoes.get(i);float y=cartao(i).y();
            boolean aberta=progresso!=null && progresso.disponivel(m.id());
            boolean concluida=progresso!=null && progresso.concluida(m.id());
            escrever((selecionada==i?"> ":"  ")+m.id()+"  "+m.titulo().toUpperCase(java.util.Locale.ROOT),90,y+59,
                aberta?Color.CYAN:Color.GRAY,1.15f);
            String estado=!aberta?"BLOQUEADA · conclua a missão anterior":concluida?"CONCLUÍDA · Rejogar":"INICIAR";
            if(progresso!=null && progresso.retomada().equals(m.id()))estado="RETOMAR DO INÍCIO";
            escrever(estado+"  ["+(i+1)+"]",105,y+27,concluida?Color.LIME:Color.LIGHT_GRAY,.85f);
            if(concluida)escrever("Recorde: "+progresso.melhorPontuacao(m.id()),525,y+27,Color.WHITE,.85f);
        }
        escreverComQuebra(aviso,72,104,656,Color.YELLOW,.8f);
        lote().end();
        if(leitura!=null && leitura.permiteReiniciar() && !confirmarReinicio)
            desenharBotoes(List.of(new Retangulo(290,20,210,54)),List.of("Nova expedição [N]"),.8f);
        if(!aviso.isEmpty() && carga==null && !confirmarReinicio)
            desenharBotoes(List.of(new Retangulo(520,20,210,54)),List.of("Tentar novamente [R]"),.8f);
    }
}
