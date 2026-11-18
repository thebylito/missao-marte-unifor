package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Como a apresentação reage a um turno (padrão Observer: os eventos da Partida disparam som e efeito). Só traduz
 * eventos em retorno visual e sonoro. Quem decide se houve colisão, embarque ou fim é a Partida: esta classe nunca
 * olha posições nem conta vidas, então nenhuma regra é duplicada na tela.
 */
public final class ReacaoAosEventos {

    /** Efeito visual de uma colisão: LEVE quando a nave perde uma vida, FATAL quando é destruída. */
    public enum Impacto { NENHUM, LEVE, FATAL }

    /** Sons a tocar, na ordem, e o impacto a mostrar. */
    public record Reacao(List<Som> sons, Impacto impacto) {
        public Reacao {
            sons = List.copyOf(sons);
        }
    }

    private ReacaoAosEventos() {
    }

    public static Reacao para(Comando comando, List<Evento> eventos) {
        Set<Som> sons = new LinkedHashSet<>();
        Impacto impacto = Impacto.NENHUM;
        if (comando.getDirecao().isPresent()) {
            sons.add(Som.MOVER);       // todo movimento gasta o turno, mesmo encostado na borda
        }
        for (Evento e : eventos) {
            switch (e.tipo()) {
                case PASSAGEIRO_EMBARCADO:
                    sons.add(Som.EMBARQUE);
                    break;
                case SEM_PASSAGEIRO_NA_POSICAO:
                case NAVE_CHEIA:
                case COMANDO_INVALIDO:
                    sons.add(Som.INVALIDO);
                    break;
                case COLISAO:
                    sons.add(Som.COLISAO);
                    if (impacto == Impacto.NENHUM) {
                        impacto = Impacto.LEVE;
                    }
                    break;
                case NAVE_DESTRUIDA:
                    sons.add(Som.EXPLOSAO);
                    impacto = Impacto.FATAL;
                    break;
                case RETORNE_A_PLATAFORMA:
                    sons.add(Som.ALERTA);
                    break;
                default:
                    // VITORIA, PONTOS_ESGOTADOS e ABORTADA: o som vem com a tela de fim (somDoFim)
                    break;
            }
        }
        return new Reacao(List.copyOf(sons), impacto);
    }

    /** Som da tela de fim de partida. A missão abortada termina em silêncio. */
    public static Optional<Som> somDoFim(Partida.Estado estado) {
        switch (estado) {
            case VITORIA: return Optional.of(Som.VITORIA);
            case DERROTA: return Optional.of(Som.DERROTA);
            default: return Optional.empty();
        }
    }
}
