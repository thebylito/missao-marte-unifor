package br.edu.unifor.missaomarte.aplicacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import org.junit.jupiter.api.Test;
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

    @Test
    void vitoriaQueEntraNoTopEhSalvaComDataFormatada() {
        ServicoRanking.Registro r = servico.registrarVitoria(servico.carregar(), vitoria(50));
        assertTrue(r.entrou(), "entrou");
        assertEquals(null, r.erroAoSalvar());
        assertEquals(List.of(new RegistroRanking("Ana", 50, Dificuldade.DIFICIL, 5, "2026-11-04 10:15:30", 34)),
                repositorio.salvos);
    }

    @Test
    void vitoriaQueNaoSuperaOQuintoNaoEhSalva() {
        for (int i = 1; i <= 5; i++) {
            servico.registrarVitoria(servico.carregar(), vitoria(i * 10));
        }
        ServicoRanking.Registro r = servico.registrarVitoria(servico.carregar(), vitoria(10));
        assertFalse(r.entrou(), "não supera o 5º");
        assertEquals(5, repositorio.salvos.size());
    }

    @Test
    void pontuacaoNaoPositivaNuncaEntra() {
        assertFalse(servico.registrarVitoria(Ranking.vazio(), vitoria(0)).entrou(), "zero pontos");
        assertEquals(0, repositorio.salvos.size());
    }

    @Test
    void falhaAoSalvarAindaContaComoEntradaETrazOMotivo() {
        repositorio.falharAoSalvar = true;
        ServicoRanking.Registro r = servico.registrarVitoria(Ranking.vazio(), vitoria(50));
        assertTrue(r.entrou(), "entrou no Top 5");
        assertEquals("disco cheio", r.erroAoSalvar());
    }

    @Test
    void apagarDelegaAoRepositorio() throws IOException {
        servico.apagar();
        assertTrue(repositorio.apagado, "apagou");
    }
}
