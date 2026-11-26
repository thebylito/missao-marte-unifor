package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import java.util.Objects;

/**
 * Os números do Modo Pilotagem (Fase 7). Unidades: unidades do mundo (a caverna mede 3840 x 1440), segundos e graus.
 * O Médio usa os valores da POC validada pelo usuário em 2026-11-04. A dificuldade muda os limites de pouso, a
 * velocidade dos asteroides e, no Fácil, o tanque: a gravidade, o empuxo e o giro ficam iguais, para o controle ter a
 * mesma sensação em todas. O Difícil não tem tanque menor: a viagem mais longa (o Professor, ida e volta) já gasta
 * perto de 100 mesmo pilotada com cuidado, e com menos combustível ela ficaria impossível (medido na Fase 7).
 *
 * <p>Fase 8 (DA-48): os números de cada dificuldade ficam em {@link AjustesDeVoo#PADRAO}, e o servidor pode
 * trocá-los por variável de ambiente ({@link #para(Dificuldade, AjustesDeVoo)}).
 *
 * @param gravidade                 aceleração para baixo
 * @param empuxo                    aceleração do motor, na direção do bico
 * @param giroGrausPorSegundo       velocidade de giro
 * @param arrasto                   fração da velocidade perdida por segundo (atmosfera rarefeita)
 * @param tanque                    combustível máximo
 * @param consumoPorSegundo         combustível gasto por segundo de motor ligado
 * @param reabastecimentoPorSegundo combustível recebido por segundo pousado na base
 * @param quedaMaxima               maior velocidade de queda num pouso seguro (inclusive)
 * @param deslizeMaximo             maior velocidade lateral num pouso seguro (inclusive)
 * @param inclinacaoMaxima          maior inclinação, em graus, num pouso seguro (inclusive)
 * @param raioDoCasco               raio do círculo de colisão da nave
 * @param peX                       distância lateral de cada pé até o centro da nave
 * @param peY                       altura dos pés em relação ao centro (negativa: abaixo do centro)
 * @param tempoDeEmbarque           segundos pousado até o passageiro embarcar
 * @param capacidade                passageiros a bordo ao mesmo tempo
 * @param vidas                     naves disponíveis
 * @param tempoParaRenascer         segundos entre a explosão e a nave nova na base
 * @param velocidadeDosAsteroides   multiplica a velocidade de todos os asteroides
 * @param bonusMaximo               bônus de combustível na vitória, com o tanque cheio
 * @param eventos                   velocidade dos meteoros e duração dos eventos (Fase 8, DA-48)
 */
public record ConfigDeVoo(
        double gravidade,
        double empuxo,
        double giroGrausPorSegundo,
        double arrasto,
        double tanque,
        double consumoPorSegundo,
        double reabastecimentoPorSegundo,
        double quedaMaxima,
        double deslizeMaximo,
        double inclinacaoMaxima,
        double raioDoCasco,
        double peX,
        double peY,
        double tempoDeEmbarque,
        int capacidade,
        int vidas,
        double tempoParaRenascer,
        double velocidadeDosAsteroides,
        int bonusMaximo,
        AjustesDosEventos eventos) {

    public ConfigDeVoo {
        Objects.requireNonNull(eventos, "eventos");
    }

    /** Sem os ajustes dos eventos: os da Fase 8 ({@link AjustesDosEventos#PADRAO}). */
    public ConfigDeVoo(double gravidade, double empuxo, double giroGrausPorSegundo, double arrasto, double tanque,
                       double consumoPorSegundo, double reabastecimentoPorSegundo, double quedaMaxima,
                       double deslizeMaximo, double inclinacaoMaxima, double raioDoCasco, double peX, double peY,
                       double tempoDeEmbarque, int capacidade, int vidas, double tempoParaRenascer,
                       double velocidadeDosAsteroides, int bonusMaximo) {
        this(gravidade, empuxo, giroGrausPorSegundo, arrasto, tanque, consumoPorSegundo, reabastecimentoPorSegundo,
                quedaMaxima, deslizeMaximo, inclinacaoMaxima, raioDoCasco, peX, peY, tempoDeEmbarque, capacidade,
                vidas, tempoParaRenascer, velocidadeDosAsteroides, bonusMaximo, AjustesDosEventos.PADRAO);
    }

    /** A configuração de cada dificuldade com os números padrão da Fase 8 ({@link AjustesDeVoo#PADRAO}). */
    public static ConfigDeVoo para(Dificuldade dificuldade) {
        return para(dificuldade, AjustesDeVoo.PADRAO);
    }

    /**
     * Fase 8 (DA-48): a configuração da dificuldade com os ajustes do servidor. Os ajustes mudam o tanque, os limites
     * de pouso, a velocidade dos asteroides e os eventos; o resto é o do {@link #medio()}.
     */
    public static ConfigDeVoo para(Dificuldade dificuldade, AjustesDeVoo ajustes) {
        AjustesDaDificuldade a = ajustes.para(dificuldade);
        return medio().com(a.tanque(), a.quedaMaxima(), a.deslizeMaximo(), a.inclinacaoMaxima(),
                a.velocidadeDosAsteroides(), ajustes.eventos());
    }

    /** Os valores da POC. */
    public static ConfigDeVoo medio() {
        return new ConfigDeVoo(
                55, 145, 200, 0.06,     // gravidade, empuxo, giro, arrasto
                100, 3, 35,             // tanque, consumo (cerca de 33 s de motor), reabastecimento
                75, 50, 20,             // limites do pouso seguro (inclinação: 14° até a Fase 7; 20° desde a Fase 8)
                30, 24, -40,            // casco e pés
                0.6, 2, 3, 2.0,         // embarque, capacidade (obriga duas viagens), vidas, renascer
                1.0, 30);               // asteroides, bônus máximo
    }

    private ConfigDeVoo com(double novoTanque, double queda, double deslize, double inclinacao, double asteroides,
                            AjustesDosEventos novosEventos) {
        return new ConfigDeVoo(gravidade, empuxo, giroGrausPorSegundo, arrasto, novoTanque, consumoPorSegundo,
                reabastecimentoPorSegundo, queda, deslize, inclinacao, raioDoCasco, peX, peY, tempoDeEmbarque,
                capacidade, vidas, tempoParaRenascer, asteroides, bonusMaximo, novosEventos);
    }
}
