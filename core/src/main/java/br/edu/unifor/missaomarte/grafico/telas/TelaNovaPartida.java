package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;

/**
 * As perguntas antes da partida. O nome chega por {@code keyTyped} (o caractere já com acento e
 * maiúscula); as setas e o Enter chegam por {@code keyDown} (o código da tecla).
 */
public class TelaNovaPartida extends TelaBase {

    private final FormularioNovaPartida formulario = new FormularioNovaPartida();

    public TelaNovaPartida(MissaoMarteGame jogo) {
        super(jogo);
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                switch (tecla) {
                    case Keys.LEFT: formulario.dificuldadeAnterior(); break;
                    case Keys.RIGHT: formulario.proximaDificuldade(); break;
                    case Keys.UP: formulario.aumentarMapa(); break;
                    case Keys.DOWN: formulario.diminuirMapa(); break;
                    case Keys.BACKSPACE: formulario.apagarLetra(); break;
                    case Keys.ENTER:
                    case Keys.NUMPAD_ENTER:
                        jogo.iniciarPartida(formulario);
                        break;
                    case Keys.ESCAPE: jogo.irParaMenu(); break;
                    default: break;
                }
                return true;
            }

            @Override
            public boolean keyTyped(char caractere) {
                formulario.digitar(caractere);
                return true;
            }
        };
    }

    @Override
    protected void desenhar() {
        lote().begin();
        escreverTitulo("NOVA MISSÃO", 530, Color.ORANGE, 2f);
        escrever("Nome do piloto:", 150, 430, Color.LIGHT_GRAY, 1.3f);
        String nome = formulario.getNomeDigitado();
        escrever((nome.isEmpty() ? "" : nome) + "_", 340, 430, Color.WHITE, 1.3f);
        if (nome.isBlank()) {
            escrever("(vazio = " + FormularioNovaPartida.PILOTO_PADRAO + ")", 340, 400, Color.GRAY, 1f);
        }
        escrever("Dificuldade:", 150, 340, Color.LIGHT_GRAY, 1.3f);
        escrever("< " + formulario.getDificuldade() + " >", 340, 340, Color.WHITE, 1.3f);
        escrever("Tamanho do mapa:", 150, 270, Color.LIGHT_GRAY, 1.3f);
        int n = formulario.getTamanho();
        escrever(n + "  (de -" + n + " a +" + n + ")", 340, 270, Color.WHITE, 1.3f);
        escreverCentralizado("Digite o nome  |  Setas esquerda/direita: dificuldade  |  Setas cima/baixo: mapa",
                150, Color.GRAY, 1f);
        escreverCentralizado("Enter: decolar   |   Esc: voltar ao menu", 115, Color.SKY, 1.1f);
        lote().end();
    }
}
