package br.edu.unifor.missaomarte.pilotagem.horda;

import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.ConfigDeVoo;
import java.util.List;

/**
 * A horda pronta para virar uma {@link br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem}: a caverna (nova a
 * cada montagem, porque os passageiros mudam de lugar na partida), a configuração com os modificadores, o vento
 * constante e o roteiro dos perigos.
 *
 * @param ventoConstante aceleração lateral constante (+ para a direita); 0 sem vento
 */
public record HordaMontada(Caverna caverna, ConfigDeVoo config, double ventoConstante, List<PerigoDoRoteiro> roteiro) {

    public HordaMontada {
        roteiro = List.copyOf(roteiro);
    }
}
