package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import java.util.List;

/**
 * Uma horda pronta, como dado: o que o servidor gera com o {@link SorteioDaHorda}, grava e manda ao jogo, que a
 * transforma numa partida com o {@link MontadorDaHorda}.
 *
 * @param numero                  o número da horda (só identifica; quem decide o sorteio é a semente)
 * @param velocidadeDosAsteroides multiplica a velocidade dos asteroides da dificuldade
 * @param fasesDosAsteroides      uma fase, em radianos, por asteroide da caverna, na mesma ordem
 * @param roteiro                 os perigos, em ordem de instante
 * @param ajustes                 os ajustes do servidor no momento do sorteio (DA-48); a horda usa estes até o fim,
 *                                mesmo que o servidor mude os dele. Sem ajustes (uma horda gravada antes deles), o
 *                                padrão
 */
public record DescricaoDaHorda(long numero, Dificuldade dificuldade, DescricaoDeCaverna caverna,
                               List<PassageiroNaHorda> passageiros, double velocidadeDosAsteroides,
                               List<Double> fasesDosAsteroides, List<Modificador> modificadores,
                               List<PerigoDoRoteiro> roteiro, AjustesDeVoo ajustes) {

    public DescricaoDaHorda {
        passageiros = List.copyOf(passageiros);
        fasesDosAsteroides = List.copyOf(fasesDosAsteroides);
        modificadores = List.copyOf(modificadores);
        roteiro = List.copyOf(roteiro);
        ajustes = ajustes == null ? AjustesDeVoo.PADRAO : ajustes;
    }

    /** Com os ajustes padrão. */
    public DescricaoDaHorda(long numero, Dificuldade dificuldade, DescricaoDeCaverna caverna,
                            List<PassageiroNaHorda> passageiros, double velocidadeDosAsteroides,
                            List<Double> fasesDosAsteroides, List<Modificador> modificadores,
                            List<PerigoDoRoteiro> roteiro) {
        this(numero, dificuldade, caverna, passageiros, velocidadeDosAsteroides, fasesDosAsteroides, modificadores,
                roteiro, AjustesDeVoo.PADRAO);
    }

    /** A mesma horda com outros ajustes (o servidor grava os dele na horda que acabou de sortear). */
    public DescricaoDaHorda comAjustes(AjustesDeVoo novos) {
        return new DescricaoDaHorda(numero, dificuldade, caverna, passageiros, velocidadeDosAsteroides,
                fasesDosAsteroides, modificadores, roteiro, novos);
    }
}
