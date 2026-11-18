package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.util.ArrayList;
import java.util.List;

/**
 * Os números do HUD (painel ao lado do mapa), lidos da Partida a cada quadro. Só lê: não guarda nada nem calcula
 * regra. A tela transforma isto em ícones, barras e textos.
 *
 * @param combustivel pontos atuais em relação aos pontos iniciais da dificuldade, de 0 a 1 (cada movimento gasta 1
 *                    ponto, e com zero a missão é perdida; embarcar passageiros pode passar dos pontos iniciais)
 */
public record Hud(String piloto, String dificuldade, int pontos, float combustivel, int vidas, int vidasIniciais,
                  List<TipoPassageiro> aBordo, int capacidade, int restantes, int movimentos) {

    /** Cor da mensagem de um evento. */
    public enum Tom { SUCESSO, ALERTA, PERIGO, NEUTRO }

    public Hud {
        aBordo = List.copyOf(aBordo);
    }

    public static Hud de(Partida partida) {
        Nave nave = partida.getMissao().getNave();
        List<TipoPassageiro> aBordo = new ArrayList<>();
        for (Passageiro p : nave.getPassageiros()) {
            aBordo.add(p.getTipo());
        }
        int iniciais = partida.getDificuldade().getPontosIniciais();
        float combustivel = Math.max(0f, Math.min(1f, partida.getPontos() / (float) iniciais));
        return new Hud(partida.getPiloto(), partida.getDificuldade().toString(), partida.getPontos(), combustivel,
                nave.getVidas(), Nave.VIDAS_INICIAIS, aBordo, nave.getCapacidade(),
                partida.getMissao().getPassageiros().size(), partida.getMovimentos());
    }

    public static Tom tom(Evento evento) {
        switch (evento.tipo()) {
            case PASSAGEIRO_EMBARCADO:
            case VITORIA:
                return Tom.SUCESSO;
            case RETORNE_A_PLATAFORMA:
            case NAVE_CHEIA:
            case SEM_PASSAGEIRO_NA_POSICAO:
            case COMANDO_INVALIDO:
                return Tom.ALERTA;
            case COLISAO:
            case NAVE_DESTRUIDA:
            case PONTOS_ESGOTADOS:
                return Tom.PERIGO;
            default:
                return Tom.NEUTRO;
        }
    }
}
