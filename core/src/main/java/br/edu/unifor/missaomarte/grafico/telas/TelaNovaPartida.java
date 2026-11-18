package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;

/**
 * As perguntas antes da partida. O nome chega por {@code keyTyped} (o caractere já com acento e
 * maiúscula); as setas e o Enter chegam por {@code keyDown} (o código da tecla). Fase 6: no modo toque, botões
 * apertam as mesmas teclas (setas, Enter, Esc) e o nome vem da caixa de texto do celular.
 */
public class TelaNovaPartida extends TelaBase {

    private static final Retangulo CAMPO_NOME = new Retangulo(330, 400, 330, 44);
    private static final Retangulo DIFICULDADE_ANTERIOR = new Retangulo(560, 306, 70, 56);
    private static final Retangulo DIFICULDADE_PROXIMA = new Retangulo(640, 306, 70, 56);
    private static final Retangulo MAPA_MENOR = new Retangulo(560, 236, 70, 56);
    private static final Retangulo MAPA_MAIOR = new Retangulo(640, 236, 70, 56);
    private static final Retangulo DECOLAR = new Retangulo(230, 60, 190, 72);
    private static final Retangulo VOLTAR = new Retangulo(440, 60, 140, 72);

    private final FormularioNovaPartida formulario = new FormularioNovaPartida();

    public TelaNovaPartida(MissaoMarteGame jogo) {
        super(jogo);
        // Fase 5: com uma conta conectada, o nome do piloto já vem preenchido (pode ser trocado)
        jogo.getSessao().ifPresent(s -> s.nome().chars().forEach(c -> formulario.digitar((char) c)));
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
    protected List<AlvoDeToque> alvosDeToque() {
        if (!toque()) {
            return List.of();
        }
        return List.of(
                new AlvoDeToque(CAMPO_NOME, () -> pedirTexto("Nome do piloto", formulario.getNomeDigitado(), false,
                        formulario::definirNome)),
                tecla(DIFICULDADE_ANTERIOR, Keys.LEFT, (char) 0),
                tecla(DIFICULDADE_PROXIMA, Keys.RIGHT, (char) 0),
                tecla(MAPA_MENOR, Keys.DOWN, (char) 0),
                tecla(MAPA_MAIOR, Keys.UP, (char) 0),
                tecla(DECOLAR, Keys.ENTER, (char) 0),
                tecla(VOLTAR, Keys.ESCAPE, (char) 0));
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
        String dificuldade = String.valueOf(formulario.getDificuldade());
        escrever(toque() ? dificuldade : "< " + dificuldade + " >", 340, 340, Color.WHITE, 1.3f);
        escrever("Tamanho do mapa:", 150, 270, Color.LIGHT_GRAY, 1.3f);
        int n = formulario.getTamanho();
        escrever(n + "  (de -" + n + " a +" + n + ")", 340, 270, Color.WHITE, 1.3f);
        if (!toque()) {
            escreverCentralizado("Digite o nome  |  Setas esquerda/direita: dificuldade  |  Setas cima/baixo: mapa",
                    150, Color.GRAY, 1f);
            escreverCentralizado("Enter: decolar   |   Esc: voltar ao menu", 115, Color.SKY, 1.1f);
        } else {
            escreverCentralizado("Toque no nome para digitar", 175, Color.GRAY, 1f);
        }
        lote().end();
        if (toque()) {
            desenharBotoes(List.of(DIFICULDADE_ANTERIOR, DIFICULDADE_PROXIMA, MAPA_MENOR, MAPA_MAIOR, DECOLAR, VOLTAR),
                    List.of("<", ">", "-", "+", "Decolar", "Voltar"), 1.2f);
        }
    }
}
