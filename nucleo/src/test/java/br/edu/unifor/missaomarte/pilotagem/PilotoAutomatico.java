package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import java.util.ArrayList;
import java.util.List;

/**
 * Piloto automático da caverna marciana, só para a validação (Fase 7): prova, sem mãos e de forma determinística,
 * que a caverna pode ser vencida com as regras e os números de cada dificuldade. Não faz parte do jogo.
 *
 * <p>Como funciona: a rota é uma lista de trechos (pontos de passagem e a plataforma de destino). A cada passo:
 * <ol>
 *   <li>calcula a velocidade desejada na direção do próximo ponto (freando a tempo nos pontos de parada);</li>
 *   <li>deduz a aceleração necessária para chegar a essa velocidade e soma a gravidade: é o empuxo desejado;</li>
 *   <li>gira a nave para o empuxo apontar para lá;</li>
 *   <li>liga o motor numa fração dos passos igual à fração do empuxo que é preciso (modulação sigma-delta: o motor
 *   só liga ou desliga, mas a média sai certa).</li>
 * </ol>
 *
 * <p>A passagem estreita fica entre o pilar (x 820-940, topo em 720) e a estalactite (x 1120-1270, base em 760), e é
 * justamente onde um asteroide sobe e desce (x 1004-1076, centro de 320 a 720). Por cima do pilar, a nave precisa do
 * centro acima de ~770; por baixo da estalactite, abaixo de 730. Com o casco de raio 30, o centro acima de 786 é
 * seguro em qualquer fase do asteroide, e a troca de altura, uma diagonal curta, só corre perigo com ele perto do
 * topo. Então a nave espera num ponto seguro (em cima do vão na ida; embaixo da estalactite, à direita do asteroide,
 * na volta) e atravessa logo depois que ele começa a descer.
 *
 * <p>A rota faz três viagens (base, um passageiro, base), reabastecendo entre elas: assim sobra combustível até no
 * Difícil. A regra não exige um mínimo de viagens; a capacidade 2 só proíbe trazer os três de uma vez.
 *
 * <p>Usado pelo teste {@code PilotoAutomaticoTeste} e pelo roteiro {@code pilotagem} do {@code PilotoDaJanela}.
 */
public final class PilotoAutomatico {

    /**
     * Um ponto da rota.
     *
     * @param raio         a que distância o ponto conta como alcançado (pequeno nas passagens apertadas)
     * @param parar        só conta como alcançado com a nave quase parada (e a nave freia para ele)
     * @param inclinacao   inclinação máxima perto do ponto (ou em todo o caminho até ele, nos verticais): inclinada,
     *                     a nave abre os pés para o lado
     * @param precisao     velocidade máxima para contar como parada
     * @param asteroide    se maior que 0, a nave fica parada aqui até o asteroide do vão estar descendo e entre
     *                     esta altura e 120 abaixo dela (o começo da descida: se ele já estiver lá embaixo, logo
     *                     volta a subir, então a nave espera o próximo ciclo)
     */
    record Ponto(double x, double y, double velocidade, double raio, boolean parar, double inclinacao,
                 double asteroide, double precisao, boolean inclinacaoNoCaminho) {
    }

    /** Um trecho: pontos de passagem e a plataforma onde pousar no fim. */
    record Trecho(List<Ponto> pontos, int plataforma) {
    }

    /** Índices das plataformas na caverna marciana (ordem de criação). */
    static final int BASE = 0;
    static final int ASTRONAUTA = 1;
    static final int ENGENHEIRO = 2;
    static final int PROFESSOR = 3;

