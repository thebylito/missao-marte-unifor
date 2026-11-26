package br.edu.unifor.missaomarte.pilotagem.horda;

import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comPontos;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.liberada;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.marcianaDescrita;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.planicie;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** O sorteio da horda (CT-01 e CT-02 da análise da Fase 8): determinístico, dentro das faixas e com rodízio. */
class SorteioDaHordaTeste {

    private static final DescricaoDeCaverna MARCIANA = marcianaDescrita();
    private static final DescricaoDeCaverna PLANICIE = planicie();
    private static final List<DescricaoDeCaverna> DUAS = List.of(MARCIANA, PLANICIE);

    /**
     * 200 sementes de 64 bits tiradas de uma fonte fixa, como as do servidor (HMAC-SHA256 do número da horda). As
     * sementes pequenas e seguidas 0 a 199 dão 19/43/38% de dificuldades: o Difícil fica 2,5 desvios acima de 30% por
     * flutuação de amostra, e não por erro do sorteio (com 100 000 sementes seguidas o resultado é 20,2/50,0/29,9%).
     * O teste {@link #dificuldadesConvergemComMuitasSementes()} confere as proporções numa amostra grande, para a
     * escolha da amostra pequena não esconder um erro.
     */
    private static final long[] SEMENTES = new SplittableRandom(2026).longs(200).toArray();

    /** As 200 hordas das {@link #SEMENTES} (número = posição + 1). */
    private static List<DescricaoDaHorda> hordas(List<DescricaoDeCaverna> liberadas, String anterior) {
        List<DescricaoDaHorda> hordas = new ArrayList<>();
        for (int i = 0; i < SEMENTES.length; i++) {
            hordas.add(SorteioDaHorda.sortear(i + 1, SEMENTES[i], liberadas, anterior));
        }
        return hordas;
    }

    private static List<DescricaoDaHorda> hordas() {
        return hordas(DUAS, null);
    }

    private static Set<String> cavernas(List<DescricaoDaHorda> hordas) {
        Set<String> ids = new HashSet<>();
        hordas.forEach(h -> ids.add(h.caverna().id()));
        return ids;
    }

    private static boolean alguma(List<DescricaoDaHorda> hordas, Predicate<DescricaoDaHorda> condicao) {
        return hordas.stream().anyMatch(condicao);
    }

    // ------------------------------------------------------------------ determinismo

    @Test
    void aMesmaSementeSorteiaAMesmaHorda() {
        DescricaoDaHorda primeira = SorteioDaHorda.sortear(7, 123_456_789L, DUAS, "marciana");
        DescricaoDaHorda segunda = SorteioDaHorda.sortear(7, 123_456_789L, List.of(marcianaDescrita(), planicie()),
                "marciana");
        assertEquals(primeira, segunda);
    }

    @Test
    void oNumeroSoECopiado() {
        DescricaoDaHorda h1 = SorteioDaHorda.sortear(1, 42, DUAS, null);
        DescricaoDaHorda h2 = SorteioDaHorda.sortear(2, 42, DUAS, null);
        assertEquals(1, h1.numero());
        assertEquals(2, h2.numero());
        assertEquals(h1, new DescricaoDaHorda(1, h2.dificuldade(), h2.caverna(), h2.passageiros(),
                h2.velocidadeDosAsteroides(), h2.fasesDosAsteroides(), h2.modificadores(), h2.roteiro()));
    }

    /**
     * Teste dourado: o resultado completo de uma semente conhecida. Se ele quebrar sem que as regras tenham mudado, a
     * ordem do sorteio (documentada no {@link SorteioDaHorda}) mudou por acidente, e todas as hordas já publicadas
     * deixariam de ser reproduzíveis.
     */
    @Test
    void dourado() {
        DescricaoDaHorda esperada = new DescricaoDaHorda(42, Dificuldade.FACIL, MARCIANA,
                List.of(new PassageiroNaHorda(TipoPassageiro.ASTRONAUTA, 4),
                        new PassageiroNaHorda(TipoPassageiro.ENGENHEIRO, 3),
                        new PassageiroNaHorda(TipoPassageiro.PROFESSOR, 1)),
                0.8192673837277159,
                List.of(6.062375343597668, 2.597835620122015, 0.447109787261378),
                List.of(new Modificador(TipoDeModificador.VENTO, 18.627877685732873),
                        new Modificador(TipoDeModificador.TANQUE, 0.9230916067456735)),
                List.of(new PerigoDoRoteiro(TipoDeEvento.CHUVA_DE_METEOROS, 24.308914625842775, 435916803858618890L),
                        new PerigoDoRoteiro(TipoDeEvento.PANE_DE_GRAVIDADE, 45.77818079404612, -5032694206115545595L)));
        assertEquals(esperada, SorteioDaHorda.sortear(42, 20_261_271L, DUAS, null));
    }

    @Test
    void aOrdemDaListaDeCavernasNaoMudaOSorteio() {
        for (long semente = 0; semente < 20; semente++) {
            assertEquals(SorteioDaHorda.sortear(1, semente, List.of(MARCIANA, PLANICIE), null),
                    SorteioDaHorda.sortear(1, semente, List.of(PLANICIE, MARCIANA), null));
        }
    }

