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

/**
 * Menu principal: as 4 opções do console e as 2 da Fase 5, escolhidas pelas teclas 1 a 6 ou, na Fase 6, por toque
 * (tocar numa opção aperta a tecla dela; na confirmação de apagar, os botões Sim e Não apertam S e N).
 */
public class TelaMenu extends TelaBase {

    private static final String[] OPCOES = {
        "1. Iniciar Nova Missão",
        "2. Visualizar Ranking (local e global)",
        "3. Resetar Histórico de Ranking",
        "4. Sair do Jogo",
        "5. Meu Histórico (online)",
        "6. Trocar de Piloto",
    };
    private static final float Y_DAS_OPCOES = 385;
    private static final float PASSO = 36;
    private static final Retangulo BOTAO_SIM = new Retangulo(250, 70, 140, 60);
    private static final Retangulo BOTAO_NAO = new Retangulo(410, 70, 140, 60);

    private final ControladorDoMenu controlador;
    private float tempo;

    public TelaMenu(MissaoMarteGame jogo) {
        super(jogo);
        this.controlador = new ControladorDoMenu(jogo.getServicos().ranking());
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
        for (int i = 0; i < OPCOES.length; i++) {
            alvos.add(tecla(linha(240, Y_DAS_OPCOES - i * PASSO, 460, PASSO - 2), Keys.NUM_1 + i, (char) ('1' + i)));
        }
        return alvos;
    }

    @Override
    protected void atualizar(float delta) {
        tempo += delta;
    }

    @Override
    protected void desenhar() {
        lote().begin();
        if (recursosProntos()) {
            // a nave flutua ao lado das opções e um asteroide gira do outro lado
            desenharCentralizado(Folha.NAVE, 0, 150, 320 + 8 * (float) Math.sin(tempo * 1.8), 120);
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
        Color corDaMensagem = controlador.isConfirmandoApagar() ? Color.YELLOW : Color.SKY;
        escreverCentralizado(controlador.getMensagem(), 150, corDaMensagem, 1.2f);
        if (!toque()) {
            escreverCentralizado("Escolha uma opção (teclas 1 a 6)", 60, Color.GRAY, 1f);
        } else if (!controlador.isConfirmandoApagar()) {
            escreverCentralizado("Toque numa opção", 60, Color.GRAY, 1f);
        }
        lote().end();
        if (toque() && controlador.isConfirmandoApagar()) {
            desenharBotoes(List.of(BOTAO_SIM, BOTAO_NAO), List.of("Sim", "Não"), 1.1f);
        }
    }
}
