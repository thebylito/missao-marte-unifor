package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * O nível da pilotagem: limites, rochas, plataformas (a base e as dos passageiros) e asteroides. Os passageiros são
 * os mesmos do jogo clássico ({@link Astronauta}, {@link Engenheiro}, {@link Professor}): cada um sabe quanto vale
 * (polimorfismo de {@link Passageiro}, desde a Fase 1). A posição de grade deles não se aplica aqui; o lugar de
 * cada um é a plataforma onde espera.
 */
public final class Caverna {

    static final double LARGURA_DA_BASE = 256;
    static final double ALTURA_DA_BASE = 48;
    static final double LARGURA_DA_PLATAFORMA = 160;
    static final double ALTURA_DA_PLATAFORMA = 36;

    private final double largura;
    private final double altura;
    private final List<Rocha> rochas = new ArrayList<>();
    private final List<PlataformaDePouso> plataformas = new ArrayList<>();
    private final List<AsteroideEmOrbita> asteroides = new ArrayList<>();
    private PlataformaDePouso base;

    public Caverna(double largura, double altura) {
        this.largura = largura;
        this.altura = altura;
    }

    public Caverna rocha(double x, double y, double largura, double altura) {
        rochas.add(new Rocha(x, y, largura, altura));
        return this;
    }

    public Caverna base(double x, double superficie) {
        base = new PlataformaDePouso(PlataformaDePouso.Tipo.BASE, x, superficie, LARGURA_DA_BASE, ALTURA_DA_BASE);
        plataformas.add(base);
        return this;
    }

    public Caverna passageiro(Passageiro passageiro, double x, double superficie) {
        PlataformaDePouso p = new PlataformaDePouso(PlataformaDePouso.Tipo.PASSAGEIRO, x, superficie,
                LARGURA_DA_PLATAFORMA, ALTURA_DA_PLATAFORMA);
        p.receber(passageiro);
        plataformas.add(p);
        return this;
    }

    /** Posto pousável sem passageiro; não participa da contagem de resgates. */
    public Caverna instalacao(double x,double superficie) {
        plataformas.add(new PlataformaDePouso(PlataformaDePouso.Tipo.INSTALACAO,x,superficie,256,36));
        return this;
    }

    public Caverna asteroide(AsteroideEmOrbita asteroide) {
        asteroides.add(asteroide);
        return this;
    }

    public double getLargura() { return largura; }
    public double getAltura() { return altura; }
    public List<Rocha> getRochas() { return Collections.unmodifiableList(rochas); }
    public List<PlataformaDePouso> getPlataformas() { return Collections.unmodifiableList(plataformas); }
    public List<AsteroideEmOrbita> getAsteroides() { return Collections.unmodifiableList(asteroides); }
    public PlataformaDePouso getBase() { return base; }

    public int totalDePassageiros() {
        return (int) plataformas.stream().filter(p -> p.getTipo() == PlataformaDePouso.Tipo.PASSAGEIRO).count();
    }

    /** Passageiros ainda esperando nas plataformas. */
    public int esperando() {
        return (int) plataformas.stream().filter(p -> p.getEsperando().isPresent()).count();
    }

    /**
     * A caverna marciana da POC: a base à esquerda; o Astronauta em cima de um bloco de rocha, o Engenheiro numa
     * prateleira alta e o Professor no chão, no fim; três asteroides oscilando. Cada chamada monta uma caverna nova
     * (os passageiros mudam de lugar durante a partida).
     */
    public static Caverna marciana() {
        double chao = 80;
        return new Caverna(3840, 1440)
                // contorno
                .rocha(0, 0, 3840, chao)
                .rocha(0, 1360, 3840, 80)
                .rocha(0, 0, 40, 1440)
                .rocha(3800, 0, 40, 1440)
                // obstáculos
                .rocha(820, chao, 120, 640)
                .rocha(1120, 760, 150, 600)
                .rocha(1300, chao, 520, 400)
                .rocha(1980, 880, 160, 480)
                .rocha(2220, 900, 440, 60)
                .rocha(2800, chao, 140, 660)
                .rocha(3050, 980, 420, 380)
                // a base no começo e os passageiros espalhados (os mesmos nomes do jogo clássico)
                .base(260, chao + ALTURA_DA_BASE)
                .passageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM), 1480, 480 + ALTURA_DA_PLATAFORMA)
                .passageiro(new Engenheiro("Eng. Rosa", Posicao.ORIGEM), 2360, 960 + ALTURA_DA_PLATAFORMA)
                .passageiro(new Professor("Dr. Silva", Posicao.ORIGEM), 3360, chao + ALTURA_DA_PLATAFORMA)
                // asteroides em movimento
                .asteroide(new AsteroideEmOrbita(1040, 520, 36, 0, 200, 6, 0, 40))
                .asteroide(new AsteroideEmOrbita(2480, 560, 40, 220, 0, 7, 1.2, -55))
                .asteroide(new AsteroideEmOrbita(3240, 600, 32, 120, 160, 5, 2.4, 70));
    }
}