    private static final double CRUZEIRO = 300;
    private static final double CUIDADO = 145;
    /** Maior velocidade vertical pedida: subindo ou descendo rápido, a nave passa do ponto por inércia. */
    private static final double VERTICAL_MAXIMA = 140;
    /** Frenagem vertical planejada: para cima, só a gravidade freia (55); para baixo, o motor (145 - 55). */
    private static final double FRENAGEM_VERTICAL = 45;
    /** Desaceleração planejada para frear até um ponto (o motor dá mais do que isso, com folga). */
    private static final double FRENAGEM = 70;
    /** Passos de motor ligado direto na decolagem (0,3 s), para a nave sair do chão. */
    private static final int PASSOS_DE_DECOLAGEM = 36;

    private final List<Trecho> rota;
    private int trecho;
    private int ponto;
    private double modulacao;
    private int decolando;
    private boolean explodiu;
    private double asteroideAntes = Double.NaN;
    private boolean descendo;

    public PilotoAutomatico() {
        this.rota = rotaDaCavernaMarciana();
    }

    /** A rota: três viagens, cada uma da base a um passageiro e de volta. */
    static List<Trecho> rotaDaCavernaMarciana() {
        List<Ponto> idaPeloVao = List.of(
                passagem(388, 330, CUIDADO), passagem(640, 810, CRUZEIRO), passagem(880, 805, CUIDADO),
                parada(1035, 800), espera(1035, 800, 700),
                apertado(1080, 722, 20), apertado(1190, 690, 30), passagem(1330, 690, CUIDADO));
        List<Ponto> voltaPeloVao = List.of(
                parada(1190, 690), espera(1190, 690, 700),
                apertado(1080, 722, 20), apertado(1030, 800, 30),
                passagem(640, 790, CRUZEIRO), parada(388, 340));
        // o corredor comprido a 740: embaixo da estalactite (base em 880) e da prateleira (900), em cima do asteroide
        // que anda de lado (topo em 600)
        List<Ponto> idaAoFundo = List.of(passagem(1700, 740, CRUZEIRO), passagem(2700, 740, CRUZEIRO));
        List<Ponto> voltaDoFundo = List.of(passagem(2700, 740, CRUZEIRO), passagem(1700, 740, CRUZEIRO),
                passagem(1330, 690, CUIDADO));
        List<Trecho> rota = new ArrayList<>();
        // o último ponto de cada viagem é uma parada em cima da plataforma: o pouso começa alinhado e devagar
        rota.add(new Trecho(juntar(idaPeloVao, List.of(parada(1560, 650))), ASTRONAUTA));
        // do Astronauta, sobe e para antes de ir para a esquerda: embaixo da estalactite, só abaixo de 730
        rota.add(new Trecho(juntar(List.of(parada(1560, 680), passagem(1330, 690, CUIDADO)), voltaPeloVao), BASE));
        // o Engenheiro fica na prateleira (x 2220-2660, topo em 960): sobe-se pela direita dela, onde o espaço é
        // largo (o vão de 80 do lado esquerdo, junto da estalactite, obriga a ir devagar e sai mais caro)
        rota.add(new Trecho(juntar(idaPeloVao, idaAoFundo, List.of(passagem(2735, 820, CUIDADO),
                passagem(2735, 1110, CUIDADO), parada(2440, 1110))), ENGENHEIRO));
        rota.add(new Trecho(juntar(List.of(parada(2440, 1110), passagem(2735, 1110, CUIDADO),
                passagem(2735, 820, CUIDADO)), voltaDoFundo, voltaPeloVao), BASE));
        // o Professor fica no chão, depois do pilar (topo em 740) e embaixo do bloco do teto (base em 980), com um
        // asteroide rodando ali perto (até 792): o corredor é a 860, e a subida da plataforma termina parada
        rota.add(new Trecho(juntar(idaPeloVao, idaAoFundo, List.of(passagem(2735, 760, CUIDADO),
                passagem(2735, 870, CUIDADO), passagem(2985, 860, CUIDADO), passagem(3440, 860, CUIDADO),
                parada(3440, 300))), PROFESSOR));
        rota.add(new Trecho(juntar(List.of(parada(3440, 850), passagem(2985, 860, CUIDADO),
                passagem(2735, 870, CUIDADO), passagem(2735, 760, CUIDADO)), voltaDoFundo, voltaPeloVao), BASE));
        return rota;
    }

