package br.edu.unifor.missaomarte.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Regras do Top 5: quem entra, a ordem e o recorde. Imutável: adicionar devolve um novo ranking. */
public final class Ranking {

    public static final int TAMANHO = 5;

    private static final Comparator<RegistroRanking> MAIOR_PONTUACAO_PRIMEIRO =
            Comparator.comparingInt(RegistroRanking::pontos).reversed();

    private final List<RegistroRanking> registros;

    public Ranking(List<RegistroRanking> registros) {
        this.registros = List.copyOf(registros);
    }

    public static Ranking vazio() {
        return new Ranking(List.of());
    }

    public List<RegistroRanking> getRegistros() {
        return registros;
    }

    public boolean estaVazio() {
        return registros.isEmpty();
    }

    /** O primeiro colocado, se houver. */
    public Optional<RegistroRanking> getLider() {
        return registros.stream().findFirst();
    }

    /** Entra se ainda houver vaga ou se superar o último colocado. */
    public boolean entraNoTop(int pontos) {
        if (registros.size() < TAMANHO) {
            return true;
        }
        return pontos > registros.get(registros.size() - 1).pontos();
    }

    /** Novo ranking com o registro incluído, ordenado (empates mantêm a ordem de chegada) e limitado ao Top 5. */
    public Ranking com(RegistroRanking novo) {
        List<RegistroRanking> todos = new ArrayList<>(registros);
        todos.add(novo);
        todos.sort(MAIOR_PONTUACAO_PRIMEIRO);
        return new Ranking(todos.subList(0, Math.min(TAMANHO, todos.size())));
    }

    /** Ordena registros vindos de fora (por exemplo, de um arquivo) pela maior pontuação. */
    public static List<RegistroRanking> ordenar(List<RegistroRanking> registros) {
        List<RegistroRanking> copia = new ArrayList<>(registros);
        copia.sort(MAIOR_PONTUACAO_PRIMEIRO);
        return Collections.unmodifiableList(copia);
    }
}
