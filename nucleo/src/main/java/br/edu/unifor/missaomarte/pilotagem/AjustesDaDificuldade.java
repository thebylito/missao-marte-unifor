package br.edu.unifor.missaomarte.pilotagem;

import static br.edu.unifor.missaomarte.pilotagem.FaixaDosAjustes.exigir;

/**
 * Fase 8 (DA-48): os números de uma dificuldade que o servidor pode ajustar. Os outros números da
 * {@link ConfigDeVoo} (gravidade, empuxo, giro, casco) ficam fixos, para o controle ter a mesma sensação sempre.
 *
 * @param tanque                  combustível máximo (50 a 300)
 * @param quedaMaxima             maior velocidade de queda num pouso seguro (20 a 200)
 * @param deslizeMaximo           maior velocidade lateral num pouso seguro (10 a 150)
 * @param inclinacaoMaxima        maior inclinação, em graus, num pouso seguro (5 a 45)
 * @param velocidadeDosAsteroides multiplica a velocidade dos asteroides (0,25 a 3)
 */
public record AjustesDaDificuldade(double tanque, double quedaMaxima, double deslizeMaximo, double inclinacaoMaxima,
                                   double velocidadeDosAsteroides) {

    public AjustesDaDificuldade {
        exigir("tanque", tanque, 50, 300);
        exigir("quedaMaxima", quedaMaxima, 20, 200);
        exigir("deslizeMaximo", deslizeMaximo, 10, 150);
        exigir("inclinacaoMaxima", inclinacaoMaxima, 5, 45);
        exigir("velocidadeDosAsteroides", velocidadeDosAsteroides, 0.25, 3);
    }
}