    /** De passagem: troca de ponto ainda longe dele, sem frear. */
    private static Ponto passagem(double x, double y, double velocidade) {
        return new Ponto(x, y, velocidade, 70, false, 0, 0, 0, false);
    }

    /** Numa passagem apertada: precisa chegar perto, com pouca inclinação, mas sem parar. */
    private static Ponto apertado(double x, double y, double raio) {
        return new Ponto(x, y, 110, raio, false, 15, 0, 0, false);
    }

    /** Para e fica: a inclinação não é limitada, porque é inclinando que a nave freia. */
    private static Ponto parada(double x, double y) {
        return new Ponto(x, y, CUIDADO, 20, true, 0, 0, 35, false);
    }


    private static Ponto espera(double x, double y, double asteroideAbaixoDe) {
        return new Ponto(x, y, CUIDADO, 20, true, 0, asteroideAbaixoDe, 35, false);
    }

    @SafeVarargs
    private static List<Ponto> juntar(List<Ponto>... partes) {
        List<Ponto> todos = new ArrayList<>();
        for (List<Ponto> parte : partes) {
            todos.addAll(parte);
        }
        return todos;
    }

    /** Joga até o fim (ou até o limite de tempo simulado) e devolve todos os eventos. */
    public static List<EventoDeVoo> jogar(PartidaDePilotagem partida, double segundosMaximos) {
        PilotoAutomatico piloto = new PilotoAutomatico();
        List<EventoDeVoo> eventos = new ArrayList<>();
        while (partida.getEstado() == Partida.Estado.EM_ANDAMENTO && partida.getTempo() < segundosMaximos) {
            partida.avancarUmPasso(piloto.decidir(partida));
            eventos.addAll(partida.consumirEventos());
        }
        return eventos;
    }

    /** Os comandos deste passo. */
    public Comandos decidir(PartidaDePilotagem partida) {
        NaveEmVoo nave = partida.getNave();
        observarAsteroide(partida);
        if (nave.getEstado() == NaveEmVoo.Estado.DESTRUIDA) {
            explodiu = true;
            return Comandos.NENHUM;
        }
        if (trecho >= rota.size()) {
            return Comandos.NENHUM;
        }
        if (nave.getEstado() == NaveEmVoo.Estado.POUSADA) {
            return noChao(partida, nave);
        }
        return voar(partida, nave);
    }

    private Comandos noChao(PartidaDePilotagem partida, NaveEmVoo nave) {
        PlataformaDePouso aqui = nave.getPlataforma();
        if (explodiu) {   // nave nova na base: quem estava a bordo voltou à plataforma, então refaz a ida
            explodiu = false;
            ponto = 0;
            if (rota.get(trecho).plataforma() == BASE) {
                trecho--;
            }
        }
        PlataformaDePouso destino = partida.getCaverna().getPlataformas().get(rota.get(trecho).plataforma());
        if (aqui == destino && ponto > 0) {     // chegou ao fim do trecho
            trecho++;
            ponto = 0;
            modulacao = 0;
            if (trecho >= rota.size()) {
                return Comandos.NENHUM;
            }
        }
        if (aqui.getTipo() == PlataformaDePouso.Tipo.PASSAGEIRO && aqui.getEsperando().isPresent()
                && nave.getABordo().size() < nave.getCapacidade()) {
            return Comandos.NENHUM;    // esperando o embarque
        }
        if (aqui.getTipo() == PlataformaDePouso.Tipo.BASE
                && nave.getCombustivel() < partida.getConfig().tanque() - 0.01) {
            return Comandos.NENHUM;    // reabastecendo
        }
        if (ponto == 0) {
            ponto = 1;   // decola rumo ao primeiro ponto (o ponto 0 é o "ainda no chão")
        }
        decolando = PASSOS_DE_DECOLAGEM;
        return Comandos.MOTOR;
    }

