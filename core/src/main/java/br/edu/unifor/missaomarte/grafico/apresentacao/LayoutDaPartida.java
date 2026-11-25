package br.edu.unifor.missaomarte.grafico.apresentacao;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Onde fica cada parte da tela de partida, para qualquer tamanho (Fase 6). Java puro: recebe a área visível do mundo
 * (que o ExtendViewport aumenta conforme a proporção da janela ou do celular) e devolve os retângulos do mapa, do
 * painel e, no modo toque, dos botões.
 *
 * <ul>
 *   <li>Paisagem (largura maior que a altura): mapa quadrado à esquerda, painel ao lado e, no toque, os botões à
 *   direita. Numa janela de 800x600 sem toque, o resultado é idêntico ao das Fases 4 e 5.</li>
 *   <li>Retrato: mapa no alto, painel em faixa larga no meio e os botões embaixo (direcional à esquerda, ações à
 *   direita).</li>
 * </ul>
 */
public record LayoutDaPartida(boolean retrato, Retangulo mapa, Retangulo painel, Map<Botao, Retangulo> botoes) {

    static final float MARGEM = 20;
    /** À direita do painel (sem toque) a margem é menor: mantém o mapa de 560 numa janela de 800x600. */
    static final float MARGEM_DIREITA = 12;
    static final float ESPACO = 16;
    /** Largura do painel em paisagem (a mesma das Fases 4 e 5). */
    static final float LARGURA_DO_PAINEL = 192;
    /** Altura do painel em retrato. */
    static final float ALTURA_DO_PAINEL_RETRATO = 250;
    /**
     * Tamanho mínimo de um botão, em unidades do mundo. Num celular comum, 72 unidades dão cerca de 48 dp em paisagem
     * (o tamanho de alvo recomendado pelo Android); em retrato o botão fica maior (o mundo é mais largo).
     */
    public static final float ALVO_MINIMO = 72;
    static final float ALVO_MAXIMO = 120;
    static final float ESPACO_ENTRE_BOTOES = 10;
    /**
     * Folga de toque das setas do direcional: o polegar que cai até 24 unidades fora (um terço do botão mínimo) ainda
     * aperta a seta mais próxima. Embarcar, Som e Abortar não têm folga, para não serem tocados sem querer.
     */
    public static final float FOLGA_DAS_SETAS = 24;
    private static final Set<Botao> SETAS = EnumSet.of(Botao.CIMA, Botao.BAIXO, Botao.ESQUERDA, Botao.DIREITA);

    public LayoutDaPartida {
        botoes = Collections.unmodifiableMap(new EnumMap<>(botoes.isEmpty() ? new EnumMap<>(Botao.class) : botoes));
    }

    public static LayoutDaPartida calcular(Retangulo visivel, boolean toque) {
        return visivel.altura() > visivel.largura() ? retrato(visivel, toque) : paisagem(visivel, toque);
    }

    /**
     * O botão no ponto (x, y): o que contém o ponto ou, fora de todos, a seta mais próxima dentro da folga de toque.
     */
    public Optional<Botao> botaoEm(float x, float y) {
        return botaoEm(botoes, SETAS, FOLGA_DAS_SETAS, x, y);
    }

    /** Toque exato primeiro; senão, entre os botões com folga, o mais próximo que estiver a até {@code folga}. */
    static <B extends Enum<B>> Optional<B> botaoEm(Map<B, Retangulo> botoes, Set<B> comFolga, float folga,
                                                   float x, float y) {
        for (Map.Entry<B, Retangulo> b : botoes.entrySet()) {
            if (b.getValue().contem(x, y)) {
                return Optional.of(b.getKey());
            }
        }
        B maisPerto = null;
        float menor = folga;
        for (Map.Entry<B, Retangulo> b : botoes.entrySet()) {
            float d = b.getValue().distancia(x, y);
            if (comFolga.contains(b.getKey()) && d <= menor) {
                maisPerto = b.getKey();
                menor = d;
            }
        }
        return Optional.ofNullable(maisPerto);
    }

    static float tamanhoDoBotao(Retangulo visivel) {
        return Math.max(ALVO_MINIMO, Math.min(ALVO_MAXIMO, Math.min(visivel.largura(), visivel.altura()) * 0.13f));
    }

