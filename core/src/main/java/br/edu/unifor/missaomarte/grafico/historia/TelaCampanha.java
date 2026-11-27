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
                } else if (tecla == Keys.ENTER && leitura != null && leitura.gravavel()) {
                    jogo.trocarTela(new TelaBriefingHistoria(jogo, servico));
                } else if (tecla == Keys.ESCAPE) jogo.irParaMenu();
                else if (tecla == Keys.R && carga == null) recarregar();
                return true;
            }
        };
    }
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(new Retangulo(70,210,660,160),Keys.ENTER,(char)0),
                tecla(new Retangulo(70,32,190,64),Keys.ESCAPE,(char)0),
                tecla(new Retangulo(520,32,210,64),Keys.R,(char)0),
                tecla(new Retangulo(290,32,210,64),Keys.N,(char)0));
    }
    @Override protected void desenhar() {
        boolean concluida = progressoCarregado() != null && progressoCarregado().m01Concluida();
        desenharBotoes(List.of(new Retangulo(70,210,660,160),new Retangulo(70,32,190,64)),
                List.of("",confirmarReinicio ? "Cancelar" : "Voltar"),1);
        lote().begin();
        escreverTitulo("HISTÓRIA",545,Color.ORANGE,2.3f);
        escreverCentralizado("Capítulo 1 — Sinais perdidos",495,Color.WHITE,1.25f);
        escreverCentralizado("Expedição local · disponível sem conta",461,Color.LIGHT_GRAY,.95f);
        escrever("M01  PRIMEIRO SINAL",100,343,Color.CYAN,1.35f);
        escrever("Resgate Maria e restabeleça o primeiro contato.",100,303,Color.WHITE,1f);
        String acao = concluida ? "CONCLUÍDA · Rejogar  [Enter]" : leitura != null
                && !leitura.progresso().retomada().isEmpty() ? "CONTINUAR DO INÍCIO  [Enter]" : "INICIAR MISSÃO  [Enter]";
        if (confirmarReinicio) acao = "CONFIRMAR NOVA EXPEDIÇÃO  [Enter]";
        escrever(acao,100,258,concluida ? Color.LIME : Color.ORANGE,1.05f);
        if (concluida) escrever("Melhor resultado: " + leitura.progresso().melhorPontuacao(),450,258,Color.WHITE,.85f);
        escreverCentralizado(concluida ? "Maria está na base. Próxima chamada: A torre muda."
                : "Um sinal de socorro atravessou a tempestade.",197,Color.WHITE,1f);
        escreverCentralizado("M02 — A torre muda · em preparação",164,Color.GRAY,1f);
        escreverComQuebra(aviso,72,134,656,Color.YELLOW,.85f);
        lote().end();
        if (leitura != null && leitura.permiteReiniciar() && !confirmarReinicio) {
            desenharBotoes(List.of(new Retangulo(290,32,210,64)),List.of("Nova expedição [N]"),.85f);
        }
        if (!aviso.isEmpty() && carga == null && !confirmarReinicio) {
            desenharBotoes(List.of(new Retangulo(520,32,210,64)),List.of("Tentar novamente [R]"),.85f);
        }
    }
}