    private Comandos voar(PartidaDePilotagem partida, NaveEmVoo nave) {
        if (decolando > 0) {
            decolando--;
            return Comandos.MOTOR;
        }
        Trecho atual = rota.get(trecho);
        int indice = ponto - 1;
        if (indice >= atual.pontos().size()) {
            PlataformaDePouso destino = partida.getCaverna().getPlataformas().get(atual.plataforma());
            return pousar(partida, nave, destino);
        }
        Ponto alvo = atual.pontos().get(indice);
        double distancia = Math.hypot(alvo.x() - nave.getX(), alvo.y() - nave.getY());
        double rapidez = Math.hypot(nave.getVelocidadeX(), nave.getVelocidadeY());
        boolean ultimo = indice == atual.pontos().size() - 1;
        boolean frear = ultimo || alvo.parar();
        boolean liberado = alvo.asteroide() <= 0 || passagemLivre(partida, alvo.asteroide());
        boolean chegou = distancia < alvo.raio() && (!alvo.parar() || rapidez < alvo.precisao());
        if (chegou && liberado) {
            ponto++;
        }
        // a inclinação só é limitada perto do ponto (ou no caminho todo, rente às paredes)
        double inclinacao = alvo.inclinacao() > 0 && (distancia < 80 || alvo.inclinacaoNoCaminho())
                ? alvo.inclinacao() : 0;
        // freia a tempo e, bem perto, devagar na proporção da distância (sem passar do ponto). Com pouca inclinação,
        // a nave freia pouco (cerca de g x tg da inclinação): o perfil leva isso em conta
        double frenagem = inclinacao > 0
                ? Math.min(FRENAGEM, 0.8 * partida.getConfig().gravidade() * Math.tan(Math.toRadians(inclinacao)))
                : FRENAGEM;
        double freio = Math.min(Math.sqrt(2 * frenagem * distancia), 1.6 * distancia);
        double velocidade = Math.min(alvo.velocidade(), frear ? freio : Math.max(freio, 90));
        return pilotar(partida, nave, alvo.x(), alvo.y(), velocidade, 50, inclinacao);
    }

    /**
     * Pouso num movimento só: freia na horizontal até o centro da plataforma (sem passar dele) e, enquanto estiver
     * longe do centro, desce só até 50 de altura (as plataformas ficam perto de paredes); perto do centro, desce,
     * cada vez mais devagar, e toca o chão bem abaixo do limite de queda.
     */
    private Comandos pousar(PartidaDePilotagem partida, NaveEmVoo nave, PlataformaDePouso destino) {
        double alvoX = destino.getCentroX();
        double alvoY = destino.getSuperficie() - partida.getConfig().peY();
        double dx = alvoX - nave.getX();
        double altura = nave.getY() - alvoY;
        double horizontal = Math.min(80, Math.min(Math.sqrt(2 * 40 * Math.abs(dx)), 1.5 * Math.abs(dx)));
        double vxDesejada = Math.signum(dx) * horizontal;
        double vyDesejada;
        double inclinacao;
        if (Math.abs(dx) > 40) {
            double sobra = alvoY + 50 - nave.getY();
            vyDesejada = limitar(sobra * 1.5, Math.min(70, Math.sqrt(2 * FRENAGEM_VERTICAL * Math.abs(sobra)) + 5));
            inclinacao = 45;
        } else {
            boolean derrapando = Math.abs(nave.getVelocidadeX()) > 20;
            // alto, desce rápido; perto do chão, cada vez mais devagar (toca o chão a uns 25-35)
            vyDesejada = derrapando ? -10 : -Math.max(25, Math.min(110, Math.sqrt(2 * 40 * Math.max(0, altura))));
            inclinacao = altura < 40 ? 6 : 20;
        }
        return conduzir(partida, nave, vxDesejada, vyDesejada, inclinacao);
    }

