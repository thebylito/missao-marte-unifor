package br.edu.unifor.missaomarte.grafico.online;

import java.util.List;

public record RankingSemanal(String semana, List<Posicao> posicoes) {
    public RankingSemanal {
        posicoes = List.copyOf(posicoes);
    }

    public record Posicao(int posicao, String nome, int pontos, int hordas, int eventos) {}
}
