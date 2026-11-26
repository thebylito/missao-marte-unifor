package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Sorteia uma horda a partir de uma semente (regras da seção 2.1 da análise da Fase 8). O servidor chama este código
 * uma vez por horda, grava o resultado e manda pronto ao jogo como dados.
 *
 * <p><b>Determinismo.</b> Tudo sai de um único {@link SplittableRandom} criado com a semente: nada de
 * {@code Math.random}, relógio ou iteração de {@code HashMap}. As cavernas candidatas são ordenadas pelo id antes do
 * sorteio, então a ordem da lista recebida não importa. O {@code numero} só é copiado para o resultado.
 *
 * <p><b>Ordem do sorteio</b> (fixa; mudá-la muda todas as hordas, e o teste dourado acusa):
 * <ol>
 *   <li><b>caverna</b>: {@code nextInt(n)} entre as candidatas (liberadas e válidas, ordenadas pelo id), tirando a
 *       de {@code idDaCavernaAnterior} quando sobra outra. Sempre uma chamada, mesmo com uma candidata só;</li>
 *   <li><b>dificuldade</b>: {@code nextDouble()}; abaixo de 0,2 Fácil, abaixo de 0,7 Médio, senão Difícil;</li>
 *   <li><b>passageiros</b>: três {@code nextInt}, um embaralhamento parcial (Fisher-Yates) dos índices de
 *       {@code pontosDePouso}: o primeiro sorteado é o do Astronauta, o segundo o do Engenheiro e o terceiro o do
 *       Professor;</li>
 *   <li><b>velocidade dos asteroides</b>: {@code nextDouble()}, 0,8 + 0,5 × u;</li>
 *   <li><b>fases</b>: um {@code nextDouble()} por asteroide, na ordem da caverna, 2π × u;</li>
 *   <li><b>modificadores</b>: {@code nextInt(3)} dá quantos (0 a 2). Para cada um, {@code nextInt} escolhe o tipo
 *       entre os que sobraram (na ordem do enum, sem {@code TANQUE} no Difícil) e em seguida vem o valor:
 *       vento = {@code nextDouble()} para a força (8 + 12 × u) e {@code nextBoolean()} para o lado (verdadeiro =
 *       direita, +); gravidade = 1,1 + 0,1 × {@code nextDouble()}; tanque = 0,85 + 0,15 × {@code nextDouble()}. A
 *       lista final fica na ordem do enum;</li>
 *   <li><b>roteiro</b>: 1 + {@code nextInt(2)} perigos. Para cada um, {@code nextInt} escolhe o tipo entre os perigos
 *       que sobraram (na ordem do enum), depois {@code nextDouble()} dá o instante (20 + 100 × u segundos) e
 *       {@code nextLong()} a semente do efeito. A lista final fica em ordem de instante.</li>
 * </ol>
 */
public final class SorteioDaHorda {

    private static final double CHANCE_FACIL = 0.2;
    private static final double CHANCE_FACIL_OU_MEDIO = 0.7;
    private static final double VELOCIDADE_MINIMA = 0.8;
    private static final double VELOCIDADE_FAIXA = 0.5;
    private static final int MAXIMO_DE_MODIFICADORES = 2;
    private static final double VENTO_MINIMO = 8;
    private static final double VENTO_FAIXA = 12;
    private static final double GRAVIDADE_MINIMA = 1.1;
    private static final double GRAVIDADE_FAIXA = 0.1;
    private static final double TANQUE_MINIMO = 0.85;
    private static final double TANQUE_FAIXA = 0.15;
    private static final int MAXIMO_DE_PERIGOS = 2;
    private static final double PRIMEIRO_INSTANTE = 20;
    private static final double INSTANTE_FAIXA = 100;
    private static final List<TipoPassageiro> PASSAGEIROS =
            List.of(TipoPassageiro.ASTRONAUTA, TipoPassageiro.ENGENHEIRO, TipoPassageiro.PROFESSOR);

    private SorteioDaHorda() {}

    /**
     * @param numero              o número da horda, só copiado para o resultado
     * @param semente             decide tudo o que é sorteado
     * @param liberadas           as cavernas do rodízio; só contam as {@code liberada()} e válidas pelo
     *                            {@link ValidadorDeCaverna}
     * @param idDaCavernaAnterior a caverna da horda anterior, que não se repete se houver outra; pode ser nulo
     * @throws IllegalArgumentException se a lista for nula ou vazia, ou se nenhuma caverna for liberada e válida
     */
    public static DescricaoDaHorda sortear(long numero, long semente, List<DescricaoDeCaverna> liberadas,
                                           String idDaCavernaAnterior) {
        if (liberadas == null || liberadas.isEmpty()) {
            throw new IllegalArgumentException("nenhuma caverna para sortear a horda");
        }
        List<DescricaoDeCaverna> candidatas = candidatas(liberadas, idDaCavernaAnterior);
        SplittableRandom sorte = new SplittableRandom(semente);

        DescricaoDeCaverna caverna = candidatas.get(sorte.nextInt(candidatas.size()));
        Dificuldade dificuldade = dificuldade(sorte);
        List<PassageiroNaHorda> passageiros = passageiros(sorte, caverna.pontosDePouso().size());
        double velocidade = VELOCIDADE_MINIMA + VELOCIDADE_FAIXA * sorte.nextDouble();
        List<Double> fases = new ArrayList<>();
        for (int i = 0; i < caverna.asteroides().size(); i++) {
            fases.add(2 * Math.PI * sorte.nextDouble());
        }
        List<Modificador> modificadores = modificadores(sorte, dificuldade);
        List<PerigoDoRoteiro> roteiro = roteiro(sorte);
        return new DescricaoDaHorda(numero, dificuldade, caverna, passageiros, velocidade, fases, modificadores,
                roteiro);
    }