    private Comandos pilotar(PartidaDePilotagem partida, NaveEmVoo nave, double alvoX, double alvoY,
                             double velocidade, double inclinacaoNormal, double inclinacaoPerto) {
        double dx = alvoX - nave.getX();
        double dy = alvoY - nave.getY();
        double distancia = Math.hypot(dx, dy);
        double vxDesejada = distancia < 1e-6 ? 0 : dx / distancia * velocidade;
        // na vertical, nunca mais rápido do que dá para parar na altura do ponto: sem passar dela por inércia
        double vertical = Math.min(VERTICAL_MAXIMA, Math.sqrt(2 * FRENAGEM_VERTICAL * Math.abs(dy)) + 5);
        double vyDesejada = distancia < 1e-6 ? 0 : limitar(dy / distancia * velocidade, vertical);
        double inclinacao = inclinacaoPerto > 0 ? Math.min(inclinacaoNormal, inclinacaoPerto) : inclinacaoNormal;
        return conduzir(partida, nave, vxDesejada, vyDesejada, inclinacao);
    }

    /** Do vetor de velocidade desejado aos comandos: ângulo do empuxo e modulação do motor. */
    private Comandos conduzir(PartidaDePilotagem partida, NaveEmVoo nave, double vxDesejada, double vyDesejada,
                              double inclinacaoMaxima) {
        ConfigDeVoo c = partida.getConfig();
        double empuxoX = (vxDesejada - nave.getVelocidadeX()) * 2.2;
        double empuxoY = (vyDesejada - nave.getVelocidadeY()) * 2.2 + c.gravidade();
        // o bico nunca aponta para baixo; mas o motor (abaixo) usa o empuxo vertical de verdade, que pode ser
        // negativo: se a nave precisa descer, o motor fica desligado mesmo que falte empuxo para o lado
        double anguloAlvo = limitar(Math.toDegrees(Math.atan2(-empuxoX, Math.max(8, empuxoY))), inclinacaoMaxima);
        double erro = anguloAlvo - nave.getAngulo();
        boolean esquerda = erro > 1.5;
        boolean direita = erro < -1.5;

        double[] bico = nave.direcaoDoBico();
        double projecao = empuxoX * bico[0] + empuxoY * bico[1];
        double fracao = Math.max(0, Math.min(1, projecao / c.empuxo()));
        if (Math.abs(erro) > 50 && nave.getVelocidadeY() > -60) {
            fracao = 0;   // ainda girando para o lado certo: não empurra para o lado errado
        }
        modulacao += fracao;
        boolean motor = modulacao >= 1;
        if (motor) {
            modulacao -= 1;
        }
        return new Comandos(esquerda, direita, motor);
    }

    /** Acompanha se o asteroide do vão (o primeiro da caverna) está descendo. */
    private void observarAsteroide(PartidaDePilotagem partida) {
        double y = partida.getCaverna().getAsteroides().get(0).getY();
        if (!Double.isNaN(asteroideAntes)) {
            descendo = y < asteroideAntes;
        }
        asteroideAntes = y;
    }

    /**
     * Livre: o asteroide acabou de passar desta altura, descendo. Dali ele vai até o fundo (320) e só volta à altura
     * da passagem mais de meio período depois: tempo para atravessar o vão.
     */
    private boolean passagemLivre(PartidaDePilotagem partida, double abaixoDe) {
        double y = partida.getCaverna().getAsteroides().get(0).getY();
        return descendo && y < abaixoDe && y > abaixoDe - 120;
    }

    private static double limitar(double valor, double limite) {
        return Math.max(-limite, Math.min(limite, valor));
    }

    int getTrecho() {
        return trecho;
    }

    int getPonto() {
        return ponto;
    }
}
