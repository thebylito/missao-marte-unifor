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
    private final MissaoHistoria missao;
    private CompletableFuture<ProgressoDaHistoria> inicio;
    private String situacao = "";
    public TelaBriefingHistoria(MissaoMarteGame jogo, ServicoHistoria servico) { this(jogo,servico,MissaoHistoria.porId("M01")); }
    public TelaBriefingHistoria(MissaoMarteGame jogo,ServicoHistoria servico,MissaoHistoria missao) {
        super(jogo);this.servico=servico;this.missao=missao;
    }
    public MissaoHistoria missao(){return missao;}
    public String situacao() { return situacao; }
    @Override protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if ((tecla == Keys.ENTER || tecla == Keys.SPACE) && inicio == null) {
                    situacao = "Preparando missão..."; inicio = servico.iniciar(missao.id());
                } else if (tecla == Keys.ESCAPE && inicio == null) {
                    jogo.trocarTela(new TelaCampanha(jogo, servico));
                }
                return true;
            }
        };
    }
    @Override protected void atualizar(float delta) {
        if (inicio != null && inicio.isDone()) {
            try { inicio.join(); inicio = null; jogo.iniciarMissaoHistoria(servico,missao); }
            catch (RuntimeException erro) { inicio = null; situacao = "Não foi possível salvar. Confira o armazenamento e tente novamente."; }
        }
    }
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(new Retangulo(410,48,300,72),Keys.ENTER,(char)0),
                tecla(new Retangulo(90,48,240,72),Keys.ESCAPE,(char)0));
    }
    @Override protected void desenhar() {
        lote().begin();
        escreverTitulo(missao.id()+" — "+missao.titulo().toUpperCase(java.util.Locale.ROOT),535,Color.ORANGE,1.7f);
        escrever("RÁDIO · EXPEDIÇÃO",90,465,Color.CYAN,1.2f);
        escreverComQuebra(missao.briefing(),90,416,620,Color.WHITE,1.05f);
        escreverComQuebra(missao.orientacao(),90,294,620,Color.SKY,.95f);
        escreverComQuebra("Pilotagem: motor e giro. Pulso: F / botão PULSO. Assistência opcional: G.",90,202,620,Color.LIGHT_GRAY,.85f);
        escreverComQuebra(situacao,90,150,620,Color.YELLOW,.9f);
        lote().end();
        desenharBotoes(List.of(new Retangulo(410,48,300,72),new Retangulo(90,48,240,72)),
                List.of("Decolar / pular rádio","Mapa"),1f);
    }
}
