package br.edu.unifor.missaomarte.pilotagem;

import static br.edu.unifor.missaomarte.pilotagem.FaixaDosAjustes.exigir;
import static br.edu.unifor.missaomarte.pilotagem.FaixaDosAjustes.numero;

import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;

/**
 * Fase 8 (DA-48): a velocidade dos meteoros e a duração de cada evento, que o servidor pode ajustar. O padrão é o
 * da Fase 8: meteoros de 150 a 250 (DA-47) e as durações de {@link TipoDeEvento#duracaoSegundos()}.
 *
 * @param velocidadeMinimaDosMeteoros menor velocidade de um meteoro da chuva (50 a 600)
 * @param velocidadeMaximaDosMeteoros maior velocidade, maior que a mínima (50 a 600)
 * @param duracaoDaChuva              segundos da chuva de meteoros (10 a 90)
 * @param duracaoDaTempestade         segundos da tempestade (10 a 90)
 * @param duracaoDaPane               segundos da pane de gravidade (10 a 90)
 * @param duracaoDoVip                segundos da cápsula VIP na caverna (10 a 90)
 */
public record AjustesDosEventos(double velocidadeMinimaDosMeteoros, double velocidadeMaximaDosMeteoros,
                                double duracaoDaChuva, double duracaoDaTempestade, double duracaoDaPane,
                                double duracaoDoVip) {

    /** A menor e a maior duração aceitas para um evento, em segundos. */
    public static final double DURACAO_MINIMA = 10;
    public static final double DURACAO_MAXIMA = 90;

    public static final AjustesDosEventos PADRAO = new AjustesDosEventos(150, 250,
            TipoDeEvento.CHUVA_DE_METEOROS.duracaoSegundos(), TipoDeEvento.TEMPESTADE.duracaoSegundos(),
            TipoDeEvento.PANE_DE_GRAVIDADE.duracaoSegundos(), TipoDeEvento.PASSAGEIRO_VIP.duracaoSegundos());

    public AjustesDosEventos {
        exigir("velocidadeMinimaDosMeteoros", velocidadeMinimaDosMeteoros, 50, 600);
        exigir("velocidadeMaximaDosMeteoros", velocidadeMaximaDosMeteoros, 50, 600);
        if (velocidadeMinimaDosMeteoros >= velocidadeMaximaDosMeteoros) {
            throw new IllegalArgumentException("velocidadeMinimaDosMeteoros (" + numero(velocidadeMinimaDosMeteoros)
                    + ") precisa ser menor que a máxima (" + numero(velocidadeMaximaDosMeteoros) + ")");
        }
        exigir("duracaoDaChuva", duracaoDaChuva, DURACAO_MINIMA, DURACAO_MAXIMA);
        exigir("duracaoDaTempestade", duracaoDaTempestade, DURACAO_MINIMA, DURACAO_MAXIMA);
        exigir("duracaoDaPane", duracaoDaPane, DURACAO_MINIMA, DURACAO_MAXIMA);
        exigir("duracaoDoVip", duracaoDoVip, DURACAO_MINIMA, DURACAO_MAXIMA);
    }

    /** A duração deste tipo de evento, em segundos. */
    public double duracao(TipoDeEvento tipo) {
        switch (tipo) {
            case CHUVA_DE_METEOROS:
                return duracaoDaChuva;
            case TEMPESTADE:
                return duracaoDaTempestade;
            case PANE_DE_GRAVIDADE:
                return duracaoDaPane;
            default:
                return duracaoDoVip;
        }
    }
}