    /** As liberadas e válidas, ordenadas pelo id, sem a anterior quando sobra outra. */
    private static List<DescricaoDeCaverna> candidatas(List<DescricaoDeCaverna> liberadas, String anterior) {
        List<DescricaoDeCaverna> validas = new ArrayList<>();
        for (DescricaoDeCaverna c : liberadas) {
            if (c != null && c.liberada() && ValidadorDeCaverna.problemas(c).isEmpty()) {
                validas.add(c);
            }
        }
        if (validas.isEmpty()) {
            throw new IllegalArgumentException("nenhuma caverna liberada e válida para sortear a horda");
        }
        validas.sort(Comparator.comparing(DescricaoDeCaverna::id));
        List<DescricaoDeCaverna> semAAnterior = new ArrayList<>();
        for (DescricaoDeCaverna c : validas) {
            if (!c.id().equals(anterior)) {
                semAAnterior.add(c);
            }
        }
        return semAAnterior.isEmpty() ? validas : semAAnterior;
    }

    private static Dificuldade dificuldade(SplittableRandom sorte) {
        double u = sorte.nextDouble();
        if (u < CHANCE_FACIL) {
            return Dificuldade.FACIL;
        }
        return u < CHANCE_FACIL_OU_MEDIO ? Dificuldade.MEDIO : Dificuldade.DIFICIL;
    }

    /** Embaralhamento parcial dos índices dos pontos: os 3 primeiros são o Astronauta, o Engenheiro e o Professor. */
    private static List<PassageiroNaHorda> passageiros(SplittableRandom sorte, int pontos) {
        int[] indices = new int[pontos];
        for (int i = 0; i < pontos; i++) {
            indices[i] = i;
        }
        List<PassageiroNaHorda> passageiros = new ArrayList<>();
        for (int i = 0; i < PASSAGEIROS.size(); i++) {
            int j = i + sorte.nextInt(pontos - i);
            int trocado = indices[i];
            indices[i] = indices[j];
            indices[j] = trocado;
            passageiros.add(new PassageiroNaHorda(PASSAGEIROS.get(i), indices[i]));
        }
        return passageiros;
    }

    private static List<Modificador> modificadores(SplittableRandom sorte, Dificuldade dificuldade) {
        List<TipoDeModificador> tipos = new ArrayList<>(List.of(TipoDeModificador.values()));
        if (dificuldade == Dificuldade.DIFICIL) {
            tipos.remove(TipoDeModificador.TANQUE);   // a viagem mais longa já gasta perto de 100 (DA-34)
        }
        int quantos = sorte.nextInt(MAXIMO_DE_MODIFICADORES + 1);
        List<Modificador> modificadores = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            TipoDeModificador tipo = tipos.remove(sorte.nextInt(tipos.size()));
            modificadores.add(new Modificador(tipo, valor(sorte, tipo)));
        }
        modificadores.sort(Comparator.comparing(Modificador::tipo));
        return modificadores;
    }

    private static double valor(SplittableRandom sorte, TipoDeModificador tipo) {
        switch (tipo) {
            case VENTO:
                double forca = VENTO_MINIMO + VENTO_FAIXA * sorte.nextDouble();
                return sorte.nextBoolean() ? forca : -forca;
            case GRAVIDADE:
                return GRAVIDADE_MINIMA + GRAVIDADE_FAIXA * sorte.nextDouble();
            default:
                return TANQUE_MINIMO + TANQUE_FAIXA * sorte.nextDouble();
        }
    }

    private static List<PerigoDoRoteiro> roteiro(SplittableRandom sorte) {
        List<TipoDeEvento> perigos = new ArrayList<>();
        for (TipoDeEvento tipo : TipoDeEvento.values()) {
            if (tipo.perigo()) {
                perigos.add(tipo);
            }
        }
        int quantos = 1 + sorte.nextInt(MAXIMO_DE_PERIGOS);
        List<PerigoDoRoteiro> roteiro = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            TipoDeEvento tipo = perigos.remove(sorte.nextInt(perigos.size()));
            double instante = PRIMEIRO_INSTANTE + INSTANTE_FAIXA * sorte.nextDouble();
            roteiro.add(new PerigoDoRoteiro(tipo, instante, sorte.nextLong()));
        }
        roteiro.sort(Comparator.comparingDouble(PerigoDoRoteiro::instante));
        return roteiro;
    }
}
