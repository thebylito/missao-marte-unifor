package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import java.io.IOException;

/** Casos de uso do ranking: consultar, registrar uma vitória e apagar o histórico. */
public class ServicoRanking {

    /** Se a vitória entrou no Top 5 e, se não foi possível salvar, o motivo. */
    public record Registro(boolean entrou, String erroAoSalvar) {
    }

    private final RepositorioRanking repositorio;
    private final Relogio relogio;

    public ServicoRanking(RepositorioRanking repositorio, Relogio relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public Ranking carregar() {
        return new Ranking(repositorio.carregar());
    }

    public Registro registrarVitoria(Ranking atual, ResultadoPartida r) {
        if (r.pontos() <= 0 || !atual.entraNoTop(r.pontos())) {
            return new Registro(false, null);
        }
        RegistroRanking novo = new RegistroRanking(r.piloto(), r.pontos(), r.dificuldade(),
                r.passageirosResgatados(), formatarDataHora(), r.tempoSegundos());
        try {
            repositorio.salvar(atual.com(novo).getRegistros());
            return new Registro(true, null);
        } catch (IOException e) {
            return new Registro(true, e.getMessage());
        }
    }

    public void apagar() throws IOException {
        repositorio.apagar();
    }

    /**
     * "aaaa-mm-dd hh:mm:ss", como na versão original. Atenção (comportamento herdado): LocalDateTime.toString()
     * omite os segundos quando segundos e nanossegundos são zero, e então o substring falha.
     */
    private String formatarDataHora() {
        return relogio.agora().toString().substring(0, 19).replace('T', ' ');
    }
}
