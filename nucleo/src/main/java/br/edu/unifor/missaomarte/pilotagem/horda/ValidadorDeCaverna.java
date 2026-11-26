package br.edu.unifor.missaomarte.pilotagem.horda;

import java.util.ArrayList;
import java.util.List;

/**
 * Confere se uma caverna descrita pode entrar no rodízio da Horda (CA-05). Devolve a lista de problemas, cada um em
 * português e citando o item pelo nome da lista no JSON ({@code rochas[3]}, {@code pontosDePouso[1]},
 * {@code asteroides[0]}, {@code base}); a lista vazia quer dizer caverna válida. Encostar (bordas iguais) nunca é
 * problema: só a sobreposição com área conta, como no {@code Caverna.marciana()}, em que as plataformas se apoiam no
 * topo das rochas.
 *
 * <p>Regras:
 * <ol>
 *   <li>id e nome não vazios; largura e altura maiores que zero; uma base; pelo menos 3 pontos de pouso;</li>
 *   <li>cada rocha com largura e altura maiores que zero e dentro dos limites;</li>
 *   <li>o corpo de cada plataforma (base e pontos) dentro dos limites, sem invadir rocha nem outra plataforma;</li>
 *   <li>acima de cada plataforma, uma folga de {@value #FOLGA_ACIMA_DA_PLATAFORMA} (largura da plataforma, a partir
 *       da superfície) dentro dos limites e sem rocha nem o corpo de outra plataforma: a nave precisa descer e pousar.
 *       Com os pés 40 abaixo do centro e o casco de raio 30, a nave pousada vai até 70 acima da superfície; 120 dá
 *       50 de margem para a descida;</li>
 *   <li>cada asteroide com período maior que zero, e a caixa que a órbita varre (centro ± |amplitude| ± raio, nos
 *       dois eixos) dentro dos limites, sem passar pelo corpo nem pela folga de nenhuma plataforma.</li>
 * </ol>
 *
 * <p>A geometria do {@code Caverna.marciana()} (jogável e vencida por humanos) passa em todas, sem afrouxar nenhuma:
 * o asteroide mais perto de uma plataforma é o do Professor, cuja órbita desce até 408, bem acima da folga dele (116
 * a 236). Duas regras vão além do mínimo pedido e também passam no marciana: a folga precisa estar dentro dos limites
 * e livre do corpo de outra plataforma (e não só de rocha), e a rocha precisa de tamanho positivo.
 */
public final class ValidadorDeCaverna {

    /** Os tamanhos das plataformas são os da {@code Caverna} (conferidos em teste). */
    public static final double LARGURA_DA_BASE = 256;
    public static final double ALTURA_DA_BASE = 48;
    public static final double LARGURA_DA_PLATAFORMA = 160;
    public static final double ALTURA_DA_PLATAFORMA = 36;
    /** Altura livre exigida acima da superfície de cada plataforma. */
    public static final double FOLGA_ACIMA_DA_PLATAFORMA = 120;

    private ValidadorDeCaverna() {}

    public static List<String> problemas(DescricaoDeCaverna c) {
        List<String> problemas = new ArrayList<>();
        conferirCabecalho(c, problemas);

        Caixa limites = new Caixa(0, 0, c.largura(), c.altura());
        List<Caixa> rochas = new ArrayList<>();
        for (int i = 0; i < c.rochas().size(); i++) {
            RochaDescrita r = c.rochas().get(i);
            Caixa rocha = new Caixa(r.x(), r.y(), r.largura(), r.altura());
            rochas.add(rocha);
            if (!(r.largura() > 0 && r.altura() > 0)) {
                problemas.add("rochas[" + i + "] precisa de largura e altura maiores que zero");
            } else if (!limites.contem(rocha)) {
                problemas.add("rochas[" + i + "] sai dos limites da caverna");
            }
        }

        List<Plataforma> plataformas = plataformas(c);
        conferirCorpos(plataformas, limites, rochas, problemas);
        conferirFolgas(plataformas, limites, rochas, problemas);
        conferirAsteroides(c.asteroides(), plataformas, limites, problemas);
        return List.copyOf(problemas);
    }

    private static void conferirCabecalho(DescricaoDeCaverna c, List<String> problemas) {
        if (vazio(c.id())) {
            problemas.add("o id da caverna está vazio");
        }
        if (vazio(c.nome())) {
            problemas.add("o nome da caverna está vazio");
        }
        if (!(c.largura() > 0)) {
            problemas.add("a largura da caverna precisa ser maior que zero (veio " + numero(c.largura()) + ")");
        }
        if (!(c.altura() > 0)) {
            problemas.add("a altura da caverna precisa ser maior que zero (veio " + numero(c.altura()) + ")");
        }
        if (c.base() == null) {
            problemas.add("a caverna não tem base");
        }
        if (c.pontosDePouso().size() < 3) {
            problemas.add("a caverna precisa de pelo menos 3 pontos de pouso (tem " + c.pontosDePouso().size() + ")");
        }
    }

