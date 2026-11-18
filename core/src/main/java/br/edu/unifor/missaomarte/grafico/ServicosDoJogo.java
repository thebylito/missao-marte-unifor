package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;

/**
 * As peças do núcleo que o cliente gráfico usa, montadas pelo launcher de cada plataforma (raiz de
 * composição). O core não escolhe onde o ranking é gravado nem como o sorteio é feito.
 */
public record ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio) {

    /** A mesma criação de partida do console. */
    public Partida novaPartida(String piloto, Dificuldade dificuldade, int tamanho) {
        return new Partida(piloto, dificuldade, gerador.gerar(new Mapa(tamanho), dificuldade), relogio);
    }
}
