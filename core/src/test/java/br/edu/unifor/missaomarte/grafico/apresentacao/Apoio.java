package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.RepositorioRanking;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import br.edu.unifor.missaomarte.grafico.audio.Sons;
import br.edu.unifor.missaomarte.grafico.online.PartidaDoHistorico;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Peças de teste: ranking em memória, relógio controlado e sons anotados. Nada de arquivo, janela, OpenGL ou áudio. */
final class Apoio {

    private Apoio() {
    }

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

    static final class RelogioParado implements Relogio {
        long millis;
        LocalDateTime dataHora = LocalDateTime.of(2026, 11, 17, 10, 15, 30);

        @Override
        public long agoraEmMillis() {
            return millis;
        }

        @Override
        public LocalDateTime agora() {
            return dataHora;
        }
    }

    /** Em vez de tocar, anota cada som pedido (respeitando o mudo, como a implementação de verdade). */
    static final class SonsGravados implements Sons {
        final List<Som> tocados = new ArrayList<>();
        boolean mudo;

        @Override
        public void tocar(Som som) {
            if (!mudo) {
                tocados.add(som);
            }
        }

        @Override
        public void alternarMudo() {
            mudo = !mudo;
        }

        @Override
        public boolean isMudo() {
            return mudo;
        }
    }

    /**
     * Servidor falso: anota cada pedido e guarda a resposta pendente, que o teste completa quando quiser (assim dá
     * para ver a tela no estado "aguardando").
     */
    static final class ServidorFalso implements ServidorDoJogo {
        final List<String> pedidos = new ArrayList<>();
        Resposta<Void> cadastro;
        Resposta<Sessao> login;
        Resposta<Void> registro;
        Resposta<List<PosicaoGlobal>> ranking;
        Resposta<List<PartidaDoHistorico>> historico;
        PartidaParaEnviar partidaEnviada;
        Sessao sessaoUsada;

        @Override
        public void cadastrar(String nome, String senha, Resposta<Void> r) {
            pedidos.add("cadastrar " + nome + " " + senha);
            cadastro = r;
        }

        @Override
        public void entrar(String nome, String senha, Resposta<Sessao> r) {
            pedidos.add("entrar " + nome + " " + senha);
            login = r;
        }

        @Override
        public void registrarPartida(Sessao sessao, PartidaParaEnviar partida, Resposta<Void> r) {
            pedidos.add("registrar " + sessao.nome());
            sessaoUsada = sessao;
            partidaEnviada = partida;
            registro = r;
        }

        @Override
        public void ranking(Modo modo, Resposta<List<PosicaoGlobal>> r) {
            pedidos.add(modo == Modo.CLASSICO ? "ranking" : "ranking " + modo);
            ranking = r;
        }

        @Override
        public void historico(Sessao sessao, Resposta<List<PartidaDoHistorico>> r) {
            pedidos.add("historico " + sessao.nome());
            historico = r;
        }
    }
}