    private static LayoutDaPartida paisagem(Retangulo v, boolean toque) {
        float b = tamanhoDoBotao(v);
        float larguraDosControles = toque ? 3 * b + 2 * ESPACO_ENTRE_BOTOES : 0;
        float espacoDosControles = toque ? larguraDosControles + ESPACO : 0;
        float margemDireita = toque ? MARGEM : MARGEM_DIREITA;
        float lado = Math.min(v.altura() - 2 * MARGEM,
                v.largura() - MARGEM - ESPACO - LARGURA_DO_PAINEL - margemDireita - espacoDosControles);
        // centralizado na vertical: sem toque, numa janela de 800x600, fica em y = 20 como nas fases anteriores
        Retangulo mapa = new Retangulo(v.x() + MARGEM, v.centroY() - lado / 2, lado, lado);
        Retangulo painel = new Retangulo(mapa.direita() + ESPACO, v.y() + MARGEM, LARGURA_DO_PAINEL,
                v.altura() - 2 * MARGEM);
        Map<Botao, Retangulo> botoes = new EnumMap<>(Botao.class);
        if (toque) {
            // controles colados na borda direita: direcional embaixo, ações em cima
            float x0 = v.direita() - MARGEM - larguraDosControles;
            direcional(botoes, x0, v.y() + MARGEM, b);
            float yAcoes = v.y() + MARGEM + 3 * b + 2 * ESPACO_ENTRE_BOTOES + ESPACO;
            acoes(botoes, x0, yAcoes, larguraDosControles, b);
        }
        return new LayoutDaPartida(false, mapa, painel, botoes);
    }

    private static LayoutDaPartida retrato(Retangulo v, boolean toque) {
        float b = tamanhoDoBotao(v);
        float lado = v.largura() - 2 * MARGEM;
        float alturaDosControles = toque ? 3 * b + 3 * ESPACO_ENTRE_BOTOES : 0;   // a coluna de ações é a mais alta
        float espacoMinimoAbaixo = ALTURA_DO_PAINEL_RETRATO + ESPACO + (toque ? alturaDosControles + ESPACO : 0);
        lado = Math.min(lado, v.altura() - 2 * MARGEM - espacoMinimoAbaixo);
        Retangulo mapa = new Retangulo(v.centroX() - lado / 2, v.topo() - MARGEM - lado, lado, lado);
        Retangulo painel = new Retangulo(v.x() + MARGEM, mapa.y() - ESPACO - ALTURA_DO_PAINEL_RETRATO,
                v.largura() - 2 * MARGEM, ALTURA_DO_PAINEL_RETRATO);
        Map<Botao, Retangulo> botoes = new EnumMap<>(Botao.class);
        if (toque) {
            // direcional embaixo à esquerda, ações embaixo à direita, encostados na borda de baixo (perto do polegar)
            float yBase = v.y() + MARGEM;
            direcional(botoes, v.x() + MARGEM, yBase, b);
            float largura = 2 * b + ESPACO_ENTRE_BOTOES;
            acoes(botoes, v.direita() - MARGEM - largura, yBase, largura, b);
        }
        return new LayoutDaPartida(true, mapa, painel, botoes);
    }

    /** Direcional em cruz numa grade 3x3 de lado b, com o canto inferior esquerdo em (x, y). */
    private static void direcional(Map<Botao, Retangulo> botoes, float x, float y, float b) {
        float passo = b + ESPACO_ENTRE_BOTOES;
        botoes.put(Botao.CIMA, new Retangulo(x + passo, y + 2 * passo, b, b));
        botoes.put(Botao.ESQUERDA, new Retangulo(x, y + passo, b, b));
        botoes.put(Botao.DIREITA, new Retangulo(x + 2 * passo, y + passo, b, b));
        botoes.put(Botao.BAIXO, new Retangulo(x + passo, y, b, b));
    }

    /**
     * Embarcar, Som e Abortar, empilhados de baixo para cima, todos com a altura mínima de alvo. Abortar fica mais
     * afastado (o dobro do espaço), para não ser tocado sem querer. Altura total: 3b + 3 espaços.
     */
    private static void acoes(Map<Botao, Retangulo> botoes, float x, float y, float largura, float b) {
        Retangulo embarcar = new Retangulo(x, y, largura, b);
        Retangulo som = new Retangulo(x, embarcar.topo() + ESPACO_ENTRE_BOTOES, largura, b);
        Retangulo abortar = new Retangulo(x, som.topo() + 2 * ESPACO_ENTRE_BOTOES, largura, b);
        botoes.put(Botao.EMBARCAR, embarcar);
        botoes.put(Botao.SOM, som);
        botoes.put(Botao.ABORTAR, abortar);
    }
}
