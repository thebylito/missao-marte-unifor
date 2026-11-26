package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FaixaDaHorda;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida.ModoDaMissao;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * As perguntas antes da partida. O nome chega por {@code keyTyped} (o caractere já com acento e
 * maiúscula); as setas e o Enter chegam por {@code keyDown} (o código da tecla). Fase 6: no modo toque, botões
 * apertam as mesmas teclas (setas, Enter, Esc) e o nome vem da caixa de texto do celular.
 */
public class TelaNovaPartida extends TelaBase {

    private static final float ALVO = LayoutDaPartida.ALVO_MINIMO;
    private static final Retangulo CAMPO_NOME = new Retangulo(330, 376, 380, ALVO);
    private static final Retangulo DIFICULDADE_ANTERIOR = new Retangulo(560, 298, ALVO, ALVO);
    private static final Retangulo DIFICULDADE_PROXIMA = new Retangulo(640, 298, ALVO, ALVO);
    private static final Retangulo MAPA_MENOR = new Retangulo(560, 220, ALVO, ALVO);
    private static final Retangulo MAPA_MAIOR = new Retangulo(640, 220, ALVO, ALVO);
    private static final Retangulo DECOLAR = new Retangulo(230, 60, 190, ALVO);
    private static final Retangulo VOLTAR = new Retangulo(440, 60, 140, ALVO);
    private static final Retangulo MODO = new Retangulo(560, 450, 150, ALVO);

    private final FormularioNovaPartida formulario = new FormularioNovaPartida();
    private final FaixaDaHorda faixa;
    private boolean aguardandoBilhete;
    private String mensagem = "";

    private record Botao(Retangulo area, int tecla, String rotulo) {}

    public TelaNovaPartida(MissaoMarteGame jogo) {
        super(jogo);
        faixa = jogo.getFaixaDaHorda();
        // Fase 5: com uma conta conectada, o nome do piloto já vem preenchido (pode ser trocado)
        jogo.getSessao().ifPresent(s -> formulario.definirNome(s.nome()));
        atualizarModo();
    }

    public ModoDaMissao getModo() {
        return formulario.getModo();
    }

    @Override
    protected void atualizar(float delta) {
        faixa.atualizar();
        atualizarModo();
    }

    private void atualizarModo() {
        boolean conectado = jogo.getSessao().isPresent();
        formulario.atualizarDisponibilidadeDaHorda(conectado && faixa.disponivel() != null);
        if (!conectado && aguardandoBilhete) {
            aguardandoBilhete = false;
            mensagem = "Sessão encerrada. Entre novamente para jogar a Horda.";
        }
    }

