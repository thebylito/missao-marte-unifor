package br.edu.unifor.missaomarte.pilotagem.horda;

import java.util.ArrayList;
import java.util.List;

/**
 * Cavernas descritas para os testes da Horda, e atalhos para copiá-las com uma mudança (os records não têm "with").
 */
final class CavernasDeTeste {

    private CavernasDeTeste() {}

    /** Índices, em {@link #marcianaDescrita()}, dos pontos dos passageiros do {@code Caverna.marciana()}. */
    static final int PONTO_DO_ASTRONAUTA = 0;
    static final int PONTO_DO_ENGENHEIRO = 1;
    static final int PONTO_DO_PROFESSOR = 2;

    /**
     * A geometria do {@code Caverna.marciana()}, como dado: as mesmas rochas na mesma ordem, a base em (260,
     * superfície 128), os três pontos dos passageiros (1480, 516), (2360, 996) e (3360, 116) e os três asteroides com
     * as fases originais. Mais dois pontos candidatos: (2000, 116), no chão entre o bloco do Astronauta e a coluna de
     * rocha das 2800, e (2790, 776), no topo dessa coluna (2800 a 2940, topo em 740), centrado nela. Superfície =
     * topo da rocha de apoio + 36 (altura da plataforma de passageiro), como no {@code marciana()}.
     */
    static DescricaoDeCaverna marcianaDescrita() {
        double chao = 80;
        return new DescricaoDeCaverna("marciana", "Caverna Marciana", 3840, 1440,
                List.of(
                        // contorno
                        new RochaDescrita(0, 0, 3840, chao),
                        new RochaDescrita(0, 1360, 3840, 80),
                        new RochaDescrita(0, 0, 40, 1440),
                        new RochaDescrita(3800, 0, 40, 1440),
                        // obstáculos
                        new RochaDescrita(820, chao, 120, 640),
                        new RochaDescrita(1120, 760, 150, 600),
                        new RochaDescrita(1300, chao, 520, 400),
                        new RochaDescrita(1980, 880, 160, 480),
                        new RochaDescrita(2220, 900, 440, 60),
                        new RochaDescrita(2800, chao, 140, 660),
                        new RochaDescrita(3050, 980, 420, 380)),
                new PontoDePouso(260, chao + 48),
                List.of(
                        new PontoDePouso(1480, 480 + 36),
                        new PontoDePouso(2360, 960 + 36),
                        new PontoDePouso(3360, chao + 36),
                        new PontoDePouso(2000, chao + 36),
                        new PontoDePouso(2790, 740 + 36)),
                List.of(
                        new AsteroideDescrito(1040, 520, 36, 0, 200, 6, 0, 40),
                        new AsteroideDescrito(2480, 560, 40, 220, 0, 7, 1.2, -55),
                        new AsteroideDescrito(3240, 600, 32, 120, 160, 5, 2.4, 70)),
                true);
    }

    /** Uma segunda caverna, plana, com 4 pontos de pouso e um asteroide só (para o sorteio ter onde escolher). */
    static DescricaoDeCaverna planicie() {
        double chao = 50;
        return new DescricaoDeCaverna("planicie", "Planície", 3000, 1500,
                List.of(new RochaDescrita(0, 0, 3000, chao), new RochaDescrita(0, 1450, 3000, 50)),
                new PontoDePouso(100, chao + 48),
                List.of(
                        new PontoDePouso(800, chao + 36),
                        new PontoDePouso(1400, chao + 36),
                        new PontoDePouso(2000, chao + 36),
                        new PontoDePouso(2600, chao + 36)),
                List.of(new AsteroideDescrito(1500, 900, 40, 600, 100, 8, 0.5, 30)),
                true);
    }

    // ------------------------------------------------------------------ cópias com uma mudança

    static DescricaoDeCaverna comId(DescricaoDeCaverna c, String id, String nome) {
        return new DescricaoDeCaverna(id, nome, c.largura(), c.altura(), c.rochas(), c.base(), c.pontosDePouso(),
                c.asteroides(), c.liberada());
    }

    static DescricaoDeCaverna comId(DescricaoDeCaverna c, String id) {
        return comId(c, id, c.nome());
    }

    static DescricaoDeCaverna comTamanho(DescricaoDeCaverna c, double largura, double altura) {
        return new DescricaoDeCaverna(c.id(), c.nome(), largura, altura, c.rochas(), c.base(), c.pontosDePouso(),
                c.asteroides(), c.liberada());
    }

    static DescricaoDeCaverna comRochas(DescricaoDeCaverna c, List<RochaDescrita> rochas) {
        return new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), rochas, c.base(), c.pontosDePouso(),
                c.asteroides(), c.liberada());
    }

    static DescricaoDeCaverna comBase(DescricaoDeCaverna c, PontoDePouso base) {
        return new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), base, c.pontosDePouso(),
                c.asteroides(), c.liberada());
    }

    static DescricaoDeCaverna comPontos(DescricaoDeCaverna c, List<PontoDePouso> pontos) {
        return new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(), pontos,
                c.asteroides(), c.liberada());
    }

    static DescricaoDeCaverna comAsteroides(DescricaoDeCaverna c, List<AsteroideDescrito> asteroides) {
        return new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(),
                c.pontosDePouso(), asteroides, c.liberada());
    }

    static DescricaoDeCaverna liberada(DescricaoDeCaverna c, boolean liberada) {
        return new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(),
                c.pontosDePouso(), c.asteroides(), liberada);
    }

    /** A lista com um item a mais no fim. */
    static <T> List<T> mais(List<T> lista, T item) {
        List<T> nova = new ArrayList<>(lista);
        nova.add(item);
        return nova;
    }

    /** A lista com o item da posição trocado. */
    static <T> List<T> trocar(List<T> lista, int posicao, T item) {
        List<T> nova = new ArrayList<>(lista);
        nova.set(posicao, item);
        return nova;
    }

    /** A lista sem o item da posição. */
    static <T> List<T> sem(List<T> lista, int posicao) {
        List<T> nova = new ArrayList<>(lista);
        nova.remove(posicao);
        return nova;
    }
}
