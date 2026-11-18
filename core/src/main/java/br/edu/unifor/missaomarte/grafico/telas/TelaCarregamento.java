package br.edu.unifor.missaomarte.grafico.telas;

import br.edu.unifor.missaomarte.grafico.Diario;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * Primeira tela: carrega os assets aos poucos ({@code AssetManager.update()} a cada quadro) e mostra o progresso.
 * Assim a janela responde desde o início, mesmo enquanto as texturas sobem para a GPU. Quando termina, vai para a
 * tela de entrada (Fase 5). Usa só formas e a fonte padrão, porque as fontes do jogo ainda não existem.
 */
public class TelaCarregamento extends TelaBase {

    private static final float LARGURA_DA_BARRA = 400;

    public TelaCarregamento(MissaoMarteGame jogo) {
        super(jogo);
    }

    @Override
    protected boolean mostrarFundo() {
        return false;
    }

    @Override
    protected InputAdapter entrada() {
        return new InputAdapter();   // nada a fazer até carregar
    }

    @Override
    protected void atualizar(float delta) {
        if (recursos().atualizar()) {
            Diario.registrar("carregamento concluído: " + recursos().quantidadeCarregada() + " assets no AssetManager");
            jogo.irParaEntrada();
        }
    }

    @Override
    protected void desenhar() {
        float x = (MissaoMarteGame.LARGURA - LARGURA_DA_BARRA) / 2;
        formas().begin(ShapeType.Filled);
        formas().setColor(0.2f, 0.16f, 0.18f, 1);
        formas().rect(x, 280, LARGURA_DA_BARRA, 16);
        formas().setColor(Color.ORANGE);
        formas().rect(x, 280, LARGURA_DA_BARRA * recursos().progresso(), 16);
        formas().end();
        lote().begin();
        escreverCentralizado("Carregando a missão... " + Math.round(recursos().progresso() * 100) + "%", 330,
                Color.LIGHT_GRAY, 1.1f);
        lote().end();
    }
}
