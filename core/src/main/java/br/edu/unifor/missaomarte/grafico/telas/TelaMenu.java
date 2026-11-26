package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoMenu;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.FolhaDeSprites;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/** Menu principal. O toque em uma opção ou na faixa da Horda usa a mesma ação do teclado. */
public class TelaMenu extends TelaBase {

    private static final String[] OPCOES = {
        "1. Iniciar Nova Missão",
        "2. Visualizar Ranking (local e global)",
        "3. Resetar Histórico de Ranking",
        "4. Sair do Jogo",
        "5. Meu Histórico (online)",
        "6. Trocar de Piloto",
        "7. Hangar",
    };
    private static final float Y_DAS_OPCOES = 385;
    private static final float PASSO = 36;
    private static final Retangulo FAIXA_HORDA = new Retangulo(40, 8, 720, 100);
    private static final Color FUNDO_HORDA = new Color(0.36f, 0.19f, 0.10f, 0.96f);
    private static final Retangulo BOTAO_SIM = new Retangulo(250, 70, 140, 60);
    private static final Retangulo BOTAO_NAO = new Retangulo(410, 70, 140, 60);

    private final ControladorDoMenu controlador;
    private float tempo;

    public TelaMenu(MissaoMarteGame jogo) {
        super(jogo);
        this.controlador = new ControladorDoMenu(jogo.getServicos().ranking(), jogo.getFaixaDaHorda(),
                () -> jogo.getSessao().isPresent());
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int tecla) {
                switch (controlador.teclaPressionada(tecla)) {
                    case NOVA_PARTIDA: jogo.irParaNovaPartida(); break;
                    case RANKING: jogo.irParaRanking(); break;
                    case SAIR: jogo.sair(); break;
                    case HISTORICO: jogo.irParaHistorico(); break;
                    case TROCAR_PILOTO: jogo.irParaEntrada(); break;
                    case HORDA: jogo.iniciarHorda(controlador.hordaAtual(), controlador::falhaAoIniciarHorda); break;
                    case HANGAR: jogo.irParaHangar(); break;
                    default: break;
                }
                return true;
            }
        };
    }

    @Override
    protected List<AlvoDeToque> alvosDeToque() {
        List<AlvoDeToque> alvos = new ArrayList<>();
        if (controlador.isConfirmandoApagar()) {
            alvos.add(tecla(BOTAO_SIM, Keys.S, 's'));
            alvos.add(tecla(BOTAO_NAO, Keys.N, 'n'));
            return alvos;
        }
        alvos.add(tecla(FAIXA_HORDA, Keys.H, 'h'));
        for (int i = 0; i < OPCOES.length; i++) {
            alvos.add(tecla(linha(240, Y_DAS_OPCOES - i * PASSO, 460, PASSO - 2), Keys.NUM_1 + i, (char) ('1' + i)));
        }
        return alvos;
    }

    @Override
    protected void atualizar(float delta) {
        tempo += delta;
        controlador.atualizarHorda();
    }

    @Override
    protected void desenhar() {
        lote().begin();
        if (recursosProntos()) {
            // a nave flutua ao lado das opções e um asteroide gira do outro lado
            desenharNave(0, 150, 320 + 8 * (float) Math.sin(tempo * 1.8), 120);
            desenharCentralizado(Folha.ASTEROIDE, FolhaDeSprites.quadroEmLaco(Folha.ASTEROIDE, tempo, 10), 660, 300, 80);
        }
        escreverTitulo("MISSÃO MARTE UNIFOR", 520, Color.ORANGE, 2.4f);
        escreverCentralizado("Pilote sua nave, salve os passageiros e desvie dos perigos!", 465, Color.LIGHT_GRAY, 1.1f);
        float y = Y_DAS_OPCOES;
        for (String opcao : OPCOES) {
            escrever(opcao, 260, y, Color.WHITE, 1.3f);
            y -= PASSO;
        }
        // Fase 5: quem está jogando (conta no servidor ou convidado)
        String quem = jogo.getSessao().map(s -> "Piloto conectado: " + s.nome() + " (ranking global ativo)")
                .orElse("Jogando como convidado (só o ranking local)");
        escreverCentralizado(quem, 430, jogo.getSessao().isPresent() ? Color.LIME : Color.LIGHT_GRAY, 1f);
        ControladorDoMenu.textoDoSaldo(jogo.getSessao(), jogo.getSaldo())
                .ifPresent(saldo -> escreverCentralizado(saldo, 407, Color.ORANGE, 1f));
        Color corDaMensagem = controlador.isConfirmandoApagar() ? Color.YELLOW : Color.SKY;
        escreverComQuebra(controlador.getMensagem(), 42, 145, 716, corDaMensagem, 1f);
        lote().end();
        if (!controlador.isConfirmandoApagar()) {
            boolean conectado = jogo.getSessao().isPresent();
            if (conectado) desenharBotoes(List.of(FAIXA_HORDA), List.of(""), 1, FUNDO_HORDA);
            else desenharBotoes(List.of(FAIXA_HORDA), List.of(""), 1);
            lote().begin();
            escreverTitulo(conectado ? "▶ Jogar a Horda" : ControladorDoMenu.CONVITE_PARA_ENTRAR,
                    104, conectado ? Color.GOLD : Color.ORANGE, conectado ? 1.6f : 1.25f);
            if (conectado && !toque()) escrever("[H]", 706, 98, Color.GOLD, 1.1f);
            float yDaFaixa = 75;
            for (String linha : controlador.faixaDaHorda()) {
                escreverCentralizado(linha, yDaFaixa, Color.WHITE, 1f);
                yDaFaixa -= 21;
            }
            lote().end();
        } else if (toque()) {
            desenharBotoes(List.of(BOTAO_SIM, BOTAO_NAO), List.of("Sim", "Não"), 1.1f);
        }
    }
}
