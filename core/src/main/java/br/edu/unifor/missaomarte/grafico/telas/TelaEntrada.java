package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Etapa;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Modo;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de entrada (Fase 5): entrar com uma conta, cadastrar ou jogar como convidado; na Fase 6 também trocar o
 * endereço do servidor e usar tudo por toque (no Android, nome, senha e endereço vêm da caixa de texto do sistema).
 * Só desenha e repassa teclas e toques; quem decide é o {@link ControladorDaEntrada}.
 */
public class TelaEntrada extends TelaBase {

    private static final String[] OPCOES = {"1. Entrar com minha conta", "2. Criar uma conta", "3. Jogar como convidado"};
    private static final float Y_DAS_OPCOES = 325;
    private static final float PASSO = 42;
    private static final Retangulo CAMPO_NOME = new Retangulo(320, 268, 300, 40);
    private static final Retangulo CAMPO_SENHA = new Retangulo(320, 218, 300, 40);
    private static final Retangulo CAMPO_SERVIDOR = new Retangulo(150, 262, 500, 40);
    private static final Retangulo BOTAO_CONFIRMAR = new Retangulo(230, 128, 160, 64);
    private static final Retangulo BOTAO_VOLTAR = new Retangulo(410, 128, 160, 64);

    private final ControladorDaEntrada controlador;
    private float tempo;

