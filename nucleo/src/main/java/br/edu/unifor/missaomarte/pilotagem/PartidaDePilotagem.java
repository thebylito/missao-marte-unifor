package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regras do Modo Pilotagem (Fase 7): física da nave, colisões, pouso, embarque e entrega dos passageiros, vidas e
 * pontos. Java puro, como a partida clássica: a apresentação manda {@link Comandos} e lê o estado e os eventos.
 *
 * <p><b>Passo fixo.</b> A simulação anda sempre em passos de {@link #PASSO} (1/120 s). {@link #atualizar} recebe o
 * tempo de um quadro, qualquer que seja a taxa de quadros, e executa quantos passos inteiros couberem; o resto fica
 * para o próximo quadro. Assim a física é a mesma a 30 ou a 144 quadros por segundo, e os testes são exatos.
 *
 * <p><b>Tempo.</b> O tempo da partida é o tempo simulado (passos x 1/120 s). Nenhuma regra lê o relógio.
 *
 * <p>O estado reaproveita {@link Partida.Estado} do jogo clássico (em andamento, vitória, derrota, abortada): a tela
 * de fim e o ranking falam a mesma língua nos dois modos.
 */
public final class PartidaDePilotagem {

    /** Duração de um passo da simulação, em segundos. */
    public static final double PASSO = 1.0 / 120.0;
    /** Faixa abaixo da superfície em que um pé ainda conta como "tocando" a plataforma. */
    static final double TOLERANCIA_DE_CONTATO = 14;
    /** Abaixo desta fração do tanque, um aviso (uma vez por tanque). */
    static final double COMBUSTIVEL_BAIXO = 0.25;
    /**
     * Ao decolar, a nave sobe reta até os pés estarem a esta altura da plataforma; só então o giro vale. Sem isso,
     * girar junto com o motor encostava o pé de fora na plataforma e a nave "pousava" de novo no mesmo instante
     * (achado pelo teste do controlador na Fase 7).
     */
    static final double SUBIDA_ANTES_DE_GIRAR = 6;
    /** Um quadro muito longo (o app voltando do segundo plano) não vira uma avalanche de passos. */
    static final double QUADRO_MAXIMO = 0.25;
    /** Erro de arredondamento tolerado no acumulador (30 quadros de 1/30 s são exatamente 120 passos). */
    private static final double FOLGA = 1e-9;

    private final String piloto;
    private final Dificuldade dificuldade;
    private final ConfigDeVoo config;
    private final Caverna caverna;
    private final NaveEmVoo nave;
    private final List<EventoDeVoo> eventos = new ArrayList<>();
    /** De que plataforma veio cada passageiro (para onde ele volta se a nave explodir). */
    private final Map<Passageiro, PlataformaDePouso> origens = new IdentityHashMap<>();
    private Partida.Estado estado = Partida.Estado.EM_ANDAMENTO;
    private double tempo;
    private double acumulador;
    private double tempoPousada;
    private double tempoDestruida;
    private int vidas;
    private int pontosDosResgates;
    private int bonus;
    private int entregues;
    private int pousos;
    private boolean avisouNaveCheia;
    /** A superfície de onde a nave acabou de decolar (NaN quando já subiu o bastante para girar). */
    private double decolouDe = Double.NaN;
    private boolean avisouCombustivel;

    public PartidaDePilotagem(String piloto, Dificuldade dificuldade, Caverna caverna, ConfigDeVoo config) {
        this.piloto = piloto;
        this.dificuldade = dificuldade;
        this.caverna = caverna;
        this.config = config;
        this.nave = new NaveEmVoo(config.capacidade());
        this.vidas = config.vidas();
        for (PlataformaDePouso p : caverna.getPlataformas()) {
            p.getEsperando().ifPresent(passageiro -> origens.put(passageiro, p));
        }
        posicionarNaBase();
    }

    /** A partida do jogo: a caverna marciana com os números da dificuldade. */
    public static PartidaDePilotagem nova(String piloto, Dificuldade dificuldade) {
        return new PartidaDePilotagem(piloto, dificuldade, Caverna.marciana(), ConfigDeVoo.para(dificuldade));
    }

    // ------------------------------------------------------------------ tempo

    /** Avança a simulação pelo tempo de um quadro, em passos fixos, com os comandos deste quadro. */
    public void atualizar(double segundos, Comandos comandos) {
        if (estado != Partida.Estado.EM_ANDAMENTO) {
            return;
        }
        acumulador += Math.min(Math.max(segundos, 0), QUADRO_MAXIMO);
        while (acumulador + FOLGA >= PASSO && estado == Partida.Estado.EM_ANDAMENTO) {
            avancarUmPasso(comandos);
            acumulador -= PASSO;
        }
    }

    /** Um passo fixo. Público para quem precisa de controle exato (testes e o piloto automático da validação). */
    public void avancarUmPasso(Comandos comandos) {
        if (estado != Partida.Estado.EM_ANDAMENTO) {
            return;
        }
        tempo += PASSO;
        double tempoDosAsteroides = tempo * config.velocidadeDosAsteroides();
        for (AsteroideEmOrbita a : caverna.getAsteroides()) {
            a.posicionar(tempoDosAsteroides);
        }
        switch (nave.estado) {
            case DESTRUIDA:
                esperarANaveNova();
                break;
            case POUSADA:
                pousada(comandos);
                break;
            default:
                voar(comandos);
                break;
        }
    }

    /** O jogador desistiu (botão Abortar ou Esc). Depois disso nada mais muda. */
    public void abortar() {
        if (estado != Partida.Estado.EM_ANDAMENTO) {
            return;
        }
        estado = Partida.Estado.ABORTADA;
        nave.motorLigado = false;
        emitir(EventoDeVoo.Tipo.ABORTADA, "Missão abortada pelo piloto.", 0);
    }

    // ------------------------------------------------------------------ voo

    private void voar(Comandos comandos) {
        if (!Double.isNaN(decolouDe) && nave.y + config.peY() >= decolouDe + SUBIDA_ANTES_DE_GIRAR) {
            decolouDe = Double.NaN;   // os pés já saíram da plataforma: pode girar
        }
        int giro = !Double.isNaN(decolouDe) ? 0
                : (comandos.girarEsquerda() ? 1 : 0) - (comandos.girarDireita() ? 1 : 0);
        nave.angulo = normalizar(nave.angulo + giro * config.giroGrausPorSegundo() * PASSO);

        double ax = 0;
        double ay = -config.gravidade();
        nave.motorLigado = comandos.motor() && nave.combustivel > 0;
        if (nave.motorLigado) {
            double[] bico = nave.direcaoDoBico();
            ax += bico[0] * config.empuxo();
            ay += bico[1] * config.empuxo();
            nave.combustivel = Math.max(0, nave.combustivel - config.consumoPorSegundo() * PASSO);
            avisarSeOCombustivelBaixou();
        }
        nave.velocidadeX += ax * PASSO;
        nave.velocidadeY += ay * PASSO;
        double amortecimento = 1 - config.arrasto() * PASSO;
        nave.velocidadeX *= amortecimento;
        nave.velocidadeY *= amortecimento;
        nave.x += nave.velocidadeX * PASSO;
        nave.y += nave.velocidadeY * PASSO;

        verificarContatos();
    }

    private void avisarSeOCombustivelBaixou() {
        if (!avisouCombustivel && nave.combustivel < config.tanque() * COMBUSTIVEL_BAIXO) {
            avisouCombustivel = true;
            emitir(EventoDeVoo.Tipo.COMBUSTIVEL_BAIXO, "Combustível baixo! Volte à base para reabastecer.", 0);
        }
    }

    private void verificarContatos() {
        double[][] pes = pes();
        // 1) pouso: um pé chegou à superfície de uma plataforma, vindo de cima
        for (PlataformaDePouso p : caverna.getPlataformas()) {
            for (double[] pe : pes) {
                boolean tocando = p.cobre(pe[0])
                        && pe[1] <= p.getSuperficie()
                        && pe[1] >= p.getSuperficie() - TOLERANCIA_DE_CONTATO;
                if (tocando) {
                    tentarPousar(p);
                    return;
                }
            }
        }
        // 2) colisões fatais: casco ou pés contra a rocha ou o corpo das plataformas, asteroides e limites
        List<Rocha> solidos = new ArrayList<>(caverna.getRochas());
        caverna.getPlataformas().forEach(p -> solidos.add(p.corpo()));
        for (Rocha r : solidos) {
            if (r.tocaCirculo(nave.x, nave.y, config.raioDoCasco())
                    || r.contem(pes[0][0], pes[0][1]) || r.contem(pes[1][0], pes[1][1])) {
                destruir("A nave bateu na rocha!");
                return;
            }
        }
        for (AsteroideEmOrbita a : caverna.getAsteroides()) {
            double dx = nave.x - a.getX();
            double dy = nave.y - a.getY();
            double soma = a.getRaio() + config.raioDoCasco();
            if (dx * dx + dy * dy < soma * soma) {
                destruir("Atingida por um asteroide!");
                return;
            }
        }
        if (nave.x < 0 || nave.x > caverna.getLargura() || nave.y < 0 || nave.y > caverna.getAltura()) {
            destruir("A nave se perdeu no espaço!");
        }
    }

    private void tentarPousar(PlataformaDePouso p) {
        Optional<String> problema = RegrasDePouso.problema(config, nave.velocidadeX, nave.velocidadeY, nave.angulo,
                p.cobre(nave.x));
        if (problema.isPresent()) {
            destruir(problema.get());
            return;
        }
        nave.estado = NaveEmVoo.Estado.POUSADA;
        nave.plataforma = p;
        nave.angulo = 0;
        nave.velocidadeX = 0;
        nave.velocidadeY = 0;
        nave.motorLigado = false;
        nave.y = p.getSuperficie() - config.peY();
        tempoPousada = 0;
        avisouNaveCheia = false;
        pousos++;
        boolean naBase = p.getTipo() == PlataformaDePouso.Tipo.BASE;
        emitir(EventoDeVoo.Tipo.POUSOU, naBase ? "Pouso na base." : "Pouso perfeito!", 0);
        if (naBase) {
            entregar();   // na hora do pouso, antes de reabastecer: o bônus usa o combustível que sobrou
        }
    }

    private void pousada(Comandos comandos) {
        if (comandos.motor() && nave.combustivel > 0) {
            nave.estado = NaveEmVoo.Estado.VOANDO;
            decolouDe = nave.plataforma.getSuperficie();
            nave.plataforma = null;
            nave.y += 1;   // descola da superfície para o mesmo pouso não ser contado de novo
            emitir(EventoDeVoo.Tipo.DECOLOU, "", 0);
            voar(comandos);
            return;
        }
        nave.motorLigado = false;
        PlataformaDePouso p = nave.plataforma;
        if (p.getTipo() == PlataformaDePouso.Tipo.BASE) {
            reabastecer();
            return;
        }
        if (p.getEsperando().isEmpty()) {
            return;
        }
        if (!nave.temLugar()) {
            if (!avisouNaveCheia) {
                avisouNaveCheia = true;
                emitir(EventoDeVoo.Tipo.NAVE_CHEIA, "Nave cheia! Leve os passageiros à base.", 0);
            }
            return;
        }
        tempoPousada += PASSO;
        if (tempoPousada + FOLGA >= config.tempoDeEmbarque()) {
            Passageiro passageiro = p.retirar();
            nave.embarcar(passageiro);
            tempoPousada = 0;
            emitir(EventoDeVoo.Tipo.EMBARCOU, passageiro.getNome() + " a bordo!", 0);
        }
    }

    private void reabastecer() {
        nave.combustivel = Math.min(config.tanque(), nave.combustivel + config.reabastecimentoPorSegundo() * PASSO);
        if (nave.combustivel >= config.tanque() * COMBUSTIVEL_BAIXO) {
            avisouCombustivel = false;
        }
    }

    private void entregar() {
        if (nave.getABordo().isEmpty()) {
            return;
        }
        int pontos = 0;
        List<Passageiro> saindo = nave.desembarcarTodos();
        for (Passageiro p : saindo) {
            pontos += p.getPontuacao();   // os mesmos pontos do jogo clássico
        }
        entregues += saindo.size();
        pontosDosResgates += pontos;
        emitir(EventoDeVoo.Tipo.ENTREGOU, saindo.size() + " passageiro(s) resgatado(s)! +" + pontos + " pontos",
                pontos);
        if (entregues == caverna.totalDePassageiros()) {
            bonus = (int) Math.floor(config.bonusMaximo() * nave.combustivel / config.tanque());
            estado = Partida.Estado.VITORIA;
            emitir(EventoDeVoo.Tipo.VITORIA, "Missão cumprida!", getPontos());
        }
    }

    private void destruir(String motivo) {
        nave.estado = NaveEmVoo.Estado.DESTRUIDA;
        nave.motorLigado = false;
        nave.plataforma = null;
        for (Passageiro p : nave.desembarcarTodos()) {
            devolver(p);   // a missão de resgate continua: quem estava a bordo volta a esperar
        }
        vidas--;
        tempoDestruida = 0;
        emitir(EventoDeVoo.Tipo.EXPLODIU, motivo, vidas);
    }

    private void devolver(Passageiro passageiro) {
        origens.get(passageiro).receber(passageiro);
    }

    private void esperarANaveNova() {
        tempoDestruida += PASSO;
        if (tempoDestruida + FOLGA < config.tempoParaRenascer()) {
            return;
        }
        if (vidas <= 0) {
            estado = Partida.Estado.DERROTA;
            emitir(EventoDeVoo.Tipo.DERROTA, "GAME OVER! Todas as naves foram destruídas.", 0);
            return;
        }
        posicionarNaBase();
        emitir(EventoDeVoo.Tipo.RENASCEU, "Nova nave pronta na base.", vidas);
    }

    private void posicionarNaBase() {
        PlataformaDePouso base = caverna.getBase();
        nave.estado = NaveEmVoo.Estado.POUSADA;
        nave.plataforma = base;
        nave.x = base.getCentroX();
        nave.y = base.getSuperficie() - config.peY();
        nave.velocidadeX = 0;
        nave.velocidadeY = 0;
        nave.angulo = 0;
        nave.motorLigado = false;
        nave.combustivel = config.tanque();
        avisouCombustivel = false;
    }

    // ------------------------------------------------------------------ apoio

    /** Onde estão os dois pés no mundo: [esquerdo, direito], cada um {x, y}. */
    public double[][] pes() {
        return new double[][] {
                nave.paraMundo(-config.peX(), config.peY()),
                nave.paraMundo(config.peX(), config.peY())
        };
    }

    private void emitir(EventoDeVoo.Tipo tipo, String mensagem, int valor) {
        eventos.add(new EventoDeVoo(tipo, mensagem, valor, nave.x, nave.y));
    }

    private static double normalizar(double graus) {
        double a = graus % 360;
        if (a > 180) {
            a -= 360;
        }
        if (a <= -180) {
            a += 360;
        }
        return a;
    }

    /** Devolve os eventos desde a última chamada, em ordem, e esvazia a lista. */
    public List<EventoDeVoo> consumirEventos() {
        List<EventoDeVoo> copia = List.copyOf(eventos);
        eventos.clear();
        return copia;
    }

    // ------------------------------------------------------------------ leitura

    public String getPiloto() { return piloto; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public ConfigDeVoo getConfig() { return config; }
    public Caverna getCaverna() { return caverna; }
    public NaveEmVoo getNave() { return nave; }
    public Partida.Estado getEstado() { return estado; }
    /** Tempo simulado, em segundos. */
    public double getTempo() { return tempo; }
    public int getVidas() { return vidas; }
    /** Pontos dos resgates mais o bônus de combustível (o bônus só existe depois da vitória). */
    public int getPontos() { return pontosDosResgates + bonus; }
    public int getPontosDosResgates() { return pontosDosResgates; }
    public int getBonus() { return bonus; }
    public int getEntregues() { return entregues; }
    public int getPousos() { return pousos; }

    /** De 0 a 1: quanto falta para o passageiro da plataforma embarcar (para a tela animar). */
    public double progressoDoEmbarque() {
        return Math.min(1, tempoPousada / config.tempoDeEmbarque());
    }

    public boolean quedaSegura() { return RegrasDePouso.quedaSegura(config, nave.velocidadeY); }
    public boolean deslizeSeguro() { return RegrasDePouso.deslizeSeguro(config, nave.velocidadeX); }
    public boolean inclinacaoSegura() { return RegrasDePouso.inclinacaoSegura(config, nave.angulo); }

    /** Os números da vitória, no mesmo formato do jogo clássico (os "movimentos" são os pousos). */
    public Optional<ResultadoPartida> getResultado() {
        if (estado != Partida.Estado.VITORIA) {
            return Optional.empty();
        }
        return Optional.of(new ResultadoPartida(piloto, dificuldade, getPontos(), pousos, (long) tempo, entregues));
    }
}
