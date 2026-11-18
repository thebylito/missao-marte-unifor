package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "Fotografia" do mapa para desenhar: uma lista de células com o conteúdo de cada uma. Lê a missão e não
 * altera nada. Usa a mesma prioridade do console quando duas coisas ocupam a mesma célula:
 * nave, passageiro, asteroide, inimigo, plataforma.
 */
public final class QuadroDaPartida {

    /** Uma célula do mapa e o que mostrar nela. */
    public record Celula(Posicao posicao, Conteudo conteudo) {
    }

    private QuadroDaPartida() {
    }

    public static List<Celula> de(Missao missao) {
        Mapa mapa = missao.getMapa();
        List<Celula> celulas = new ArrayList<>();
        for (int y = mapa.getMin(); y <= mapa.getMax(); y++) {
            for (int x = mapa.getMin(); x <= mapa.getMax(); x++) {
                Posicao p = new Posicao(x, y);
                celulas.add(new Celula(p, conteudo(missao, p)));
            }
        }
        return celulas;
    }

    public static Conteudo conteudo(Missao missao, Posicao celula) {
        if (missao.getNave().estaEm(celula)) {
            return Conteudo.NAVE;
        }
        Optional<Passageiro> passageiro = missao.getPassageiros().stream()
                .filter(p -> p.getPosicao().equals(celula)).findFirst();
        if (passageiro.isPresent()) {
            switch (passageiro.get().getTipo()) {
                case ENGENHEIRO: return Conteudo.ENGENHEIRO;
                case ASTRONAUTA: return Conteudo.ASTRONAUTA;
                default: return Conteudo.PROFESSOR;
            }
        }
        if (missao.getAsteroides().stream().anyMatch(a -> a.getPosicao().equals(celula))) {
            return Conteudo.ASTEROIDE;
        }
        if (missao.getInimigos().stream().anyMatch(i -> i.getPosicao().equals(celula))) {
            return Conteudo.INIMIGO;
        }
        return celula.equals(missao.getMapa().getPlataforma()) ? Conteudo.PLATAFORMA : Conteudo.VAZIO;
    }
}