    @Test
    void sementesDiferentesVariamCavernaPontosDificuldadeEModificadores() {
        List<DescricaoDaHorda> hordas = hordas();
        assertEquals(Set.of("marciana", "planicie"), cavernas(hordas));
        Set<Dificuldade> dificuldades = new HashSet<>();
        Set<List<PassageiroNaHorda>> pontos = new HashSet<>();
        Set<Integer> quantosModificadores = new HashSet<>();
        Set<Integer> quantosPerigos = new HashSet<>();
        for (DescricaoDaHorda h : hordas) {
            dificuldades.add(h.dificuldade());
            pontos.add(h.passageiros());
            quantosModificadores.add(h.modificadores().size());
            quantosPerigos.add(h.roteiro().size());
        }
        assertEquals(Set.of(Dificuldade.values()), dificuldades);
        assertTrue(pontos.size() > 20, "os passageiros mudam de ponto: " + pontos.size());
        assertEquals(Set.of(0, 1, 2), quantosModificadores);
        assertEquals(Set.of(1, 2), quantosPerigos);
        assertNotEquals(hordas.get(0), hordas.get(1));
    }

    // ------------------------------------------------------------------ faixas

    @Test
    void passageirosUmDeCadaEmTresPontosDistintos() {
        for (DescricaoDaHorda h : hordas()) {
            List<TipoPassageiro> tipos = h.passageiros().stream().map(PassageiroNaHorda::tipo).toList();
            assertEquals(List.of(TipoPassageiro.ASTRONAUTA, TipoPassageiro.ENGENHEIRO, TipoPassageiro.PROFESSOR), tipos);
            Set<Integer> pontos = new HashSet<>();
            for (PassageiroNaHorda p : h.passageiros()) {
                assertTrue(p.ponto() >= 0 && p.ponto() < h.caverna().pontosDePouso().size(), "ponto " + p.ponto());
                pontos.add(p.ponto());
            }
            assertEquals(3, pontos.size(), "pontos distintos: " + h.passageiros());
        }
    }

    @Test
    void velocidadeEFasesDosAsteroidesNasFaixas() {
        for (DescricaoDaHorda h : hordas()) {
            assertTrue(h.velocidadeDosAsteroides() >= 0.8 && h.velocidadeDosAsteroides() <= 1.3,
                    "velocidade " + h.velocidadeDosAsteroides());
            assertEquals(h.caverna().asteroides().size(), h.fasesDosAsteroides().size(), "uma fase por asteroide");
            for (double fase : h.fasesDosAsteroides()) {
                assertTrue(fase >= 0 && fase < 2 * Math.PI, "fase " + fase);
            }
        }
    }

    @Test
    void modificadoresDeTiposDistintosNasFaixas() {
        for (DescricaoDaHorda h : hordas()) {
            assertTrue(h.modificadores().size() <= 2, "no máximo 2: " + h.modificadores());
            Set<TipoDeModificador> tipos = new HashSet<>();
            for (Modificador m : h.modificadores()) {
                assertTrue(tipos.add(m.tipo()), "tipos distintos: " + h.modificadores());
                double v = m.valor();
                switch (m.tipo()) {
                    case VENTO -> assertTrue(Math.abs(v) >= 8 && Math.abs(v) <= 20, "vento " + v);
                    case GRAVIDADE -> assertTrue(v >= 1.1 && v <= 1.2, "gravidade " + v);
                    case TANQUE -> assertTrue(v >= 0.85 && v <= 1.0, "tanque " + v);
                }
            }
        }
    }

    @Test
    void nuncaCortaOTanqueNoDificil() {
        List<DescricaoDaHorda> hordas = hordas();
        Predicate<DescricaoDaHorda> temTanque = h -> h.modificadores().stream()
                .anyMatch(m -> m.tipo() == TipoDeModificador.TANQUE);
        for (DescricaoDaHorda h : hordas) {
            if (h.dificuldade() == Dificuldade.DIFICIL) {
                assertFalse(temTanque.test(h), "tanque no Difícil: " + h.modificadores());
            }
        }
        assertTrue(alguma(hordas, h -> h.dificuldade() == Dificuldade.DIFICIL && h.modificadores().size() == 2),
                "o Difícil também tem modificadores");
        assertTrue(alguma(hordas, temTanque), "o tanque aparece fora do Difícil");
    }

    @Test
    void todosOsModificadoresAparecemEOVentoSopraParaOsDoisLados() {
        Set<TipoDeModificador> tipos = new HashSet<>();
        boolean direita = false;
        boolean esquerda = false;
        for (DescricaoDaHorda h : hordas()) {
            for (Modificador m : h.modificadores()) {
                tipos.add(m.tipo());
                if (m.tipo() == TipoDeModificador.VENTO) {
                    direita |= m.valor() > 0;
                    esquerda |= m.valor() < 0;
                }
            }
        }
        assertEquals(Set.of(TipoDeModificador.values()), tipos);
        assertTrue(direita && esquerda, "vento para a direita e para a esquerda");
    }