    public TelaEntrada(MissaoMarteGame jogo) {
        super(jogo);
        this.controlador = new ControladorDaEntrada(jogo.getServicos().servidor(), jogo.getPlataforma().servidor());
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
    protected List<AlvoDeToque> alvosDeToque() {
        List<AlvoDeToque> alvos = new ArrayList<>();
        switch (controlador.getEtapa()) {
            case ESCOLHA:
                for (int i = 0; i < OPCOES.length; i++) {
                    alvos.add(tecla(linha(240, Y_DAS_OPCOES - i * PASSO, 400, PASSO - 4), Keys.NUM_1 + i,
                            (char) ('1' + i)));
                }
                alvos.add(new AlvoDeToque(linha(150, Y_DAS_OPCOES - 3 * PASSO, 500, PASSO - 4), () -> {
                    // no celular, a caixa de texto do sistema; no desktop, a edição na própria tela (tecla 4)
                    if (!pedirEndereco()) {
                        tecla(CAMPO_SERVIDOR, Keys.NUM_4, '4').acao().run();
                    }
                }));
                break;
            case FORMULARIO:
                alvos.add(new AlvoDeToque(CAMPO_NOME, () -> {
                    if (!pedirTexto("Nome do piloto", controlador.getNome(), false, controlador::definirNome)) {
                        controlador.focar(false);
                    }
                }));
                alvos.add(new AlvoDeToque(CAMPO_SENHA, () -> {
                    if (!pedirTexto("Senha", "", true, controlador::definirSenha)) {
                        controlador.focar(true);
                    }
                }));
                if (toque()) {
                    alvos.add(new AlvoDeToque(BOTAO_CONFIRMAR, controlador::confirmar));
                    alvos.add(tecla(BOTAO_VOLTAR, Keys.ESCAPE, (char) 0));
                }
                break;
            case SERVIDOR:
                alvos.add(new AlvoDeToque(CAMPO_SERVIDOR, this::pedirEndereco));
                if (toque()) {
                    alvos.add(tecla(BOTAO_CONFIRMAR, Keys.ENTER, (char) 0));
                    alvos.add(tecla(BOTAO_VOLTAR, Keys.ESCAPE, (char) 0));
                }
                break;
            default:
                break;
        }
        return alvos;
    }

    /**
     * Caixa de texto do celular para o endereço: abre vazia, com o endereço atual como dica (apagar texto já
     * preenchido no teclado virtual é chato). Confirmar vazio mantém o endereço; no teclado físico (tecla 4), apagar
     * tudo e dar Enter volta ao padrão.
     */
    private boolean pedirEndereco() {
        return pedirTexto("Endereço do servidor", "", controlador.getEnderecoAtual(), false, texto -> {
            if (!texto.isBlank()) {
                controlador.salvarServidor(texto);
            }
        });
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
        switch (controlador.getEtapa()) {
            case ESCOLHA: desenharEscolha(); break;
            case SERVIDOR: desenharServidor(); break;
            default: desenharFormulario(); break;
        }
        Color corDaMensagem = controlador.getEtapa() == Etapa.AGUARDANDO ? Color.SKY : Color.YELLOW;
        escreverCentralizado(controlador.getMensagem(), 110, corDaMensagem, 1f);
        lote().end();
        desenharBotoesDeToque();
    }

    private void desenharEscolha() {
        float y = Y_DAS_OPCOES;
        for (String opcao : OPCOES) {
            escrever(opcao, 260, y, Color.WHITE, 1.3f);
            y -= PASSO;
        }
        escrever("4. Servidor: " + controlador.getEnderecoAtual(), 260, y, Color.LIGHT_GRAY, 1f);
        escreverCentralizado("Convidado: só o ranking local. Com conta: ranking global e histórico.", 158,
                Color.LIGHT_GRAY, 0.95f);
        escreverCentralizado(toque() ? "Toque numa opção" : "Escolha uma opção (teclas 1 a 4)", 60, Color.GRAY, 1f);
    }

    private void desenharFormulario() {
        escreverCentralizado(controlador.getModo() == Modo.ENTRAR ? "Entrar" : "Criar conta", 345, Color.SKY, 1.3f);
        boolean senha = controlador.isEditandoSenha();
        escrever("Nome:", 220, 295, Color.LIGHT_GRAY, 1.2f);
        escrever(controlador.getNome() + (senha ? "" : "_"), 330, 295, senha ? Color.LIGHT_GRAY : Color.WHITE, 1.2f);
        escrever("Senha:", 220, 245, Color.LIGHT_GRAY, 1.2f);
        escrever(controlador.getSenhaMascarada() + (senha ? "_" : ""), 330, 245, senha ? Color.WHITE : Color.LIGHT_GRAY,
                1.2f);
        if (!toque()) {
            escreverCentralizado("Tab: trocar de campo   |   Enter: continuar   |   Esc: voltar", 60, Color.GRAY, 1f);
        } else {
            escreverCentralizado("Toque no campo para digitar", 60, Color.GRAY, 1f);
        }
    }

    private void desenharServidor() {
        escreverCentralizado("Endereço do servidor de ranking", 340, Color.SKY, 1.2f);
        escrever(controlador.getEnderecoEditado() + "_", 160, 290, Color.WHITE, 1.15f);
        escreverCentralizado("Ex.: http://192.168.0.10:8080 (o computador do servidor, na mesma rede)", 245,
                Color.LIGHT_GRAY, 0.9f);
        escreverCentralizado("Vazio: volta ao padrão. Nenhuma senha vai aqui.", 222, Color.LIGHT_GRAY, 0.9f);
        if (!toque()) {
            escreverCentralizado("Enter: salvar   |   Esc: voltar", 60, Color.GRAY, 1f);
        }
    }

    private void desenharBotoesDeToque() {
        if (!toque()) {
            return;
        }
        if (controlador.getEtapa() == Etapa.FORMULARIO) {
            desenharBotoes(List.of(BOTAO_CONFIRMAR, BOTAO_VOLTAR),
                    List.of(controlador.getModo() == Modo.ENTRAR ? "Entrar" : "Criar", "Voltar"), 1.1f);
        } else if (controlador.getEtapa() == Etapa.SERVIDOR) {
            desenharBotoes(List.of(BOTAO_CONFIRMAR, BOTAO_VOLTAR), List.of("Salvar", "Voltar"), 1.1f);
        }
    }
}