    private void decolar() {
        if (aguardandoBilhete) return;
        if (getModo() != ModoDaMissao.HORDA) {
            jogo.iniciarPartida(formulario);
            return;
        }
        var atual = faixa.disponivel();
        if (atual == null) {
            atualizarModo();
            mensagem = "A Horda está indisponível no momento.";
            return;
        }
        aguardandoBilhete = true;
        mensagem = "Solicitando bilhete da Horda...";
        jogo.iniciarHorda(atual, falha -> {
            aguardandoBilhete = false;
            mensagem = falha;
        });
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                switch (tecla) {
                    case Keys.LEFT:
                        if (getModo() != ModoDaMissao.HORDA) formulario.dificuldadeAnterior();
                        break;
                    case Keys.RIGHT:
                        if (getModo() != ModoDaMissao.HORDA) formulario.proximaDificuldade();
                        break;
                    case Keys.UP:
                        if (getModo() == ModoDaMissao.CLASSICO) formulario.aumentarMapa();
                        break;
                    case Keys.DOWN:
                        if (getModo() == ModoDaMissao.CLASSICO) formulario.diminuirMapa();
                        break;
                    case Keys.BACKSPACE: formulario.apagarLetra(); break;
                    case Keys.TAB:
                        if (!aguardandoBilhete) {
                            atualizarModo();
                            formulario.alternarModo();
                            mensagem = "";
                        }
                        break;
                    case Keys.ENTER:
                    case Keys.NUMPAD_ENTER:
                        decolar();
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
        List<AlvoDeToque> alvos = new ArrayList<>();
        alvos.add(new AlvoDeToque(CAMPO_NOME, () -> pedirTexto("Nome do piloto", formulario.getNomeDigitado(), false,
                formulario::definirNome)));
        for (Botao botao : botoes()) {
            alvos.add(tecla(botao.area(), botao.tecla(), (char) 0));
        }
        return alvos;
    }

    private List<Botao> botoes() {
        List<Botao> botoes = new ArrayList<>();
        botoes.add(new Botao(MODO, Keys.TAB, formulario.proximoModo().getNome()));
        botoes.add(new Botao(DECOLAR, Keys.ENTER, "Decolar"));
        botoes.add(new Botao(VOLTAR, Keys.ESCAPE, "Voltar"));
        if (getModo() != ModoDaMissao.HORDA) {
            botoes.add(new Botao(DIFICULDADE_ANTERIOR, Keys.LEFT, "<"));
            botoes.add(new Botao(DIFICULDADE_PROXIMA, Keys.RIGHT, ">"));
        }
        if (getModo() == ModoDaMissao.CLASSICO) {
            botoes.add(new Botao(MAPA_MENOR, Keys.DOWN, "-"));
            botoes.add(new Botao(MAPA_MAIOR, Keys.UP, "+"));
        }
        return botoes;
    }

    List<String> resumoDaHorda() {
        return getModo() == ModoDaMissao.HORDA ? faixa.linhas() : List.of();
    }

    @Override
    protected void desenhar() {
        lote().begin();
        escreverTitulo("NOVA MISSÃO", 560, Color.ORANGE, 2f);
        boolean horda = getModo() == ModoDaMissao.HORDA;
        boolean pilotagem = getModo() == ModoDaMissao.PILOTAGEM;
        escrever("Modo:", 150, 480, Color.LIGHT_GRAY, 1.3f);
        escrever(getModo().getNome(), 340, 480, Color.ORANGE, 1.3f);
        if (!toque()) {   // o botão do toque já diz para onde troca; no teclado, a dica
            escrever("(Tab: trocar para " + formulario.proximoModo().getNome() + ")", 470, 478, Color.SKY, 1f);
        }
        escrever("Nome do piloto:", 150, 430, Color.LIGHT_GRAY, 1.3f);
        String nome = formulario.getNomeDigitado();
        escrever((nome.isEmpty() ? "" : nome) + "_", 340, 430, Color.WHITE, 1.3f);
        if (nome.isBlank()) {
            escrever("(vazio = " + FormularioNovaPartida.PILOTO_PADRAO + ")", 340, 400, Color.GRAY, 1f);
        }
        if (horda) {
            float y = 350;
            for (String linha : resumoDaHorda()) {
                y -= escreverComQuebra(linha, 150, y, 560, Color.WHITE, 1.2f) + 16;
            }
            escreverCentralizado(toque() ? "Decolar inicia a Horda" : "Enter inicia a Horda  |  Esc: voltar ao menu",
                    194, Color.SKY, 1.1f);
        } else {
            escrever("Dificuldade:", 150, 340, Color.LIGHT_GRAY, 1.3f);
            String dificuldade = String.valueOf(formulario.getDificuldade());
            escrever(toque() ? dificuldade : "< " + dificuldade + " >", 340, 340, Color.WHITE, 1.3f);
            if (pilotagem) {
                escrever("Nível:", 150, 270, Color.LIGHT_GRAY, 1.3f);
                escrever("Caverna marciana (3 passageiros)", 340, 270, Color.WHITE, 1.3f);
                escrever("Gire, acelere e pouse devagar e reto nas plataformas.", 150, 230, Color.GRAY, 1f);
            } else {
                escrever("Tamanho do mapa:", 150, 270, Color.LIGHT_GRAY, 1.3f);
                int n = formulario.getTamanho();
                escrever(n + "  (de -" + n + " a +" + n + ")", 340, 270, Color.WHITE, 1.3f);
            }
            if (!toque()) {
                escreverCentralizado(pilotagem ? "Digite o nome  |  Setas esquerda/direita: dificuldade  |  Tab: modo"
                        : "Digite o nome  |  Setas esquerda/direita: dificuldade  |  Setas cima/baixo: mapa  |  Tab: modo",
                        150, Color.GRAY, 1f);
                escreverCentralizado("Enter: decolar   |   Esc: voltar ao menu", 115, Color.SKY, 1.1f);
            } else {
                escreverCentralizado("Toque no nome para digitar", 194, Color.GRAY, 1f);
            }
        }
        if (!mensagem.isEmpty()) {
            escreverComQuebra(mensagem, 100, 177, 600, aguardandoBilhete ? Color.SKY : Color.ORANGE, 1f);
        }
        lote().end();
        if (toque()) {
            List<Botao> botoes = botoes();
            desenharBotoes(botoes.stream().map(Botao::area).collect(Collectors.toList()),
                    botoes.stream().map(Botao::rotulo).collect(Collectors.toList()), 1.2f);
        }
    }
}
