package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;

/**
 * As peças do núcleo que o cliente gráfico usa, montadas pelo launcher de cada plataforma (raiz de
 * composição). O core não escolhe onde o ranking é gravado nem como o sorteio é feito. Na Fase 5 entra também o
 * servidor de ranking (o launcher decide o endereço).
 */
public record ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio,
                             ServidorDoJogo servidor) {

    /** Sem servidor (testes e fases anteriores): só o ranking local. */
    public ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio) {
        this(gerador, ranking, relogio, ServidorDoJogo.DESLIGADO);
    }

    /** A mesma criação de partida do console. */
    public Partida novaPartida(String piloto, Dificuldade dificuldade, int tamanho) {
        return new Partida(piloto, dificuldade, gerador.gerar(new Mapa(tamanho), dificuldade), relogio);
    }
}
