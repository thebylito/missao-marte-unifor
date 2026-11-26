package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;

/** Desafio publicado e tempo restante, contado a partir do recebimento da resposta. */
public record HordaAtual(DescricaoDaHorda descricao, long segundosParaAcabar, long segundosParaProxima,
                         String proximaAs) {
    public long numero() {
        return descricao.numero();
    }
}
