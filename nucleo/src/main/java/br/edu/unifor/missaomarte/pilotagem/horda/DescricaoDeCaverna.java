package br.edu.unifor.missaomarte.pilotagem.horda;

import java.util.List;

/**
 * Uma caverna como dado, para o rodízio da Horda: os limites, as rochas, a base, os pontos de pouso candidatos dos
 * passageiros (a horda sorteia 3 deles) e os asteroides. Só entra no rodízio se {@code liberada} (vencida por um
 * humano no Difícil) e válida pelo {@link ValidadorDeCaverna}.
 *
 * <p>As listas são copiadas e imutáveis; uma lista nula é erro (o arquivo está incompleto).
 */
public record DescricaoDeCaverna(String id, String nome, double largura, double altura, List<RochaDescrita> rochas,
                                 PontoDePouso base, List<PontoDePouso> pontosDePouso,
                                 List<AsteroideDescrito> asteroides, boolean liberada) {

    public DescricaoDeCaverna {
        rochas = List.copyOf(rochas);
        pontosDePouso = List.copyOf(pontosDePouso);
        asteroides = List.copyOf(asteroides);
    }
}
