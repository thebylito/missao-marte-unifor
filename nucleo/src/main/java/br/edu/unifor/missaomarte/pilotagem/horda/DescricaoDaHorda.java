package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import java.util.List;

/**
 * Uma horda pronta, como dado: o que o servidor gera com o {@link SorteioDaHorda}, grava e manda ao jogo, que a
 * transforma numa partida com o {@link MontadorDaHorda}.
 *
 * @param numero                  o número da horda (só identifica; quem decide o sorteio é a semente)
 * @param velocidadeDosAsteroides multiplica a velocidade dos asteroides da dificuldade
 * @param fasesDosAsteroides      uma fase, em radianos, por asteroide da caverna, na mesma ordem
 * @param roteiro                 os perigos, em ordem de instante
 */
public record DescricaoDaHorda(long numero, Dificuldade dificuldade, DescricaoDeCaverna caverna,
                               List<PassageiroNaHorda> passageiros, double velocidadeDosAsteroides,
                               List<Double> fasesDosAsteroides, List<Modificador> modificadores,
                               List<PerigoDoRoteiro> roteiro) {

    public DescricaoDaHorda {
        passageiros = List.copyOf(passageiros);
        fasesDosAsteroides = List.copyOf(fasesDosAsteroides);
        modificadores = List.copyOf(modificadores);
        roteiro = List.copyOf(roteiro);
    }
}
