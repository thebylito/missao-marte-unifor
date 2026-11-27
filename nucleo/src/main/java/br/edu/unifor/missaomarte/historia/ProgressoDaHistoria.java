package br.edu.unifor.missaomarte.historia;

/** Progresso local da primeira operação; uma nova tentativa não desfaz uma conclusão. */
public record ProgressoDaHistoria(boolean m01Concluida, String retomada, int melhorPontuacao) {
    public ProgressoDaHistoria {
        if (retomada == null || !(retomada.isEmpty() || retomada.equals("M01")) || melhorPontuacao < 0
                || (!m01Concluida && melhorPontuacao != 0)) {
            throw new IllegalArgumentException("Progresso da história inválido");
        }
    }
    public static ProgressoDaHistoria novo() { return new ProgressoDaHistoria(false, "", 0); }
    public ProgressoDaHistoria iniciarM01() { return new ProgressoDaHistoria(m01Concluida, "M01", melhorPontuacao); }
    public ProgressoDaHistoria concluirM01(int pontos) {
        if (pontos < 0) throw new IllegalArgumentException("Pontuação negativa");
        return new ProgressoDaHistoria(true, "", Math.max(pontos, melhorPontuacao));
    }
}
