package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoHangar;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.online.SkinDaLoja;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.ArrayList;
import java.util.List;

/** A lista mantém a seleção visível ao navegar e ao mudar a orientação da tela. */
public final class TelaHangar extends TelaBase {
    private static final float ALVO = LayoutDaPartida.ALVO_MINIMO;
    private static final float PASSO = ALVO + 6;
    private static final Color LINHA = new Color(0.12f, 0.10f, 0.19f, 1);
    private static final Color SELECAO = new Color(0.29f, 0.20f, 0.15f, 1);

    private record Linha(Retangulo area, int indice, SkinDaLoja skin) { }

    private final ControladorDoHangar controlador;
    private int selecionada;
    private int primeiraVisivel;

    public TelaHangar(MissaoMarteGame jogo, ControladorDoHangar controlador) {
        super(jogo);
        this.controlador = controlador;
    }

    @Override
    public void show() {
        super.show();
        if (jogo.getSessao().isPresent()) {
            jogo.getNaves().prepararPrevias();
        }
        controlador.carregar();
    }

    @Override
    public void dispose() {
        jogo.getNaves().liberarPrevias();
        super.dispose();
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                switch (tecla) {
                    case Keys.UP, Keys.LEFT -> mover(-1);
                    case Keys.DOWN, Keys.RIGHT -> mover(1);
                    case Keys.ENTER, Keys.NUMPAD_ENTER -> acionar();
                    case Keys.ESCAPE -> jogo.irParaMenu();
                    default -> { return false; }
                }
                return true;
            }
        };
    }

    private void mover(int deslocamento) {
        selecionada = Math.max(0, Math.min(selecionada + deslocamento, controlador.itens().size() - 1));
    }

    private void acionar() {
        if (!controlador.itens().isEmpty()) {
            controlador.acionar(controlador.itens().get(selecionada).id());
        }
    }

    private List<Retangulo> botoes() {
        Retangulo v = visivel();
        float x = v.centroX();
        float y = v.y() + MARGEM;
        return List.of(new Retangulo(x - 345, y, 142, ALVO), new Retangulo(x - 192, y, 122, ALVO),
                new Retangulo(x - 59, y, 122, ALVO), new Retangulo(x + 74, y, 271, ALVO));
    }

    @Override
    protected List<AlvoDeToque> alvosDeToque() {
        List<AlvoDeToque> alvos = new ArrayList<>();
        if (toque()) {
            List<Retangulo> areas = botoes();
            alvos.add(tecla(areas.get(0), Keys.ESCAPE, (char) 0));
            if (!controlador.itens().isEmpty()) {
                alvos.add(tecla(areas.get(1), Keys.UP, (char) 0));
                alvos.add(tecla(areas.get(2), Keys.DOWN, (char) 0));
                alvos.add(tecla(areas.get(3), Keys.ENTER, (char) 0));
            }
        }
        for (Linha linha : linhas()) {
            alvos.add(new AlvoDeToque(linha.area(), () -> selecionada = linha.indice()));
        }
        return alvos;
    }

    private List<Linha> linhas() {
        Retangulo v = visivel();
        float altura = v.altura() - 250;
        int capacidade = Math.max(1, (int) (altura / PASSO));
        List<SkinDaLoja> itens = controlador.itens();
        int limite = Math.max(0, itens.size() - capacidade);
        primeiraVisivel = Math.min(limite, Math.max(selecionada - capacidade + 1,
                Math.min(selecionada, primeiraVisivel)));

        List<Linha> linhas = new ArrayList<>();
        float largura = Math.min(920, v.largura() - 2 * MARGEM);
        float topo = v.topo() - 128;
        for (int i = primeiraVisivel; i < Math.min(itens.size(), primeiraVisivel + capacidade); i++) {
            float y = topo - ALVO - (i - primeiraVisivel) * PASSO;
            linhas.add(new Linha(new Retangulo(v.centroX() - largura / 2, y, largura, ALVO), i, itens.get(i)));
        }
        return linhas;
    }

    @Override
    protected void desenhar() {
        Retangulo v = visivel();
        List<Linha> linhas = linhas();
        formas().begin(ShapeType.Filled);
        for (Linha linha : linhas) {
            Retangulo area = linha.area();
            formas().setColor(linha.indice() == selecionada ? SELECAO : LINHA);
            formas().rect(area.x(), area.y(), area.largura(), area.altura());
        }
        formas().end();

        lote().begin();
        escreverTitulo("HANGAR", v.topo() - 35, Color.ORANGE, 2.2f);
        if (jogo.getSessao().isPresent()) {
            Integer saldo = jogo.getSaldo();
            if (controlador.hangar() != null) {
                saldo = controlador.hangar().saldo();
            }
            escreverCentralizado("Saldo: " + (saldo == null ? "—" : saldo + " moedas"),
                    v.topo() - 85, Color.SKY, 1.2f);
        }
        for (Linha linha : linhas) {
            desenharLinha(linha);
        }
        String mensagem = controlador.mensagem();
        if (mensagem.isEmpty() && controlador.ocupado()) {
            mensagem = "Aguardando o servidor...";
        } else if (mensagem.isEmpty() && linhas.isEmpty()) {
            mensagem = "Nenhuma nave da loja está disponível nesta instalação.";
        }
        escreverCentralizado(mensagem, v.y() + 114, Color.YELLOW, 1.1f);
        if (!toque()) {
            String atalho = linhas.isEmpty() ? "Esc: voltar" : atalhosDaLista();
            escreverCentralizado(atalho, v.y() + 60, Color.SKY, 1f);
        }
        lote().end();

        if (toque()) {
            List<Retangulo> areas = botoes();
            if (linhas.isEmpty()) {
                desenharBotoes(List.of(areas.get(0)), List.of("Voltar"), 1.1f);
            } else {
                desenharBotoes(areas, List.of("Voltar", "Anterior", "Próxima", acaoSelecionada()), 1.1f);
            }
        }
    }

    private void desenharLinha(Linha linha) {
        Retangulo area = linha.area();
        SkinDaLoja skin = linha.skin();
        lote().draw(jogo.getNaves().previa(skin.id()), area.x() + 8, area.y() + 4, 64, 64);
        escreverTitulo(skin.nome(), area.x() + 84, area.topo() - 11, Color.WHITE, 1.2f);
        escrever(controlador.rotuloDaLinha(skin), area.x() + 84, area.y() + 27, Color.LIGHT_GRAY, 1f);
        escrever(skin.preco() + " moedas", area.direita() - 154, area.topo() - 13, Color.ORANGE, 1.1f);
        escrever(rotulo(skin), area.direita() - 154, area.y() + 27, Color.SKY, 1f);
    }

    private String rotulo(SkinDaLoja skin) {
        return switch (controlador.estado(skin.id())) {
            case EQUIPADA -> "Equipada";
            case POSSUIDA -> "Possuída";
            case A_COMPRAR -> "Comprar";
            case INDISPONIVEL -> "Indisponível";
        };
    }

    private String atalhosDaLista() {
        return switch (controlador.estado(controlador.itens().get(selecionada).id())) {
            case A_COMPRAR, POSSUIDA -> "Setas: selecionar    Enter: " + acaoSelecionada() + "    Esc: voltar";
            default -> "Setas: selecionar    Esc: voltar";
        };
    }

    private String acaoSelecionada() {
        SkinDaLoja skin = controlador.itens().get(selecionada);
        return controlador.estado(skin.id()) == ControladorDoHangar.Estado.POSSUIDA ? "Equipar" : rotulo(skin);
    }
}
