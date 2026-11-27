package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.telas.TelaBase;
import br.edu.unifor.missaomarte.historia.ProgressoDaHistoria;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TelaResultadoHistoria extends TelaBase {
    private final ServicoHistoria servico;
    private final br.edu.unifor.missaomarte.historia.MissaoHistoria missao;
    private final boolean vitoria;
    private final int pontos;
    private CompletableFuture<ProgressoDaHistoria> gravacao;
    private String situacao;
    private boolean salvo;
    public TelaResultadoHistoria(MissaoMarteGame jogo, ServicoHistoria servico, Partida.Estado estado, int pontos) {
        this(jogo,servico,estado,pontos,br.edu.unifor.missaomarte.historia.MissaoHistoria.porId("M01"));
    }
    public TelaResultadoHistoria(MissaoMarteGame jogo,ServicoHistoria servico,Partida.Estado estado,int pontos,
            br.edu.unifor.missaomarte.historia.MissaoHistoria missao) {
        super(jogo);this.missao=missao; this.servico = servico; this.pontos = pontos; vitoria = estado == Partida.Estado.VITORIA;
        situacao = vitoria ? "Salvando progresso..." : "Você pode reiniciar a missão pela entrada.";
        if (vitoria) gravacao = servico.concluir(missao.id(),pontos);
    }
    public String situacao() { return situacao; }
    @Override protected void atualizar(float delta) {
        if (gravacao != null && gravacao.isDone()) {
            try { gravacao.join(); salvo = true; situacao = "Progresso salvo"; }
            catch (RuntimeException erro) { situacao = "Não foi possível salvar. Toque em tentar novamente."; }
            gravacao = null;
        }
    }
    @Override protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if (gravacao != null) return true;
                if (tecla == Keys.R && vitoria && !salvo) {
                    situacao = "Salvando progresso..."; gravacao = servico.concluir(missao.id(),pontos);
                } else if(tecla == Keys.N && vitoria && salvo && !missao.id().equals("M03")) {
                    String proxima=missao.id().equals("M01")?"M02":"M03";
                    jogo.trocarTela(new TelaBriefingHistoria(jogo,servico,br.edu.unifor.missaomarte.historia.MissaoHistoria.porId(proxima)));
                } else if (tecla == Keys.ENTER && (!vitoria || salvo)) {
                    jogo.trocarTela(new TelaBriefingHistoria(jogo,servico,missao));
                } else if (tecla == Keys.ESCAPE) jogo.trocarTela(new TelaCampanha(jogo,servico));
                return true;
            }
        };
    }
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(new Retangulo(70,70,220,72),Keys.ENTER,(char)0),
                tecla(new Retangulo(310,70,200,72),Keys.ESCAPE,(char)0),
                tecla(new Retangulo(530,70,220,72),salvo?Keys.N:Keys.R,(char)0));
    }
    @Override protected void desenhar() {
        lote().begin();
        escreverTitulo(vitoria ? missao.id().equals("M03")?"CAPÍTULO CONCLUÍDO":"MISSÃO CONCLUÍDA" : "MISSÃO INTERROMPIDA",500,Color.ORANGE,1.9f);
        escreverCentralizado(missao.id()+" — "+missao.titulo(),449,Color.WHITE,1.25f);
        escreverComQuebra(vitoria?missao.encerramento():"O pedido de socorro continua ativo.",90,388,620,Color.CYAN,1f);
        escreverCentralizado("Pontos: "+pontos,300,Color.WHITE,1.2f);
        escreverCentralizado(vitoria && missao.id().equals("M03") ? "Maria, Rosa e Silva estão a salvo. Obrigado por pilotar!"
            : "Reveja sua rota ou siga para a próxima operação.",252,Color.LIGHT_GRAY,.9f);
        escreverComQuebra(situacao,90,204,620,salvo ? Color.LIME : Color.YELLOW,1f);
        lote().end();
        desenharBotoes(List.of(new Retangulo(70,70,220,72),new Retangulo(310,70,200,72)),
                List.of("Rejogar","Mapa"),1f);
        if(vitoria && salvo && !missao.id().equals("M03")) {
            desenharBotoes(List.of(new Retangulo(530,70,220,72)),List.of("Próxima [N]"),.9f);
        }
        if (vitoria && !salvo && gravacao == null) {
            desenharBotoes(List.of(new Retangulo(530,70,220,72)),List.of("Tentar novamente"),.9f);
        }
    }
}
