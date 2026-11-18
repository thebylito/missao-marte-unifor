package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import java.util.ArrayList;
import java.util.List;

/**
 * Movimento suave entre dois turnos, só na apresentação. A {@code Partida} resolve o turno de uma vez (a nave já
 * está na célula nova); esta classe guarda a foto de antes e a de depois e calcula, a cada quadro, uma posição
 * intermediária em coordenadas de célula (frações). Nenhuma regra é decidida aqui.
 */
public final class AnimacaoDoTurno {

    /** Duração do deslize de uma célula para a outra, em segundos. */
    public static final float DURACAO = 0.15f;

    /** Posição em células, com frações (x e y no sentido do domínio). */
    public record Ponto(float x, float y) {
    }

    /** Foto do que se move ou some num turno: nave, inimigos e passageiros ainda no mapa. */
    public record Instantaneo(Posicao nave, List<Posicao> inimigos, List<Passageiro> passageiros) {
        public Instantaneo {
            inimigos = List.copyOf(inimigos);
            passageiros = List.copyOf(passageiros);
        }

        public static Instantaneo de(Missao missao) {
            List<Posicao> inimigos = new ArrayList<>();
            for (Inimigo i : missao.getInimigos()) {
                inimigos.add(i.getPosicao());
            }
            return new Instantaneo(missao.getNave().getPosicao(), inimigos, missao.getPassageiros());
        }
    }

    /** Passageiro que acabou de embarcar: a tela o leva até a nave enquanto o deslize acontece. */
    public record Embarque(Passageiro passageiro, float progresso) {
    }

    private Instantaneo antes;
    private Instantaneo depois;
    private float tempo = DURACAO;

    public AnimacaoDoTurno(Instantaneo inicial) {
        this.antes = inicial;
        this.depois = inicial;
    }

    /**
     * Começa a animação de um turno. Se outra estava no meio, ela é abandonada (nenhuma tecla espera a animação).
     * Se nada mudou (missão abortada, embarque sem passageiro com inimigos parados), não há o que animar.
     */
    public void iniciar(Instantaneo antes, Instantaneo depois) {
        this.antes = antes;
        this.depois = depois;
        this.tempo = antes.equals(depois) ? DURACAO : 0;
    }

    public void avancar(float delta) {
        tempo = Math.min(DURACAO, tempo + delta);
    }

    public boolean emAndamento() {
        return tempo < DURACAO;
    }

    /** De 0 a 1, com aceleração e frenagem suaves (curva "smoothstep"). */
    public float progresso() {
        float t = tempo / DURACAO;
        return t * t * (3 - 2 * t);
    }

    public Ponto nave() {
        return entre(antes.nave(), depois.nave());
    }

    public List<Ponto> inimigos() {
        List<Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < depois.inimigos().size(); i++) {
            Posicao destino = depois.inimigos().get(i);
            Posicao origem = i < antes.inimigos().size() ? antes.inimigos().get(i) : destino;
            pontos.add(entre(origem, destino));
        }
        return pontos;
    }

    /** Passageiros que estão no mapa depois do turno (os que embarcaram saem desta lista). */
    public List<Passageiro> passageirosNoMapa() {
        return depois.passageiros();
    }

    /** Passageiros que estavam no mapa antes do turno e não estão mais, enquanto a animação dura. */
    public List<Embarque> embarques() {
        List<Embarque> lista = new ArrayList<>();
        if (!emAndamento()) {
            return lista;
        }
        for (Passageiro p : antes.passageiros()) {
            if (depois.passageiros().stream().noneMatch(q -> q == p)) {
                lista.add(new Embarque(p, progresso()));
            }
        }
        return lista;
    }

    private Ponto entre(Posicao de, Posicao para) {
        float t = progresso();
        return new Ponto(de.x() + (para.x() - de.x()) * t, de.y() + (para.y() - de.y()) * t);
    }
}
