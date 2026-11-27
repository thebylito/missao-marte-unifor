package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.telas.TelaBase;
import br.edu.unifor.missaomarte.historia.MissaoHistoria;
import br.edu.unifor.missaomarte.historia.ProgressoDaHistoria;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Briefing curto; o jogador decola quando quiser, depois de persistir a retomada. */
public final class TelaBriefingHistoria extends TelaBase {
    private final ServicoHistoria servico;
    private CompletableFuture<ProgressoDaHistoria> inicio;
    private String situacao = "";
    public TelaBriefingHistoria(MissaoMarteGame jogo, ServicoHistoria servico) { super(jogo); this.servico = servico; }
    public String situacao() { return situacao; }
    @Override protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if ((tecla == Keys.ENTER || tecla == Keys.SPACE) && inicio == null) {
                    situacao = "Preparando missão..."; inicio = servico.iniciarM01();
                } else if (tecla == Keys.ESCAPE && inicio == null) {
                    jogo.trocarTela(new TelaCampanha(jogo, servico));
                }
                return true;
            }
        };
    }
    @Override protected void atualizar(float delta) {
        if (inicio != null && inicio.isDone()) {
            try { inicio.join(); inicio = null; jogo.iniciarM01(servico); }
            catch (RuntimeException erro) { inicio = null; situacao = "Não foi possível salvar. Confira o armazenamento e tente novamente."; }
        }
    }
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(new Retangulo(410,48,300,72),Keys.ENTER,(char)0),
                tecla(new Retangulo(90,48,240,72),Keys.ESCAPE,(char)0));
    }
    @Override protected void desenhar() {
        lote().begin();
        escreverTitulo("M01 — PRIMEIRO SINAL",535,Color.ORANGE,1.9f);
        escrever("RÁDIO · MARIA",90,465,Color.CYAN,1.2f);
        escreverComQuebra("Base, vocês me ouvem? Minha baliza ainda funciona. Estou além da formação rochosa.",90,421,620,Color.WHITE,1.1f);
        escreverComQuebra(MissaoHistoria.BRIEFING,90,333,620,Color.LIGHT_GRAY,.95f);
        escreverComQuebra("Suba, gire para seguir até a baliza azul e pouse devagar. Depois do embarque, volte ao marcador verde da base.",90,235,620,Color.SKY,.95f);
        escreverComQuebra(situacao,90,150,620,Color.YELLOW,.9f);
        lote().end();
        desenharBotoes(List.of(new Retangulo(410,48,300,72),new Retangulo(90,48,240,72)),
                List.of("Decolar / pular rádio","Mapa"),1f);
    }
}
