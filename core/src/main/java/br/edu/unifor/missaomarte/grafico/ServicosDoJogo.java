package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.RepositorioRanking;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import java.util.List;

/**
 * As peças do núcleo que o cliente gráfico usa, montadas pelo launcher de cada plataforma (raiz de
 * composição). O core não escolhe onde o ranking é gravado nem como o sorteio é feito. Na Fase 5 entra também o
 * servidor de ranking (o launcher decide o endereço). Na Fase 7, o ranking local da pilotagem, num arquivo próprio
 * (o mesmo serviço, outro repositório: a pilotagem não mistura pontos com o clássico).
 */
public record ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio,
                             ServidorDoJogo servidor, ServicoRanking rankingDaPilotagem) {

    /** Sem servidor (testes e fases anteriores): só o ranking local. */
    public ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio) {
        this(gerador, ranking, relogio, ServidorDoJogo.DESLIGADO);
    }

    /** Fases 5 e 6: o ranking da pilotagem fica só na memória. */
    public ServicosDoJogo(GeradorDeMissao gerador, ServicoRanking ranking, Relogio relogio, ServidorDoJogo servidor) {
        this(gerador, ranking, relogio, servidor, new ServicoRanking(new SoNaMemoria(), relogio));
    }

    /** Um ranking que não é gravado em lugar nenhum. */
    private static final class SoNaMemoria implements RepositorioRanking {
        private List<RegistroRanking> registros = List.of();

        @Override
        public List<RegistroRanking> carregar() {
            return registros;
        }

        @Override
        public void salvar(List<RegistroRanking> novos) {
            registros = List.copyOf(novos);
        }

        @Override
        public void apagar() {
            registros = List.of();
        }
    }

    /** A mesma criação de partida do console. */
    public Partida novaPartida(String piloto, Dificuldade dificuldade, int tamanho) {
        return new Partida(piloto, dificuldade, gerador.gerar(new Mapa(tamanho), dificuldade), relogio);
    }
}
