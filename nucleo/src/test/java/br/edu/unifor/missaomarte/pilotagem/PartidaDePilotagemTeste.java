package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Regras do Modo Pilotagem (casos de teste CT-01 a CT-17 da análise da Fase 7), numa caverna de teste plana e sem
 * asteroides: a base à esquerda e três plataformas de passageiro. Sem janela, sem relógio: o tempo anda pelos passos.
 */
class PartidaDePilotagemTeste {

    private static final double CHAO = 50;
    private static final ConfigDeVoo CONFIG = ConfigDeVoo.medio();

    /** Base em x 100-356 e passageiros em x 800, 1400 e 2000 (centros 880, 1480 e 2080). */
    private static Caverna cavernaPlana() {
        return new Caverna(3000, 1500)
                .rocha(0, 0, 3000, CHAO)
                .base(100, CHAO + Caverna.ALTURA_DA_BASE)
                .passageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM), 800, CHAO + Caverna.ALTURA_DA_PLATAFORMA)
                .passageiro(new Engenheiro("Eng. Rosa", Posicao.ORIGEM), 1400, CHAO + Caverna.ALTURA_DA_PLATAFORMA)
                .passageiro(new Professor("Dr. Silva", Posicao.ORIGEM), 2000, CHAO + Caverna.ALTURA_DA_PLATAFORMA);
    }

    private static PartidaDePilotagem partida(Caverna caverna) {
        return new PartidaDePilotagem("Ana", Dificuldade.MEDIO, caverna, CONFIG);
    }

    private static PartidaDePilotagem partida() {
        return partida(cavernaPlana());
    }

    /** Coloca a nave voando num ponto, com velocidade e ângulo dados. */
    private static void voando(PartidaDePilotagem p, double x, double y, double vx, double vy, double angulo) {
        NaveEmVoo n = p.getNave();
        n.estado = NaveEmVoo.Estado.VOANDO;
        n.plataforma = null;
        n.x = x;
        n.y = y;
        n.velocidadeX = vx;
        n.velocidadeY = vy;
        n.angulo = angulo;
    }

    /** A nave a 5 unidades (dos pés) acima da superfície, caindo devagar, em cima do centro da plataforma. */
    private static void prontaParaPousar(PartidaDePilotagem p, PlataformaDePouso plataforma, double vy) {
        voando(p, plataforma.getCentroX(), plataforma.getSuperficie() - CONFIG.peY() + 5, 0, vy, 0);
    }

    /** Avança n passos com os mesmos comandos e devolve os eventos. */
    private static List<EventoDeVoo> passos(PartidaDePilotagem p, int n, Comandos comandos) {
        List<EventoDeVoo> eventos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            p.avancarUmPasso(comandos);
            eventos.addAll(p.consumirEventos());
        }
        return eventos;
    }

    private static int passosEm(double segundos) {
        return (int) Math.round(segundos / PartidaDePilotagem.PASSO);
    }

    private static List<Tipo> tipos(List<EventoDeVoo> eventos) {
        return eventos.stream().map(EventoDeVoo::tipo).toList();
    }

    private static PlataformaDePouso plataforma(PartidaDePilotagem p, int indice) {
        return p.getCaverna().getPlataformas().get(indice);
    }

    // ------------------------------------------------------------------ CT-01 a CT-04: voo

    @Test
    void semComandoANaveCaiCadaVezMaisRapido() {                       // CT-01
        PartidaDePilotagem p = partida();
        voando(p, 1000, 800, 0, 0, 0);
        passos(p, passosEm(0.5), Comandos.NENHUM);
        double meioSegundo = p.getNave().getVelocidadeY();
        passos(p, passosEm(0.5), Comandos.NENHUM);
        assertTrue(meioSegundo < 0, "caindo");
        assertTrue(p.getNave().getVelocidadeY() < meioSegundo, "cada vez mais rápido");
        assertEquals(-CONFIG.gravidade(), p.getNave().getVelocidadeY(), 2.0, "cerca de g x 1 s (menos o arrasto)");
    }

    @Test
    void motorComOBicoParaCimaSobeEGastaTresPorSegundo() {              // CT-02
        PartidaDePilotagem p = partida();
        voando(p, 1000, 600, 0, 0, 0);
        passos(p, passosEm(1.0), Comandos.MOTOR);
        assertTrue(p.getNave().getVelocidadeY() > 80, "empuxo 145 contra gravidade 55");
        assertEquals(CONFIG.tanque() - 3, p.getNave().getCombustivel(), 1e-6);
        assertTrue(p.getNave().isMotorLigado());
    }

    @Test
    void comOTanqueVazioOMotorNaoEmpurra() {                             // CT-03
        PartidaDePilotagem comMotor = partida();
        PartidaDePilotagem semComando = partida();
        voando(comMotor, 1000, 800, 0, 0, 0);
        voando(semComando, 1000, 800, 0, 0, 0);
        comMotor.getNave().combustivel = 0;
        passos(comMotor, 60, Comandos.MOTOR);
        passos(semComando, 60, Comandos.NENHUM);
        assertEquals(semComando.getNave().getVelocidadeY(), comMotor.getNave().getVelocidadeY(), 1e-12);
        assertFalse(comMotor.getNave().isMotorLigado());
        assertEquals(0, comMotor.getNave().getCombustivel());
    }

    @Test
    void girarMeioSegundoMudaOAnguloEmCemGraus() {                       // CT-04
        PartidaDePilotagem esquerda = partida();
        PartidaDePilotagem direita = partida();
        PartidaDePilotagem osDois = partida();
        for (PartidaDePilotagem p : List.of(esquerda, direita, osDois)) {
            voando(p, 1000, 900, 0, 0, 0);
        }
        passos(esquerda, passosEm(0.5), new Comandos(true, false, false));
        passos(direita, passosEm(0.5), new Comandos(false, true, false));
        passos(osDois, passosEm(0.5), new Comandos(true, true, false));
        assertEquals(100, esquerda.getNave().getAngulo(), 1e-6);
        assertEquals(-100, direita.getNave().getAngulo(), 1e-6);
        assertEquals(0, osDois.getNave().getAngulo(), 1e-6, "girar para os dois lados se anula");
    }

    @Test
    void oAnguloFicaEntreMenos180E180() {
        PartidaDePilotagem p = partida();
        voando(p, 1000, 1200, 0, 300, 0);
        passos(p, passosEm(1.0), new Comandos(true, false, false));   // 200 graus para a esquerda
        assertEquals(-160, p.getNave().getAngulo(), 1e-6);
    }

    // ------------------------------------------------------------------ CT-05 a CT-08: pouso e colisões

    @Test
    void pousoNosLimitesExatosESeguro() {                                 // CT-05
        assertEquals(Optional.empty(), RegrasDePouso.problema(CONFIG, 50, -75, 14, true));
        assertEquals(Optional.empty(), RegrasDePouso.problema(CONFIG, -50, -75, -14, true));
        assertEquals(Optional.empty(), RegrasDePouso.problema(CONFIG, 0, 10, 0, true), "subindo devagar também");
    }

    @Test
    void pousoUmPoucoAcimaDeCadaLimiteExplode() {                         // CT-06
        assertTrue(RegrasDePouso.problema(CONFIG, 0, -75.5, 0, true).get().startsWith("Pouso duro!"));
        assertTrue(RegrasDePouso.problema(CONFIG, 50.5, 0, 0, true).get().startsWith("Pouso derrapando!"));
        assertTrue(RegrasDePouso.problema(CONFIG, 0, 0, -14.5, true).get().startsWith("Pouso torto!"));
    }

    @Test
    void pousoComOCentroForaDaPlataformaExplode() {                       // CT-07
        assertEquals(Optional.of("Pouso na beirada! A nave tombou."),
                RegrasDePouso.problema(CONFIG, 0, -10, 0, false));
        PartidaDePilotagem p = partida();
        PlataformaDePouso astronauta = plataforma(p, 1);
        // só o pé direito em cima da plataforma: o centro está 10 para fora da borda esquerda
        voando(p, astronauta.getX() - 10, astronauta.getSuperficie() - CONFIG.peY() + 3, 0, -20, 0);
        List<Tipo> eventos = tipos(passos(p, 60, Comandos.NENHUM));
        assertTrue(eventos.contains(Tipo.EXPLODIU));
        assertEquals(2, p.getVidas());
    }

    @Test
    void pousoDevagarERetoNaPlataformaPara() {
        PartidaDePilotagem p = partida();
        prontaParaPousar(p, plataforma(p, 1), -30);
        List<EventoDeVoo> eventos = passos(p, 60, Comandos.NENHUM);
        assertEquals(Tipo.POUSOU, eventos.get(0).tipo());
        assertEquals(NaveEmVoo.Estado.POUSADA, p.getNave().getEstado());
        assertEquals(plataforma(p, 1), p.getNave().getPlataforma());
        assertEquals(0, p.getNave().getVelocidadeY());
        assertEquals(1, p.getPousos());
    }

    @Test
    void pousoRapidoNaPlataformaExplode() {
        PartidaDePilotagem p = partida();
        prontaParaPousar(p, plataforma(p, 1), -120);
        EventoDeVoo explosao = passos(p, 60, Comandos.NENHUM).get(0);
        assertEquals(Tipo.EXPLODIU, explosao.tipo());
        assertTrue(explosao.mensagem().startsWith("Pouso duro!"));
        assertEquals(2, explosao.valor(), "vidas restantes");
    }

    @Test
    void rochaAsteroideESaidaDaCavernaExplodem() {                        // CT-08
        PartidaDePilotagem naRocha = partida(cavernaPlana().rocha(1000, 500, 200, 200));
        voando(naRocha, 1100, 760, 0, -150, 0);
        assertEquals("A nave bateu na rocha!", passos(naRocha, 60, Comandos.NENHUM).get(0).mensagem());

        PartidaDePilotagem noAsteroide = partida(cavernaPlana().asteroide(
                new AsteroideEmOrbita(1100, 600, 30, 0, 0, 5, 0, 0)));
        voando(noAsteroide, 1100, 680, 0, -100, 0);
        assertEquals("Atingida por um asteroide!", passos(noAsteroide, 60, Comandos.NENHUM).get(0).mensagem());

        PartidaDePilotagem foraDaCaverna = partida();
        voando(foraDaCaverna, 1500, 1495, 0, 200, 0);
        assertEquals("A nave se perdeu no espaço!", passos(foraDaCaverna, 60, Comandos.NENHUM).get(0).mensagem());
    }

    @Test
    void decolarDaPlataformaSoComCombustivel() {
        PartidaDePilotagem p = partida();
        PlataformaDePouso professor = plataforma(p, 3);
        prontaParaPousar(p, professor, -20);
        passos(p, 30, Comandos.NENHUM);
        assertEquals(NaveEmVoo.Estado.POUSADA, p.getNave().getEstado());
        p.getNave().combustivel = 0;      // fora da base não há reabastecimento
        passos(p, 30, Comandos.MOTOR);
        assertEquals(NaveEmVoo.Estado.POUSADA, p.getNave().getEstado(), "sem combustível não decola");
        PartidaDePilotagem comCombustivel = partida();
        assertEquals(NaveEmVoo.Estado.POUSADA, comCombustivel.getNave().getEstado(), "começa pousada na base");
        List<Tipo> eventos = tipos(passos(comCombustivel, 30, Comandos.MOTOR));
        assertEquals(List.of(Tipo.DECOLOU), eventos);
        assertEquals(NaveEmVoo.Estado.VOANDO, comCombustivel.getNave().getEstado());
    }

    @Test
    void girarJuntoComOMotorNaDecolagemNaoFazANavePousarDeNovo() {
        PartidaDePilotagem p = partida();
        List<Tipo> eventos = tipos(passos(p, passosEm(1.0), new Comandos(false, true, true)));
        assertEquals(List.of(Tipo.DECOLOU), eventos, "decolou uma vez só");
        assertEquals(NaveEmVoo.Estado.VOANDO, p.getNave().getEstado());
        assertTrue(p.getNave().getAngulo() < -100, "e girou depois de sair da plataforma");
        assertTrue(p.getNave().getY() > p.getCaverna().getBase().getSuperficie() - CONFIG.peY() + 20);
    }

    @Test
    void naDecolagemANaveSobeRetaAteOsPesSairemDaPlataforma() {
        PartidaDePilotagem p = partida();
        passos(p, 3, new Comandos(true, false, true));
        assertEquals(0, p.getNave().getAngulo(), "ainda com os pés quase na plataforma");
    }

    // ------------------------------------------------------------------ CT-09 a CT-11: passageiros

    @Test
    void passageiroEmbarcaDepoisDeSeisDecimosDeSegundoPousado() {          // CT-09
        PartidaDePilotagem p = partida();
        PlataformaDePouso astronauta = plataforma(p, 1);
        prontaParaPousar(p, astronauta, -20);
        List<EventoDeVoo> pouso = passos(p, 120, Comandos.NENHUM);
        assertTrue(tipos(pouso).contains(Tipo.EMBARCOU), "dentro de 1 s pousada, embarca");
        assertEquals(1, p.getNave().getABordo().size());
        assertTrue(astronauta.getEsperando().isEmpty());

        PartidaDePilotagem exato = partida();
        PlataformaDePouso outra = plataforma(exato, 1);
        prontaParaPousar(exato, outra, -20);
        while (exato.getNave().getEstado() != NaveEmVoo.Estado.POUSADA) {
            exato.avancarUmPasso(Comandos.NENHUM);
        }
        exato.consumirEventos();
        assertFalse(tipos(passos(exato, passosEm(0.6) - 1, Comandos.NENHUM)).contains(Tipo.EMBARCOU), "antes");
        EventoDeVoo embarque = passos(exato, 1, Comandos.NENHUM).get(0);
        assertEquals(Tipo.EMBARCOU, embarque.tipo(), "aos 0,6 s");
        assertEquals("Ast. Maria a bordo!", embarque.mensagem());
    }

    @Test
    void naveCheiaAvisaUmaVezENinguemEmbarca() {                           // CT-10
        PartidaDePilotagem p = partida();
        p.getNave().embarcar(new Astronauta("A", Posicao.ORIGEM));
        p.getNave().embarcar(new Engenheiro("E", Posicao.ORIGEM));
        PlataformaDePouso professor = plataforma(p, 3);
        prontaParaPousar(p, professor, -20);
        List<Tipo> eventos = tipos(passos(p, passosEm(3), Comandos.NENHUM));
        assertEquals(1, eventos.stream().filter(t -> t == Tipo.NAVE_CHEIA).count());
        assertFalse(eventos.contains(Tipo.EMBARCOU));
        assertTrue(professor.getEsperando().isPresent());
    }

    @Test
    void naBaseTodosDesembarcamComOsPontosDoJogoClassicoEOTanqueEnche() {   // CT-11
        PartidaDePilotagem p = partida();
        p.getNave().embarcar(new Astronauta("A", Posicao.ORIGEM));     // 20
        p.getNave().embarcar(new Engenheiro("E", Posicao.ORIGEM));     // 15
        p.getNave().combustivel = 10;
        prontaParaPousar(p, p.getCaverna().getBase(), -20);
        List<EventoDeVoo> eventos = passos(p, 60, Comandos.NENHUM);
        EventoDeVoo entrega = eventos.stream().filter(e -> e.tipo() == Tipo.ENTREGOU).findFirst().orElseThrow();
        assertEquals(35, entrega.valor());
        assertEquals(35, p.getPontos());
        assertEquals(2, p.getEntregues());
        assertTrue(p.getNave().getABordo().isEmpty());
        passos(p, passosEm(5), Comandos.NENHUM);
        assertEquals(CONFIG.tanque(), p.getNave().getCombustivel(), 1e-9, "reabastecido até o máximo");
    }

    @Test
    void combustivelBaixoAvisaUmaVezPorTanque() {
        PartidaDePilotagem p = partida();
        voando(p, 1500, 1000, 0, 0, 0);
        p.getNave().combustivel = CONFIG.tanque() * 0.25 + 0.01;
        List<Tipo> eventos = tipos(passos(p, 60, Comandos.MOTOR));
        assertEquals(1, eventos.stream().filter(t -> t == Tipo.COMBUSTIVEL_BAIXO).count());
        assertEquals(0, tipos(passos(p, 60, Comandos.MOTOR)).stream().filter(t -> t == Tipo.COMBUSTIVEL_BAIXO).count());
    }

    // ------------------------------------------------------------------ CT-12 e CT-13: vidas

    @Test
    void explosaoDevolveOsPassageirosETiraUmaVidaEANaveRenasceNaBase() {   // CT-12
        PartidaDePilotagem p = partida();
        PlataformaDePouso astronauta = plataforma(p, 1);
        prontaParaPousar(p, astronauta, -20);
        passos(p, passosEm(1.5), Comandos.NENHUM);
        assertEquals(1, p.getNave().getABordo().size());
        voando(p, 1500, 600, 0, -300, 0);    // cai no chão a toda (bate em cerca de 1,4 s)
        List<EventoDeVoo> queda = passos(p, passosEm(2.0), Comandos.NENHUM);
        assertEquals(Tipo.EXPLODIU, queda.get(0).tipo());
        assertEquals(2, p.getVidas());
        assertTrue(p.getNave().getABordo().isEmpty());
        assertTrue(astronauta.getEsperando().isPresent(), "o Astronauta voltou para a plataforma dele");
        assertEquals(NaveEmVoo.Estado.DESTRUIDA, p.getNave().getEstado());

        List<Tipo> depois = tipos(passos(p, passosEm(2.0), Comandos.NENHUM));
        assertTrue(depois.contains(Tipo.RENASCEU));
        assertEquals(NaveEmVoo.Estado.POUSADA, p.getNave().getEstado());
        assertEquals(p.getCaverna().getBase(), p.getNave().getPlataforma());
        assertEquals(CONFIG.tanque(), p.getNave().getCombustivel());
    }

    @Test
    void aTerceiraExplosaoTerminaEmDerrota() {                              // CT-13
        PartidaDePilotagem p = partida();
        for (int vida = 3; vida >= 1; vida--) {
            voando(p, 1500, 600, 0, -300, 0);
            passos(p, passosEm(4.0), Comandos.NENHUM);   // bate em ~1,4 s; a nave nova surge 2 s depois
        }
        assertEquals(Partida.Estado.DERROTA, p.getEstado());
        assertEquals(0, p.getVidas());
        assertEquals(Optional.empty(), p.getResultado());
    }

    // ------------------------------------------------------------------ CT-14 e CT-15: fim

    private static PartidaDePilotagem umSoPassageiro() {
        Caverna c = new Caverna(3000, 1500).rocha(0, 0, 3000, CHAO).base(100, CHAO + Caverna.ALTURA_DA_BASE)
                .passageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM), 800, CHAO + Caverna.ALTURA_DA_PLATAFORMA);
        return partida(c);
    }

    private static void entregarComCombustivel(PartidaDePilotagem p, double combustivel) {
        p.getNave().embarcar(plataforma(p, 1).retirar());
        p.getNave().combustivel = combustivel;
        prontaParaPousar(p, p.getCaverna().getBase(), -20);
        passos(p, 60, Comandos.NENHUM);
    }

    @Test
    void entregarTodosDaVitoriaComBonusDeCombustivel() {                    // CT-14
        PartidaDePilotagem cheio = umSoPassageiro();
        entregarComCombustivel(cheio, CONFIG.tanque());
        assertEquals(Partida.Estado.VITORIA, cheio.getEstado());
        assertEquals(30, cheio.getBonus());
        assertEquals(50, cheio.getPontos(), "20 do Astronauta + 30 de bônus");

        PartidaDePilotagem vazio = umSoPassageiro();
        entregarComCombustivel(vazio, 0);
        assertEquals(0, vazio.getBonus());
        assertEquals(20, vazio.getPontos());

        PartidaDePilotagem metade = umSoPassageiro();
        entregarComCombustivel(metade, 51);
        assertEquals(15, metade.getBonus(), "30 x 0,51 = 15,3, arredondado para baixo");

        ResultadoPartida r = metade.getResultado().orElseThrow();
        assertEquals(new ResultadoPartida("Ana", Dificuldade.MEDIO, 35, 1, (long) metade.getTempo(), 1), r);
    }

    @Test
    void abortarTerminaEDepoisNadaMaisMuda() {                              // CT-15
        PartidaDePilotagem p = partida();
        voando(p, 1500, 900, 30, 0, 0);
        passos(p, 30, Comandos.NENHUM);
        p.abortar();
        assertEquals(List.of(Tipo.ABORTADA), tipos(p.consumirEventos()));
        assertEquals(Partida.Estado.ABORTADA, p.getEstado());
        double tempo = p.getTempo();
        double x = p.getNave().getX();
        p.atualizar(1.0, Comandos.MOTOR);
        passos(p, 30, Comandos.MOTOR);
        p.abortar();
        assertEquals(tempo, p.getTempo());
        assertEquals(x, p.getNave().getX());
        assertTrue(p.consumirEventos().isEmpty());
    }

    // ------------------------------------------------------------------ CT-16 e CT-17

    /** Motor por 1 s, girando para a esquerda por mais 0,5 s, solto por mais 0,5 s: quadros de um tamanho fixo. */
    private static PartidaDePilotagem voarComQuadrosDe(double quadro) {
        PartidaDePilotagem p = partida();
        voando(p, 1500, 600, 0, 0, 0);
        int quadros = (int) Math.round(2.0 / quadro);
        for (int i = 0; i < quadros; i++) {
            double t = i * quadro;
            Comandos c = t < 1.0 - 1e-9 ? Comandos.MOTOR
                    : t < 1.5 - 1e-9 ? new Comandos(true, false, false) : Comandos.NENHUM;
            p.atualizar(quadro, c);
        }
        return p;
    }

    @Test
    void osMesmosComandosDaoOMesmoVooA30EA144QuadrosPorSegundo() {          // CT-16
        PartidaDePilotagem a30 = voarComQuadrosDe(1.0 / 30);
        PartidaDePilotagem a144 = voarComQuadrosDe(1.0 / 144);
        assertEquals(240, Math.round(a30.getTempo() / PartidaDePilotagem.PASSO), "2 s = 240 passos");
        assertEquals(a30.getTempo(), a144.getTempo(), 1e-9);
        NaveEmVoo n30 = a30.getNave();
        NaveEmVoo n144 = a144.getNave();
        assertEquals(n30.getX(), n144.getX(), 1e-6);
        assertEquals(n30.getY(), n144.getY(), 1e-6);
        assertEquals(n30.getVelocidadeY(), n144.getVelocidadeY(), 1e-6);
        assertEquals(n30.getAngulo(), n144.getAngulo(), 1e-6);
        assertEquals(n30.getCombustivel(), n144.getCombustivel(), 1e-6);
    }

    @Test
    void umQuadroMuitoLongoNaoViraUmaAvalancheDePassos() {
        PartidaDePilotagem p = partida();
        p.atualizar(5.0, Comandos.NENHUM);   // o app voltando do segundo plano
        assertEquals(PartidaDePilotagem.QUADRO_MAXIMO, p.getTempo(), PartidaDePilotagem.PASSO);
    }

    @Test
    void aDificuldadeMudaOTanqueOsLimitesDePousoEOsAsteroides() {           // CT-17
        ConfigDeVoo facil = ConfigDeVoo.para(Dificuldade.FACIL);
        ConfigDeVoo medio = ConfigDeVoo.para(Dificuldade.MEDIO);
        ConfigDeVoo dificil = ConfigDeVoo.para(Dificuldade.DIFICIL);
        assertEquals(List.of(130.0, 100.0, 100.0), List.of(facil.tanque(), medio.tanque(), dificil.tanque()));
        assertEquals(List.of(90.0, 75.0, 65.0), List.of(facil.quedaMaxima(), medio.quedaMaxima(), dificil.quedaMaxima()));
        assertEquals(List.of(60.0, 50.0, 40.0),
                List.of(facil.deslizeMaximo(), medio.deslizeMaximo(), dificil.deslizeMaximo()));
        assertEquals(List.of(18.0, 14.0, 12.0),
                List.of(facil.inclinacaoMaxima(), medio.inclinacaoMaxima(), dificil.inclinacaoMaxima()));
        assertEquals(List.of(0.75, 1.0, 1.3), List.of(facil.velocidadeDosAsteroides(), medio.velocidadeDosAsteroides(),
                dificil.velocidadeDosAsteroides()));
        for (ConfigDeVoo c : List.of(facil, dificil)) {   // o resto é o da POC
            assertEquals(medio.gravidade(), c.gravidade());
            assertEquals(medio.empuxo(), c.empuxo());
            assertEquals(medio.giroGrausPorSegundo(), c.giroGrausPorSegundo());
            assertEquals(medio.consumoPorSegundo(), c.consumoPorSegundo());
        }
        assertEquals(new ConfigDeVoo(55, 145, 200, 0.06, 100, 3, 35, 75, 50, 14, 30, 24, -40, 0.6, 2, 3, 2.0, 1.0, 30),
                medio, "os valores da POC");
    }

    @Test
    void osAsteroidesAndamNaVelocidadeDaDificuldade() {
        PartidaDePilotagem facil = PartidaDePilotagem.nova("A", Dificuldade.FACIL);
        PartidaDePilotagem dificil = PartidaDePilotagem.nova("A", Dificuldade.DIFICIL);
        passos(facil, passosEm(1.0), Comandos.NENHUM);
        passos(dificil, passosEm(1.0), Comandos.NENHUM);
        // o primeiro asteroide sobe e desce (seno): no mesmo instante, o do Difícil já subiu mais
        double inicio = 520;
        double subidaFacil = facil.getCaverna().getAsteroides().get(0).getY() - inicio;
        double subidaDificil = dificil.getCaverna().getAsteroides().get(0).getY() - inicio;
        assertEquals(200 * Math.sin(2 * Math.PI * 0.75 / 6), subidaFacil, 1e-6);
        assertEquals(200 * Math.sin(2 * Math.PI * 1.3 / 6), subidaDificil, 1e-6);
    }

    @Test
    void aCavernaMarcianaTemTresPassageirosDoJogoClassicoECadaPartidaUmaNova() {
        PartidaDePilotagem p = PartidaDePilotagem.nova("Ana", Dificuldade.MEDIO);
        Caverna c = p.getCaverna();
        assertEquals(3, c.totalDePassageiros());
        assertEquals(List.of(20, 15, 10), c.getPlataformas().stream()
                .filter(pl -> pl.getTipo() == PlataformaDePouso.Tipo.PASSAGEIRO)
                .map(pl -> pl.getEsperando().orElseThrow().getPontuacao()).toList());
        assertTrue(c != PartidaDePilotagem.nova("Bia", Dificuldade.MEDIO).getCaverna());
        assertEquals(NaveEmVoo.Estado.POUSADA, p.getNave().getEstado());
        assertEquals(c.getBase(), p.getNave().getPlataforma());
        assertEquals(3, p.getVidas());
    }
}
