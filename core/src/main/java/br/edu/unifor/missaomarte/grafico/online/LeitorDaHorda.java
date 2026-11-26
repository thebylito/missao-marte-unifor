package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.pilotagem.horda.AsteroideDescrito;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import br.edu.unifor.missaomarte.pilotagem.horda.Modificador;
import br.edu.unifor.missaomarte.pilotagem.horda.PassageiroNaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.PerigoDoRoteiro;
import br.edu.unifor.missaomarte.pilotagem.horda.PontoDePouso;
import br.edu.unifor.missaomarte.pilotagem.horda.RochaDescrita;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeModificador;
import com.badlogic.gdx.utils.JsonValue;
import static br.edu.unifor.missaomarte.grafico.online.ServidorHttp.lista;

/** Lê os dados do sorteio tal como o servidor os gravou, inclusive as sementes inteiras de 64 bits. */
final class LeitorDaHorda {
    private LeitorDaHorda() {}

    static HordaAtual atual(JsonValue json) {
        DescricaoDaHorda descricao = descricao(json.require("descricao"));
        if (descricao.numero() != json.getLong("numero")) {
            throw new IllegalArgumentException("Número da horda diverge da descrição");
        }
        return new HordaAtual(descricao, json.getLong("segundosParaAcabar"), json.getLong("segundosParaProxima"),
                json.getString("proximaAs"));
    }

    private static DescricaoDaHorda descricao(JsonValue d) {
        return new DescricaoDaHorda(d.getLong("numero"), dificuldade(d.getString("dificuldade")),
                caverna(d.require("caverna")), lista(d.get("passageiros"), p -> new PassageiroNaHorda(
                        TipoPassageiro.valueOf(p.getString("tipo")), p.getInt("ponto"))),
                d.getDouble("velocidadeDosAsteroides"), lista(d.get("fasesDosAsteroides"), JsonValue::asDouble),
                lista(d.get("modificadores"), m -> new Modificador(TipoDeModificador.valueOf(m.getString("tipo")),
                        m.getDouble("valor"))),
                lista(d.get("roteiro"), p -> new PerigoDoRoteiro(TipoDeEvento.valueOf(p.getString("tipo")),
                        p.getDouble("instante"), p.getLong("semente"))),
                LeitorDeAjustes.ouPadrao(d.get("ajustes")));
    }

    private static Dificuldade dificuldade(String texto) {
        for (Dificuldade d : Dificuldade.values()) {
            if (d.toString().equalsIgnoreCase(texto) || d.name().equalsIgnoreCase(texto)) return d;
        }
        throw new IllegalArgumentException("Dificuldade desconhecida: " + texto);
    }

    private static DescricaoDeCaverna caverna(JsonValue c) {
        return new DescricaoDeCaverna(c.getString("id"), c.getString("nome"), c.getDouble("largura"),
                c.getDouble("altura"), lista(c.get("rochas"), r -> new RochaDescrita(r.getDouble("x"),
                        r.getDouble("y"), r.getDouble("largura"), r.getDouble("altura"))),
                ponto(c.require("base")), lista(c.get("pontosDePouso"), LeitorDaHorda::ponto),
                lista(c.get("asteroides"), a -> new AsteroideDescrito(a.getDouble("centroX"),
                        a.getDouble("centroY"), a.getDouble("raio"), a.getDouble("amplitudeX"),
                        a.getDouble("amplitudeY"), a.getDouble("periodo"), a.getDouble("fase"),
                        a.getDouble("giroGrausPorSegundo"))), c.getBoolean("liberada"));
    }

    private static PontoDePouso ponto(JsonValue p) {
        return new PontoDePouso(p.getDouble("x"), p.getDouble("superficie"));
    }
}
