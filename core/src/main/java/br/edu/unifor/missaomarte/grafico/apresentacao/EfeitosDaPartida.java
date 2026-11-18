package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos.Impacto;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;
import br.edu.unifor.missaomarte.grafico.recursos.FolhaDeSprites;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Efeitos visuais em andamento: explosões (folha de 16 quadros) e o tremor da tela. Avança com o tempo que a tela
 * informa, então os testes controlam o relógio. Os efeitos só existem porque a Partida avisou uma colisão.
 */
public final class EfeitosDaPartida {

    public static final float QUADROS_POR_SEGUNDO_DA_EXPLOSAO = 20f;
    static final float DURACAO_DA_EXPLOSAO = FolhaDeSprites.duracao(Folha.EXPLOSAO, QUADROS_POR_SEGUNDO_DA_EXPLOSAO);

    /** Explosão visível: em que célula, com que tamanho (em células) e em que quadro da folha. */
    public record ExplosaoVisivel(Posicao celula, float tamanho, int quadro) {
    }

    private static final class Explosao {
        final Posicao celula;
        final float tamanho;
        float tempo;

        Explosao(Posicao celula, float tamanho) {
            this.celula = celula;
            this.tamanho = tamanho;
        }
    }

    private final List<Explosao> explosoes = new ArrayList<>();
    private float tremorRestante;
    private float tremorDuracao = 1;
    private float tremorAmplitude;
    private float relogio;
    private boolean naveDestruida;

    public void aplicar(Impacto impacto, Posicao nave) {
        switch (impacto) {
            case LEVE:
                explosoes.add(new Explosao(nave, 1.3f));
                tremer(0.3f, 5);
                break;
            case FATAL:
                explosoes.add(new Explosao(nave, 2.2f));
                tremer(0.7f, 12);
                naveDestruida = true;
                break;
            default:
                break;
        }
    }

    private void tremer(float duracao, float amplitude) {
        tremorDuracao = duracao;
        tremorRestante = duracao;
        tremorAmplitude = amplitude;
    }

    public void avancar(float delta) {
        relogio += delta;
        tremorRestante = Math.max(0, tremorRestante - delta);
        for (Iterator<Explosao> it = explosoes.iterator(); it.hasNext(); ) {
            Explosao e = it.next();
            e.tempo += delta;
            if (e.tempo >= DURACAO_DA_EXPLOSAO) {
                it.remove();
            }
        }
    }

    /** Enquanto houver efeito, a tela de fim espera (o jogador vê a explosão antes do "GAME OVER"). */
    public boolean emAndamento() {
        return !explosoes.isEmpty() || tremorRestante > 0;
    }

    public List<ExplosaoVisivel> explosoes() {
        List<ExplosaoVisivel> lista = new ArrayList<>();
        for (Explosao e : explosoes) {
            lista.add(new ExplosaoVisivel(e.celula, e.tamanho,
                    FolhaDeSprites.quadroUmaVez(Folha.EXPLOSAO, e.tempo, QUADROS_POR_SEGUNDO_DA_EXPLOSAO)));
        }
        return lista;
    }

    /** Deslocamento horizontal da tela pelo tremor, em pixels; diminui até zero. */
    public float tremorX() {
        float forca = forcaDoTremor();
        return forca == 0 ? 0 : forca * (float) Math.sin(relogio * 55);
    }

    public float tremorY() {
        float forca = forcaDoTremor();
        return forca == 0 ? 0 : forca * (float) Math.cos(relogio * 47);
    }

    private float forcaDoTremor() {
        return tremorRestante <= 0 ? 0 : tremorAmplitude * tremorRestante / tremorDuracao;
    }

    /** Depois do impacto fatal a nave some do mapa (só sobra a explosão). */
    public boolean isNaveDestruida() {
        return naveDestruida;
    }
}
