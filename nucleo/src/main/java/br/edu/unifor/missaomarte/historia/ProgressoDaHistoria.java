package br.edu.unifor.missaomarte.historia;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;

/** Progresso imutável do capítulo. Repetir uma missão nunca reduz os resultados anteriores. */
public record ProgressoDaHistoria(Map<String,Integer> resultados, String retomada) {
    public static final List<String> MISSOES = List.of("M01", "M02", "M03");
    public ProgressoDaHistoria {
        if (resultados == null || retomada == null) throw new IllegalArgumentException("Progresso ausente");
        var copia = new LinkedHashMap<String,Integer>();
        boolean lacuna = false;
        for (String id : MISSOES) {
            Integer pontos = resultados.get(id);
            if (pontos == null) lacuna = true;
            else {
                if (lacuna || pontos < 0) throw new IllegalArgumentException("Sequência inválida");
                copia.put(id,pontos);
            }
        }
        if (copia.size()!=resultados.size()) throw new IllegalArgumentException("Missão desconhecida");
        if (!retomada.isEmpty() && (!MISSOES.contains(retomada)
                || (MISSOES.indexOf(retomada)>0 && !copia.containsKey(MISSOES.get(MISSOES.indexOf(retomada)-1)))))
            throw new IllegalArgumentException("Retomada bloqueada");
        resultados = Collections.unmodifiableMap(copia);
    }
    public ProgressoDaHistoria(boolean m01, String retomada, int melhor) {
        this(legado(m01,melhor),retomada);
    }
    private static Map<String,Integer> legado(boolean m01,int melhor) {
        if (melhor<0 || (!m01 && melhor!=0)) throw new IllegalArgumentException("Pontuação inválida");
        return m01 ? Map.of("M01",melhor) : Map.of();
    }
    public static ProgressoDaHistoria novo() { return new ProgressoDaHistoria(Map.of(), ""); }
    public boolean concluida(String id) { return resultados.containsKey(id); }
    public boolean disponivel(String id) {
        int indice = MISSOES.indexOf(id);
        return indice>=0 && (indice==0 || concluida(MISSOES.get(indice-1)));
    }
    public int melhorPontuacao(String id) { return resultados.getOrDefault(id,0); }
    public boolean capituloConcluido() { return concluida("M03"); }
    public String proximaMissao() {
        for (String id:MISSOES) if(!concluida(id)) return id;
        return "M03";
    }
    public ProgressoDaHistoria iniciar(String id) {
        if(!disponivel(id)) throw new IllegalStateException("Missão bloqueada");
        return new ProgressoDaHistoria(resultados,id);
    }
    public ProgressoDaHistoria concluir(String id,int pontos) {
        if(!disponivel(id)) throw new IllegalStateException("Missão bloqueada");
        if(pontos<0) throw new IllegalArgumentException("Pontuação negativa");
        var novos=new LinkedHashMap<>(resultados);
        novos.put(id,Math.max(pontos,melhorPontuacao(id)));
        return new ProgressoDaHistoria(novos, "");
    }
    public boolean m01Concluida() { return concluida("M01"); }
    public int melhorPontuacao() { return melhorPontuacao("M01"); }
    public ProgressoDaHistoria iniciarM01() { return iniciar("M01"); }
    public ProgressoDaHistoria concluirM01(int pontos) { return concluir("M01",pontos); }
}