    @Test
    void roteiroDeUmOuDoisPerigosDistintosOrdenadosNoTempo() {
        Set<TipoDeEvento> vistos = new HashSet<>();
        Set<Long> sementes = new HashSet<>();
        int perigos = 0;
        for (DescricaoDaHorda h : hordas()) {
            List<PerigoDoRoteiro> roteiro = h.roteiro();
            assertTrue(roteiro.size() == 1 || roteiro.size() == 2, "1 ou 2 perigos: " + roteiro);
            Set<TipoDeEvento> tipos = new HashSet<>();
            double anterior = Double.NEGATIVE_INFINITY;
            for (PerigoDoRoteiro p : roteiro) {
                assertTrue(p.tipo().perigo(), "só perigos: " + p);
                assertTrue(tipos.add(p.tipo()), "tipos distintos: " + roteiro);
                assertTrue(p.instante() >= 20 && p.instante() <= 120, "instante " + p.instante());
                assertTrue(p.instante() >= anterior, "ordenado pelo instante: " + roteiro);
                anterior = p.instante();
                vistos.add(p.tipo());
                sementes.add(p.semente());
                perigos++;
            }
        }
        assertEquals(Set.of(TipoDeEvento.CHUVA_DE_METEOROS, TipoDeEvento.TEMPESTADE, TipoDeEvento.PANE_DE_GRAVIDADE),
                vistos);
        assertEquals(perigos, sementes.size(), "cada perigo tem a própria semente");
    }

    @Test
    void dificuldadesPertoDe20Por50Por30() {
        Map<Dificuldade, Integer> contagem = new EnumMap<>(Dificuldade.class);
        for (DescricaoDaHorda h : hordas()) {
            contagem.merge(h.dificuldade(), 1, Integer::sum);
        }
        conferirProporcoes(contagem, SEMENTES.length, 0.08);
    }

    @Test
    void dificuldadesConvergemComMuitasSementes() {
        // 20 000 sementes seguidas: o desvio-padrão do Médio fica em 0,35 ponto; 1,5 ponto são mais de 4 desvios
        int total = 20_000;
        Map<Dificuldade, Integer> contagem = new EnumMap<>(Dificuldade.class);
        for (long semente = 0; semente < total; semente++) {
            contagem.merge(SorteioDaHorda.sortear(1, semente, DUAS, null).dificuldade(), 1, Integer::sum);
        }
        conferirProporcoes(contagem, total, 0.015);
    }

    private static void conferirProporcoes(Map<Dificuldade, Integer> contagem, int total, double tolerancia) {
        assertEquals(0.20, contagem.getOrDefault(Dificuldade.FACIL, 0) / (double) total, tolerancia,
                "Fácil " + contagem);
        assertEquals(0.50, contagem.getOrDefault(Dificuldade.MEDIO, 0) / (double) total, tolerancia,
                "Médio " + contagem);
        assertEquals(0.30, contagem.getOrDefault(Dificuldade.DIFICIL, 0) / (double) total, tolerancia,
                "Difícil " + contagem);
    }

    // ------------------------------------------------------------------ rodízio

    @Test
    void naoRepeteACavernaAnteriorComDuasLiberadas() {
        assertEquals(Set.of("planicie"), cavernas(hordas(DUAS, "marciana")));
        assertEquals(Set.of("marciana"), cavernas(hordas(DUAS, "planicie")));
    }

    @Test
    void repeteQuandoSoHaUmaLiberada() {
        assertEquals(Set.of("marciana"), cavernas(hordas(List.of(MARCIANA), "marciana")));
    }

    @Test
    void umaAnteriorQueNaoEstaNoRodizioNaoAtrapalha() {
        assertEquals(Set.of("marciana", "planicie"), cavernas(hordas(DUAS, "vulcao")));
    }

    @Test
    void ignoraCavernasNaoLiberadas() {
        // só a marciana conta: ela se repete, mesmo sendo a anterior
        assertEquals(Set.of("marciana"), cavernas(hordas(List.of(liberada(PLANICIE, false), MARCIANA), "marciana")));
    }

    @Test
    void ignoraCavernasInvalidas() {
        DescricaoDeCaverna invalida = comPontos(PLANICIE, PLANICIE.pontosDePouso().subList(0, 2));
        assertFalse(ValidadorDeCaverna.problemas(invalida).isEmpty());
        assertEquals(Set.of("marciana"), cavernas(hordas(List.of(invalida, MARCIANA), "marciana")));
    }

    @Test
    void semCavernasEErro() {
        assertThrows(IllegalArgumentException.class, () -> SorteioDaHorda.sortear(1, 1, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> SorteioDaHorda.sortear(1, 1, null, null));
    }

    @Test
    void semNenhumaLiberadaEValidaEErro() {
        DescricaoDeCaverna invalida = comPontos(PLANICIE, List.of());
        assertThrows(IllegalArgumentException.class,
                () -> SorteioDaHorda.sortear(1, 1, List.of(liberada(MARCIANA, false), invalida), null));
    }
}
