package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.pilotagem.AjustesDaDificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import com.badlogic.gdx.utils.JsonValue;

/**
 * DA-48: lê os ajustes como o servidor os escreve (em /api/ajustes e dentro da horda). Um valor fora da faixa é
 * recusado pelo núcleo, e a leitura falha: o jogo fica com os ajustes que já tinha.
 */
final class LeitorDeAjustes {
    private LeitorDeAjustes() {}

    static AjustesDeVoo ler(JsonValue json) {
        return new AjustesDeVoo(dificuldade(json.require("facil")), dificuldade(json.require("medio")),
                dificuldade(json.require("dificil")), eventos(json.require("eventos")));
    }

    /** Uma horda gravada antes dos ajustes não tem o campo: vale o padrão. */
    static AjustesDeVoo ouPadrao(JsonValue json) {
        return json == null || json.isNull() ? AjustesDeVoo.PADRAO : ler(json);
    }

    private static AjustesDaDificuldade dificuldade(JsonValue d) {
        return new AjustesDaDificuldade(numero(d, "tanque"), numero(d, "quedaMaxima"), numero(d, "deslizeMaximo"),
                numero(d, "inclinacaoMaxima"), numero(d, "velocidadeDosAsteroides"));
    }

    private static AjustesDosEventos eventos(JsonValue e) {
        return new AjustesDosEventos(numero(e, "velocidadeMinimaDosMeteoros"),
                numero(e, "velocidadeMaximaDosMeteoros"), numero(e, "duracaoDaChuva"),
                numero(e, "duracaoDaTempestade"), numero(e, "duracaoDaPane"), numero(e, "duracaoDoVip"));
    }

    private static double numero(JsonValue objeto, String nome) {
        return objeto.require(nome).asDouble();
    }
}