    /** A base (se houver) primeiro, depois os pontos de pouso, na ordem da descrição. */
    private static List<Plataforma> plataformas(DescricaoDeCaverna c) {
        List<Plataforma> plataformas = new ArrayList<>();
        if (c.base() != null) {
            plataformas.add(new Plataforma("base", "a base", "da base", "na base", c.base(), LARGURA_DA_BASE,
                    ALTURA_DA_BASE));
        }
        for (int i = 0; i < c.pontosDePouso().size(); i++) {
            String nome = "pontosDePouso[" + i + "]";
            plataformas.add(new Plataforma(nome, nome, "de " + nome, "em " + nome, c.pontosDePouso().get(i),
                    LARGURA_DA_PLATAFORMA, ALTURA_DA_PLATAFORMA));
        }
        return plataformas;
    }

    private static void conferirCorpos(List<Plataforma> plataformas, Caixa limites, List<Caixa> rochas,
                                       List<String> problemas) {
        for (int i = 0; i < plataformas.size(); i++) {
            Plataforma p = plataformas.get(i);
            if (!limites.contem(p.corpo)) {
                problemas.add(p.nome + ": a plataforma sai dos limites da caverna");
            }
            for (int r = 0; r < rochas.size(); r++) {
                if (p.corpo.sobrepoe(rochas.get(r))) {
                    problemas.add(p.nome + ": a plataforma invade rochas[" + r + "]");
                }
            }
            for (int j = 0; j < i; j++) {
                Plataforma anterior = plataformas.get(j);
                if (p.corpo.sobrepoe(anterior.corpo)) {
                    problemas.add(p.nome + ": a plataforma invade " + anterior.comArtigo);
                }
            }
        }
    }

    private static void conferirFolgas(List<Plataforma> plataformas, Caixa limites, List<Caixa> rochas,
                                       List<String> problemas) {
        String folga = ": a folga de " + numero(FOLGA_ACIMA_DA_PLATAFORMA) + " acima da plataforma ";
        for (Plataforma p : plataformas) {
            if (!limites.contem(p.folga)) {
                problemas.add(p.nome + folga + "sai dos limites da caverna");
            }
            for (int r = 0; r < rochas.size(); r++) {
                if (p.folga.sobrepoe(rochas.get(r))) {
                    problemas.add(p.nome + folga + "esbarra em rochas[" + r + "]");
                }
            }
            for (Plataforma outra : plataformas) {
                if (outra != p && p.folga.sobrepoe(outra.corpo)) {
                    problemas.add(p.nome + folga + "esbarra " + outra.comEm);
                }
            }
        }
    }

    private static void conferirAsteroides(List<AsteroideDescrito> asteroides, List<Plataforma> plataformas,
                                           Caixa limites, List<String> problemas) {
        for (int i = 0; i < asteroides.size(); i++) {
            AsteroideDescrito a = asteroides.get(i);
            String nome = "asteroides[" + i + "]";
            if (!(a.periodo() > 0)) {
                problemas.add(nome + ": o período precisa ser maior que zero (veio " + numero(a.periodo()) + ")");
            }
            double meiaLargura = Math.abs(a.amplitudeX()) + a.raio();
            double meiaAltura = Math.abs(a.amplitudeY()) + a.raio();
            Caixa orbita = new Caixa(a.centroX() - meiaLargura, a.centroY() - meiaAltura, 2 * meiaLargura,
                    2 * meiaAltura);
            if (!limites.contem(orbita)) {
                problemas.add(nome + ": a órbita sai dos limites da caverna");
            }
            for (Plataforma p : plataformas) {
                if (orbita.sobrepoe(p.corpo)) {
                    problemas.add(nome + ": a órbita passa pela plataforma " + p.comDe);
                }
                if (orbita.sobrepoe(p.folga)) {
                    problemas.add(nome + ": a órbita passa pela folga " + p.comDe);
                }
            }
        }
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** O número como o autor da caverna escreveu: 120 em vez de 120.0. */
    private static String numero(double valor) {
        if (valor == Math.rint(valor) && Math.abs(valor) < 1e15) {
            return Long.toString((long) valor);
        }
        return Double.toString(valor);
    }

    /** Uma plataforma da descrição, com o nome usado nas mensagens, o corpo sólido e a folga acima dele. */
    private static final class Plataforma {
        final String nome;
        final String comArtigo;
        final String comDe;
        final String comEm;
        final Caixa corpo;
        final Caixa folga;

        Plataforma(String nome, String comArtigo, String comDe, String comEm, PontoDePouso ponto, double largura,
                   double altura) {
            this.nome = nome;
            this.comArtigo = comArtigo;
            this.comDe = comDe;
            this.comEm = comEm;
            this.corpo = new Caixa(ponto.x(), ponto.superficie() - altura, largura, altura);
            this.folga = new Caixa(ponto.x(), ponto.superficie(), largura, FOLGA_ACIMA_DA_PLATAFORMA);
        }
    }

    /** Retângulo com o canto inferior esquerdo em (x, y), y para cima. */
    private record Caixa(double x, double y, double largura, double altura) {

        double direita() { return x + largura; }
        double topo() { return y + altura; }

        /** A outra caixa está inteira dentro desta (bordas incluídas). */
        boolean contem(Caixa outra) {
            return outra.x >= x && outra.y >= y && outra.direita() <= direita() && outra.topo() <= topo();
        }

        /** As duas caixas têm área em comum (encostar não conta). */
        boolean sobrepoe(Caixa outra) {
            return x < outra.direita() && outra.x < direita() && y < outra.topo() && outra.y < topo();
        }
    }
}
