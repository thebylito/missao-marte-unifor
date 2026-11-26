package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.pilotagem.AsteroideEmOrbita;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.ConfigDeVoo;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Transforma a horda descrita (os dados do servidor) no que a
 * {@link br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem} usa.
 *
 * <p>A {@link Caverna} sai da API pública dela, nesta ordem: as rochas da descrição na ordem dada; a base; os
 * passageiros na ordem Astronauta, Engenheiro e Professor (qualquer que seja a ordem da lista da horda), com os nomes
 * do {@code Caverna.marciana()}; e os asteroides, com a fase sorteada no lugar da fase da descrição. Assim, a horda
 * da caverna marciana com as fases e os pontos originais monta exatamente o {@code Caverna.marciana()}.
 *
 * <p>A {@link ConfigDeVoo} é a da dificuldade com os ajustes gravados na horda, com a velocidade dos asteroides multiplicada pela da horda, a
 * gravidade pelo fator {@code GRAVIDADE} e o tanque pelo fator {@code TANQUE} (quando há). O vento não está na
 * configuração: vai em {@link HordaMontada#ventoConstante()}.
 */
public final class MontadorDaHorda {

    private static final List<TipoPassageiro> ORDEM_DOS_PASSAGEIROS =
            List.of(TipoPassageiro.ASTRONAUTA, TipoPassageiro.ENGENHEIRO, TipoPassageiro.PROFESSOR);

    private MontadorDaHorda() {}

    /**
     * @throws IllegalArgumentException se a horda não tiver exatamente um passageiro de cada tipo, apontar para um
     *                                  ponto que não existe, repetir um modificador ou não tiver uma fase por
     *                                  asteroide
     */
    public static HordaMontada montar(DescricaoDaHorda horda) {
        return new HordaMontada(caverna(horda), config(horda), valor(horda, TipoDeModificador.VENTO, 0),
                horda.roteiro());
    }

    private static Caverna caverna(DescricaoDaHorda horda) {
        DescricaoDeCaverna d = horda.caverna();
        if (horda.fasesDosAsteroides().size() != d.asteroides().size()) {
            throw new IllegalArgumentException("a horda tem " + horda.fasesDosAsteroides().size()
                    + " fases para " + d.asteroides().size() + " asteroides");
        }
        if (horda.passageiros().size() != ORDEM_DOS_PASSAGEIROS.size()) {
            throw new IllegalArgumentException("a horda precisa de 3 passageiros (tem " + horda.passageiros().size()
                    + ")");
        }
        Caverna c = new Caverna(d.largura(), d.altura());
        for (RochaDescrita r : d.rochas()) {
            c.rocha(r.x(), r.y(), r.largura(), r.altura());
        }
        c.base(d.base().x(), d.base().superficie());
        for (TipoPassageiro tipo : ORDEM_DOS_PASSAGEIROS) {
            PontoDePouso ponto = d.pontosDePouso().get(ponto(horda, tipo));
            c.passageiro(passageiro(tipo), ponto.x(), ponto.superficie());
        }
        for (int i = 0; i < d.asteroides().size(); i++) {
            AsteroideDescrito a = d.asteroides().get(i);
            c.asteroide(new AsteroideEmOrbita(a.centroX(), a.centroY(), a.raio(), a.amplitudeX(), a.amplitudeY(),
                    a.periodo(), horda.fasesDosAsteroides().get(i), a.giroGrausPorSegundo()));
        }
        return c;
    }

    /** O índice do ponto de pouso do único passageiro deste tipo. */
    private static int ponto(DescricaoDaHorda horda, TipoPassageiro tipo) {
        PassageiroNaHorda achado = null;
        for (PassageiroNaHorda p : horda.passageiros()) {
            if (p.tipo() == tipo) {
                if (achado != null) {
                    throw new IllegalArgumentException("a horda tem mais de um passageiro do tipo " + tipo.getNome());
                }
                achado = p;
            }
        }
        if (achado == null) {
            throw new IllegalArgumentException("a horda não tem passageiro do tipo " + tipo.getNome());
        }
        int pontos = horda.caverna().pontosDePouso().size();
        if (achado.ponto() < 0 || achado.ponto() >= pontos) {
            throw new IllegalArgumentException("o " + tipo.getNome() + " está no ponto " + achado.ponto()
                    + ", mas a caverna tem " + pontos + " pontos de pouso");
        }
        return achado.ponto();
    }

    /** Os passageiros de sempre, com os nomes do {@code Caverna.marciana()} (e do jogo clássico). */
    private static Passageiro passageiro(TipoPassageiro tipo) {
        switch (tipo) {
            case ASTRONAUTA:
                return new Astronauta("Ast. Maria", Posicao.ORIGEM);
            case ENGENHEIRO:
                return new Engenheiro("Eng. Rosa", Posicao.ORIGEM);
            default:
                return new Professor("Dr. Silva", Posicao.ORIGEM);
        }
    }

    /**
     * A configuração da dificuldade com os ajustes gravados na horda (DA-48) e os fatores dela por cima; o record não
     * tem "with", então vai pelo construtor.
     */
    private static ConfigDeVoo config(DescricaoDaHorda horda) {
        ConfigDeVoo b = ConfigDeVoo.para(horda.dificuldade(), horda.ajustes());
        return new ConfigDeVoo(
                b.gravidade() * valor(horda, TipoDeModificador.GRAVIDADE, 1),
                b.empuxo(),
                b.giroGrausPorSegundo(),
                b.arrasto(),
                b.tanque() * valor(horda, TipoDeModificador.TANQUE, 1),
                b.consumoPorSegundo(),
                b.reabastecimentoPorSegundo(),
                b.quedaMaxima(),
                b.deslizeMaximo(),
                b.inclinacaoMaxima(),
                b.raioDoCasco(),
                b.peX(),
                b.peY(),
                b.tempoDeEmbarque(),
                b.capacidade(),
                b.vidas(),
                b.tempoParaRenascer(),
                b.velocidadeDosAsteroides() * horda.velocidadeDosAsteroides(),
                b.bonusMaximo(),
                b.eventos());
    }

    /** O valor do modificador deste tipo, ou o padrão quando a horda não tem esse modificador. */
    private static double valor(DescricaoDaHorda horda, TipoDeModificador tipo, double padrao) {
        Set<TipoDeModificador> vistos = EnumSet.noneOf(TipoDeModificador.class);
        double valor = padrao;
        for (Modificador m : horda.modificadores()) {
            if (!vistos.add(m.tipo())) {
                throw new IllegalArgumentException("a horda repete o modificador " + m.tipo());
            }
            if (m.tipo() == tipo) {
                valor = m.valor();
            }
        }
        return valor;
    }
}
