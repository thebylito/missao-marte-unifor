package br.edu.unifor.missaomarte.grafico.apresentacao;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Onde ficam os botões de toque da pilotagem (Fase 7), em qualquer tamanho de tela. Java puro, como o
 * {@link LayoutDaPartida}: recebe a área visível (em unidades da tela, pelo menos 800 x 600) e devolve os retângulos.
 *
 * <ul>
 *   <li>Girar (◄ ►) embaixo à esquerda, para o polegar esquerdo; MOTOR embaixo à direita, maior, para o direito: dá
 *   para girar e acelerar ao mesmo tempo.</li>
 *   <li>Abortar no alto, à direita, embaixo do minimapa: longe dos polegares, para não ser tocado sem querer.</li>
 * </ul>
 * Sem toque (desktop), não há botões: o teclado faz tudo.
 */
public record LayoutDaPilotagem(Retangulo minimapa, Map<Botao, Retangulo> botoes) {

    /** Os botões da pilotagem. */
    public enum Botao { ESQUERDA, DIREITA, MOTOR, ABORTAR }

    static final float MARGEM = 20;
    static final float ESPACO = 14;
    static final float LARGURA_DO_MINIMAPA = 240;
    static final float ALTURA_DO_ABORTAR = LayoutDaPartida.ALVO_MINIMO;
    static final float LARGURA_DO_ABORTAR = 150;
    /** Folga de toque de ◄ e ►, a mesma do direcional ({@link LayoutDaPartida#FOLGA_DAS_SETAS}). */
    public static final float FOLGA_DAS_SETAS = LayoutDaPartida.FOLGA_DAS_SETAS;
    private static final Set<Botao> SETAS = EnumSet.of(Botao.ESQUERDA, Botao.DIREITA);

    public LayoutDaPilotagem {
        botoes = Collections.unmodifiableMap(new EnumMap<>(botoes.isEmpty() ? new EnumMap<>(Botao.class) : botoes));
    }

    /**
     * @param proporcaoDaCaverna altura / largura da caverna (o minimapa tem a mesma proporção)
     */
    public static LayoutDaPilotagem calcular(Retangulo visivel, boolean toque, double proporcaoDaCaverna) {
        Retangulo v = visivel;
        float alturaDoMinimapa = (float) (LARGURA_DO_MINIMAPA * proporcaoDaCaverna);
        Retangulo minimapa = new Retangulo(v.direita() - MARGEM - LARGURA_DO_MINIMAPA,
                v.topo() - MARGEM - alturaDoMinimapa, LARGURA_DO_MINIMAPA, alturaDoMinimapa);
        Map<Botao, Retangulo> botoes = new EnumMap<>(Botao.class);
        if (toque) {
            float b = tamanhoDoBotao(v);
            botoes.put(Botao.ESQUERDA, new Retangulo(v.x() + MARGEM, v.y() + MARGEM, b, b));
            botoes.put(Botao.DIREITA, new Retangulo(v.x() + MARGEM + b + ESPACO, v.y() + MARGEM, b, b));
            float motor = b * 1.3f;
            botoes.put(Botao.MOTOR, new Retangulo(v.direita() - MARGEM - motor, v.y() + MARGEM, motor, motor));
            botoes.put(Botao.ABORTAR, new Retangulo(v.direita() - MARGEM - LARGURA_DO_ABORTAR,
                    minimapa.y() - ESPACO - ALTURA_DO_ABORTAR, LARGURA_DO_ABORTAR, ALTURA_DO_ABORTAR));
        }
        return new LayoutDaPilotagem(minimapa, botoes);
    }

    /** Cerca de 16% do menor lado da tela, entre o alvo mínimo e 140. */
    static float tamanhoDoBotao(Retangulo visivel) {
        return Math.max(LayoutDaPartida.ALVO_MINIMO,
                Math.min(140, Math.min(visivel.largura(), visivel.altura()) * 0.16f));
    }

    /**
     * O botão no ponto: o que contém o ponto ou, fora de todos, a seta (◄ ►) mais próxima dentro da folga de toque.
     * Motor e Abortar não têm folga.
     */
    public Optional<Botao> botaoEm(float x, float y) {
        return LayoutDaPartida.botaoEm(botoes, SETAS, FOLGA_DAS_SETAS, x, y);
    }
}
