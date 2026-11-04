package br.edu.unifor.missaomarte.aplicacao;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.teste.Teste;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** O serviço depende só da interface RepositorioRanking, então o teste usa um repositório em memória. */
class ServicoRankingTeste {

    static final class RepositorioEmMemoria implements RepositorioRanking {
        List<RegistroRanking> salvos = new ArrayList<>();
        boolean falharAoSalvar;
        boolean apagado;

        @Override
        public List<RegistroRanking> carregar() {
            return Ranking.ordenar(salvos);
        }

        @Override
        public void salvar(List<RegistroRanking> registros) throws IOException {
            if (falharAoSalvar) {
                throw new IOException("disco cheio");
            }
            salvos = new ArrayList<>(registros);
        }

        @Override
        public void apagar() {
            apagado = true;
            salvos.clear();
        }
    }

    private final RepositorioEmMemoria repositorio = new RepositorioEmMemoria();
    private final ServicoRanking servico = new ServicoRanking(repositorio, new RelogioFixo());

    private static ResultadoPartida vitoria(int pontos) {
        return new ResultadoPartida("Ana", Dificuldade.DIFICIL, pontos, 12, 34, 5);
    }

    @Teste
    void vitoriaQueEntraNoTopEhSalvaComDataFormatada() {
        ServicoRanking.Registro r = servico.registrarVitoria(servico.carregar(), vitoria(50));
        verdadeiro(r.entrou(), "entrou");
        igual(null, r.erroAoSalvar());
        igual(List.of(new RegistroRanking("Ana", 50, Dificuldade.DIFICIL, 5, "2026-11-04 10:15:30", 34)),
                repositorio.salvos);
    }

    @Teste
    void vitoriaQueNaoSuperaOQuintoNaoEhSalva() {
        for (int i = 1; i <= 5; i++) {
            servico.registrarVitoria(servico.carregar(), vitoria(i * 10));
        }
        ServicoRanking.Registro r = servico.registrarVitoria(servico.carregar(), vitoria(10));
        falso(r.entrou(), "não supera o 5º");
        igual(5, repositorio.salvos.size());
    }

    @Teste
    void pontuacaoNaoPositivaNuncaEntra() {
        falso(servico.registrarVitoria(Ranking.vazio(), vitoria(0)).entrou(), "zero pontos");
        igual(0, repositorio.salvos.size());
    }

    @Teste
    void falhaAoSalvarAindaContaComoEntradaETrazOMotivo() {
        repositorio.falharAoSalvar = true;
        ServicoRanking.Registro r = servico.registrarVitoria(Ranking.vazio(), vitoria(50));
        verdadeiro(r.entrou(), "entrou no Top 5");
        igual("disco cheio", r.erroAoSalvar());
    }

    @Teste
    void apagarDelegaAoRepositorio() throws IOException {
        servico.apagar();
        verdadeiro(repositorio.apagado, "apagou");
    }
}
