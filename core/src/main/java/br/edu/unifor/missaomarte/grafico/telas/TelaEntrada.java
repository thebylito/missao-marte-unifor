package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Etapa;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Modo;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/**
 * Tela de entrada (Fase 5): entrar com uma conta, cadastrar ou jogar como convidado. Só desenha e repassa teclas;
 * quem decide é o {@link ControladorDaEntrada}. Quando o login termina, vai para o menu.
 */
public class TelaEntrada extends TelaBase {

    private final ControladorDaEntrada controlador;
    private float tempo;

    public TelaEntrada(MissaoMarteGame jogo) {
        super(jogo);
        this.controlador = new ControladorDaEntrada(jogo.getServicos().servidor());
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                if (controlador.teclaPressionada(tecla) == ControladorDaEntrada.Acao.CONVIDADO) {
                    jogo.jogarComoConvidado();
                }
                return true;
            }

            @Override
            public boolean keyTyped(char caractere) {
                controlador.digitar(caractere);
                return true;
            }
        };
    }

    @Override
    protected void atualizar(float delta) {
        tempo += delta;
        controlador.getSessao().ifPresent(jogo::entrarComo);
    }

    @Override
    protected void desenhar() {
        lote().begin();
        if (recursosProntos()) {
            desenharCentralizado(Folha.NAVE, 0, 400, 470 + 6 * (float) Math.sin(tempo * 1.8), 90);
        }
        escreverTitulo("MISSÃO MARTE UNIFOR", 395, Color.ORANGE, 2f);
        if (controlador.getEtapa() == Etapa.ESCOLHA) {
            desenharEscolha();
        } else {
            desenharFormulario();
        }
        Color corDaMensagem = controlador.getEtapa() == Etapa.AGUARDANDO ? Color.SKY : Color.YELLOW;
        escreverCentralizado(controlador.getMensagem(), 110, corDaMensagem, 1f);
        lote().end();
    }

    private void desenharEscolha() {
        String[] opcoes = {"1. Entrar com minha conta", "2. Criar uma conta", "3. Jogar como convidado"};
        float y = 320;
        for (String opcao : opcoes) {
            escrever(opcao, 260, y, Color.WHITE, 1.3f);
            y -= 40;
        }
        escreverCentralizado("Convidado: só o ranking local. Com conta: ranking global e histórico.", 175,
                Color.LIGHT_GRAY, 0.95f);
        escreverCentralizado("Escolha uma opção (teclas 1 a 3)", 60, Color.GRAY, 1f);
    }

    private void desenharFormulario() {
        escreverCentralizado(controlador.getModo() == Modo.ENTRAR ? "Entrar" : "Criar conta", 345, Color.SKY, 1.3f);
        boolean senha = controlador.isEditandoSenha();
        escrever("Nome:", 220, 290, Color.LIGHT_GRAY, 1.2f);
        escrever(controlador.getNome() + (senha ? "" : "_"), 330, 290, senha ? Color.LIGHT_GRAY : Color.WHITE, 1.2f);
        escrever("Senha:", 220, 240, Color.LIGHT_GRAY, 1.2f);
        escrever(controlador.getSenhaMascarada() + (senha ? "_" : ""), 330, 240, senha ? Color.WHITE : Color.LIGHT_GRAY,
                1.2f);
        escreverCentralizado("Tab: trocar de campo   |   Enter: continuar   |   Esc: voltar", 60, Color.GRAY, 1f);
    }
}
