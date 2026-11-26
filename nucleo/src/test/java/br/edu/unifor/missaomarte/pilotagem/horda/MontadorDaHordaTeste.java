package br.edu.unifor.missaomarte.pilotagem.horda;

import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.PONTO_DO_ASTRONAUTA;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.PONTO_DO_ENGENHEIRO;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.PONTO_DO_PROFESSOR;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.marcianaDescrita;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.pilotagem.AjustesDaDificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import br.edu.unifor.missaomarte.pilotagem.AsteroideEmOrbita;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.ConfigDeVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.PlataformaDePouso;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A horda descrita vira uma partida: a caverna pela API dela e a configuração da dificuldade com os modificadores. */
class MontadorDaHordaTeste {

    private static final DescricaoDeCaverna MARCIANA = marcianaDescrita();
    private static final List<Double> FASES_ORIGINAIS = List.of(0.0, 1.2, 2.4);
    private static final List<PerigoDoRoteiro> ROTEIRO = List.of(
            new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 30, 99),
            new PerigoDoRoteiro(TipoDeEvento.CHUVA_DE_METEOROS, 80, -5));

    private static List<PassageiroNaHorda> passageiros(int astronauta, int engenheiro, int professor) {
        return List.of(new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, astronauta),
                new PassageiroNaHorda(TipoPassageiro.ENGENHEIRO, engenheiro),
                new PassageiroNaHorda(TipoPassageiro.PROFESSOR, professor));
    }

    private static final List<PassageiroNaHorda> NOS_LUGARES_DO_MARCIANA =
            passageiros(PONTO_DO_ASTRONAUTA, PONTO_DO_ENGENHEIRO, PONTO_DO_PROFESSOR);

    private static DescricaoDaHorda horda(Dificuldade dificuldade, List<PassageiroNaHorda> passageiros,
                                          double velocidade, List<Double> fases, List<Modificador> modificadores) {
        return new DescricaoDaHorda(1, dificuldade, MARCIANA, passageiros, velocidade, fases, modificadores, ROTEIRO);
    }

    /** A horda que reproduz o {@code Caverna.marciana()}: passageiros e fases originais, sem modificadores. */
    private static DescricaoDaHorda hordaDoMarciana(Dificuldade dificuldade) {
        return horda(dificuldade, NOS_LUGARES_DO_MARCIANA, 1.0, FASES_ORIGINAIS, List.of());
    }

    private static DescricaoDaHorda comModificadores(Dificuldade dificuldade, double velocidade,
                                                     Modificador... modificadores) {
        return horda(dificuldade, NOS_LUGARES_DO_MARCIANA, velocidade, FASES_ORIGINAIS, List.of(modificadores));
    }

    /** O que importa de cada plataforma: tipo, x, superfície, largura, altura e quem espera (tipo e nome). */
    private static List<String> plataformas(Caverna c) {
        List<String> descritas = new ArrayList<>();
        for (PlataformaDePouso p : c.getPlataformas()) {
            String esperando = p.getEsperando().map(q -> q.getTipo() + " " + q.getNome()).orElse("ninguém");
            descritas.add(p.getTipo() + " x=" + p.getX() + " sup=" + p.getSuperficie() + " " + p.getLargura() + "x"
                    + p.getAltura() + " " + esperando);
        }
        return descritas;
    }

    /** Onde cada asteroide está no instante zero (a fase decide), com raio, período e rotação. */
    private static List<String> asteroides(Caverna c) {
        List<String> descritos = new ArrayList<>();
        for (AsteroideEmOrbita a : c.getAsteroides()) {
            descritos.add(a.getX() + "," + a.getY() + " r=" + a.getRaio() + " T=" + a.getPeriodo() + " rot="
                    + a.getRotacao());
        }
        return descritos;
    }

    // ------------------------------------------------------------------ a caverna

    @Test
    void aHordaDoMarcianaMontaAMesmaCavernaDoMarciana() {
        Caverna esperada = Caverna.marciana();
        Caverna montada = MontadorDaHorda.montar(hordaDoMarciana(Dificuldade.MEDIO)).caverna();

        assertEquals(esperada.getLargura(), montada.getLargura());
        assertEquals(esperada.getAltura(), montada.getAltura());
        assertEquals(esperada.getRochas(), montada.getRochas());
        assertEquals(plataformas(esperada), plataformas(montada));
        assertEquals(asteroides(esperada), asteroides(montada));
        assertSame(montada.getPlataformas().get(0), montada.getBase());
        assertEquals(3, montada.totalDePassageiros());
    }

    @Test
    void osPassageirosFicamNosPontosSorteados() {
        Caverna c = MontadorDaHorda.montar(horda(Dificuldade.MEDIO, passageiros(4, 0, 3), 1.0, FASES_ORIGINAIS,
                List.of())).caverna();

        List<PlataformaDePouso> p = c.getPlataformas();
        assertEquals(4, p.size());
        assertEquals(PlataformaDePouso.Tipo.BASE, p.get(0).getTipo());
        conferirPassageiro(p.get(1), MARCIANA.pontosDePouso().get(4), TipoPassageiro.ASTRONAUTA, "Ast. Maria");
        conferirPassageiro(p.get(2), MARCIANA.pontosDePouso().get(0), TipoPassageiro.ENGENHEIRO, "Eng. Rosa");
        conferirPassageiro(p.get(3), MARCIANA.pontosDePouso().get(3), TipoPassageiro.PROFESSOR, "Dr. Silva");
    }

    private static void conferirPassageiro(PlataformaDePouso plataforma, PontoDePouso ponto, TipoPassageiro tipo,
                                           String nome) {
        assertEquals(PlataformaDePouso.Tipo.PASSAGEIRO, plataforma.getTipo());
        assertEquals(ponto.x(), plataforma.getX());
        assertEquals(ponto.superficie(), plataforma.getSuperficie());
        assertEquals(tipo, plataforma.getEsperando().orElseThrow().getTipo());
        assertEquals(nome, plataforma.getEsperando().orElseThrow().getNome());
    }

    @Test
    void aOrdemDaListaDePassageirosNaoMudaAOrdemDasPlataformas() {
        List<PassageiroNaHorda> embaralhados = List.of(
                new PassageiroNaHorda(TipoPassageiro.PROFESSOR, PONTO_DO_PROFESSOR),
                new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, PONTO_DO_ASTRONAUTA),
                new PassageiroNaHorda(TipoPassageiro.ENGENHEIRO, PONTO_DO_ENGENHEIRO));
        Caverna c = MontadorDaHorda.montar(horda(Dificuldade.MEDIO, embaralhados, 1.0, FASES_ORIGINAIS, List.of()))
                .caverna();
        assertEquals(plataformas(Caverna.marciana()), plataformas(c));
    }

    @Test
    void asFasesSorteadasSubstituemAsDaDescricao() {
        List<Double> fases = List.of(1.0, 2.0, 3.0);
        Caverna c = MontadorDaHorda.montar(horda(Dificuldade.MEDIO, NOS_LUGARES_DO_MARCIANA, 1.0, fases, List.of()))
                .caverna();
        for (int i = 0; i < fases.size(); i++) {
            AsteroideDescrito descrito = MARCIANA.asteroides().get(i);
            AsteroideEmOrbita montado = c.getAsteroides().get(i);
            assertEquals(descrito.centroX() + descrito.amplitudeX() * Math.sin(fases.get(i)), montado.getX(), 1e-9);
            assertEquals(descrito.centroY() + descrito.amplitudeY() * Math.sin(fases.get(i)), montado.getY(), 1e-9);
            assertEquals(descrito.raio(), montado.getRaio());
            assertEquals(descrito.periodo(), montado.getPeriodo());
        }
    }

    @Test
    void cadaMontagemEUmaCavernaNova() {
        DescricaoDaHorda h = hordaDoMarciana(Dificuldade.MEDIO);
        assertNotSame(MontadorDaHorda.montar(h).caverna(), MontadorDaHorda.montar(h).caverna());
    }

    // ------------------------------------------------------------------ a configuração

    @Test
    void semModificadoresAConfigEADaDificuldade() {
        for (Dificuldade d : Dificuldade.values()) {
            HordaMontada m = MontadorDaHorda.montar(hordaDoMarciana(d));
            assertEquals(ConfigDeVoo.para(d), m.config(), d.name());
            assertEquals(0, m.ventoConstante());
        }
    }

    @Test
    void osModificadoresMultiplicamGravidadeTanqueEVelocidade() {
        HordaMontada m = MontadorDaHorda.montar(comModificadores(Dificuldade.FACIL, 1.2,
                new Modificador(TipoDeModificador.VENTO, -12.5),
                new Modificador(TipoDeModificador.GRAVIDADE, 1.15),
                new Modificador(TipoDeModificador.TANQUE, 0.9)));
        ConfigDeVoo b = ConfigDeVoo.para(Dificuldade.FACIL);
        ConfigDeVoo esperada = new ConfigDeVoo(b.gravidade() * 1.15, b.empuxo(), b.giroGrausPorSegundo(),
                b.arrasto(), b.tanque() * 0.9, b.consumoPorSegundo(), b.reabastecimentoPorSegundo(),
                b.quedaMaxima(), b.deslizeMaximo(), b.inclinacaoMaxima(), b.raioDoCasco(), b.peX(), b.peY(),
                b.tempoDeEmbarque(), b.capacidade(), b.vidas(), b.tempoParaRenascer(),
                b.velocidadeDosAsteroides() * 1.2, b.bonusMaximo());

        assertEquals(esperada, m.config());
        assertEquals(55 * 1.15, m.config().gravidade(), 1e-9);
        assertEquals(130 * 0.9, m.config().tanque(), 1e-9);
        assertEquals(0.75 * 1.2, m.config().velocidadeDosAsteroides(), 1e-9);
        assertEquals(-12.5, m.ventoConstante());
    }

    @Test
    void soOVentoNaoMexeNaConfig() {
        HordaMontada m = MontadorDaHorda.montar(comModificadores(Dificuldade.DIFICIL, 1.0,
                new Modificador(TipoDeModificador.VENTO, 9)));
        assertEquals(ConfigDeVoo.para(Dificuldade.DIFICIL), m.config());
        assertEquals(9, m.ventoConstante());
    }

    @Test
    void soAGravidade() {
        HordaMontada m = MontadorDaHorda.montar(comModificadores(Dificuldade.MEDIO, 1.0,
                new Modificador(TipoDeModificador.GRAVIDADE, 1.2)));
        ConfigDeVoo b = ConfigDeVoo.para(Dificuldade.MEDIO);
        assertEquals(b.gravidade() * 1.2, m.config().gravidade());
        assertEquals(b.tanque(), m.config().tanque());
        assertEquals(b.velocidadeDosAsteroides(), m.config().velocidadeDosAsteroides());
        assertEquals(0, m.ventoConstante());
    }

    /** Fase 8 (DA-48): a horda monta com os ajustes gravados nela, e os modificadores valem por cima deles. */
    @Test
    void aHordaMontaComOsAjustesDelaEOsModificadoresPorCima() {
        AjustesDosEventos eventos = new AjustesDosEventos(200, 300, 30, 25, 20, 45);
        AjustesDeVoo ajustes = new AjustesDeVoo(AjustesDeVoo.PADRAO.facil(), AjustesDeVoo.PADRAO.medio(),
                new AjustesDaDificuldade(120, 70, 45, 18, 1.2), eventos);
        DescricaoDaHorda h = comModificadores(Dificuldade.DIFICIL, 1.1,
                new Modificador(TipoDeModificador.TANQUE, 0.9)).comAjustes(ajustes);

        ConfigDeVoo config = MontadorDaHorda.montar(h).config();

        assertEquals(120 * 0.9, config.tanque(), 1e-9);
        assertEquals(70, config.quedaMaxima());
        assertEquals(45, config.deslizeMaximo());
        assertEquals(18, config.inclinacaoMaxima());
        assertEquals(1.2 * 1.1, config.velocidadeDosAsteroides(), 1e-9);
        assertEquals(eventos, config.eventos());
        assertEquals(ConfigDeVoo.para(Dificuldade.DIFICIL).gravidade(), config.gravidade());
    }

    @Test
    void oRoteiroEODaHorda() {
        assertEquals(ROTEIRO, MontadorDaHorda.montar(hordaDoMarciana(Dificuldade.MEDIO)).roteiro());
    }

    // ------------------------------------------------------------------ da semente à partida

    @Test
    void umaHordaSorteadaViraUmaPartidaJogavel() {
        DescricaoDaHorda h = SorteioDaHorda.sortear(42, 20_261_271L, List.of(MARCIANA, CavernasDeTeste.planicie()),
                null);
        HordaMontada m = MontadorDaHorda.montar(h);
        PartidaDePilotagem p = new PartidaDePilotagem("Ana", h.dificuldade(), m.caverna(), m.config());
        for (int i = 0; i < 240; i++) {
            p.avancarUmPasso(Comandos.NENHUM);
        }
        assertEquals(Partida.Estado.EM_ANDAMENTO, p.getEstado());
        assertEquals(3, p.getCaverna().esperando());
        assertEquals(m.config(), p.getConfig());
    }

    // ------------------------------------------------------------------ hordas mal formadas

    @Test
    void passageirosQueNaoSaoUmDeCadaSaoErro() {
        List<PassageiroNaHorda> doisAstronautas = List.of(
                new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 0),
                new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 1),
                new PassageiroNaHorda(TipoPassageiro.PROFESSOR, 2));
        assertThrows(IllegalArgumentException.class, () -> MontadorDaHorda.montar(
                horda(Dificuldade.MEDIO, doisAstronautas, 1.0, FASES_ORIGINAIS, List.of())));
        assertThrows(IllegalArgumentException.class, () -> MontadorDaHorda.montar(
                horda(Dificuldade.MEDIO, NOS_LUGARES_DO_MARCIANA.subList(0, 2), 1.0, FASES_ORIGINAIS, List.of())));
    }

    @Test
    void pontoQueNaoExisteEErro() {
        assertThrows(IllegalArgumentException.class, () -> MontadorDaHorda.montar(
                horda(Dificuldade.MEDIO, passageiros(0, 1, 5), 1.0, FASES_ORIGINAIS, List.of())));
    }

    @Test
    void modificadorRepetidoEErro() {
        assertThrows(IllegalArgumentException.class, () -> MontadorDaHorda.montar(comModificadores(Dificuldade.MEDIO,
                1.0, new Modificador(TipoDeModificador.GRAVIDADE, 1.1), new Modificador(TipoDeModificador.GRAVIDADE, 1.2))));
    }

    @Test
    void umaFasePorAsteroide() {
        assertThrows(IllegalArgumentException.class, () -> MontadorDaHorda.montar(
                horda(Dificuldade.MEDIO, NOS_LUGARES_DO_MARCIANA, 1.0, List.of(0.0, 1.2), List.of())));
    }
}
