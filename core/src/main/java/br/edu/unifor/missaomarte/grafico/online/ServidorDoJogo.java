package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import java.util.List;
import java.util.Optional;

/**
 * O que o jogo precisa do servidor de ranking (porta, no sentido de "ports and adapters"). As telas dependem desta
 * interface; a implementação HTTP ({@link ServidorHttp}) fica atrás dela, e os testes usam uma versão falsa. Todos
 * os métodos são assíncronos: voltam na hora e respondem depois, sem travar a tela.
 */
public interface ServidorDoJogo {

    void cadastrar(String nome, String senha, Resposta<Void> resposta);

    void entrar(String nome, String senha, Resposta<Sessao> resposta);

    void registrarPartida(Sessao sessao, PartidaParaEnviar partida, Resposta<Void> resposta);

    /**
     * Registro com recompensa. O nome distinto preserva o método legado (Resposta<Void> tem a mesma
     * assinatura apagada que Resposta<RegistroDePartida>). Um servidor antigo devolve sucesso sem recompensa.
     */
    default void registrarPartidaComMoedas(Sessao sessao, PartidaParaEnviar partida,
                                          Resposta<RegistroDePartida> resposta) {
        registrarPartida(sessao, partida, new Resposta<>() {
            @Override public void sucesso(Void nada) { resposta.sucesso(null); }
            @Override public void falha(Falha falha) { resposta.falha(falha); }
        });
    }

    default void iniciarPartida(Sessao sessao, Modo modo, Dificuldade dificuldade, Resposta<Bilhete> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void iniciarHorda(Sessao sessao, Dificuldade dificuldade, long numero, Resposta<Bilhete> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void hordaAtual(Resposta<HordaAtual> resposta) {
        resposta.falha(Falha.semConexao());
    }

    /** Sem número, consulta o Top 10 da horda atual. */
    default void rankingDaHorda(Long numero, Resposta<List<PosicaoGlobal>> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void rankingSemanal(Resposta<RankingSemanal> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void loja(Resposta<List<SkinDaLoja>> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void hangar(Sessao sessao, Resposta<Hangar> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void comprar(Sessao sessao, String skin, Resposta<Hangar> resposta) {
        resposta.falha(Falha.semConexao());
    }

    default void equipar(Sessao sessao, String skin, Resposta<String> resposta) {
        resposta.falha(Falha.semConexao());
    }

    /** Fase 7: o ranking global de um modo (cada modo tem o seu). */
    void ranking(Modo modo, Resposta<List<PosicaoGlobal>> resposta);

    /** O ranking do modo clássico (o único das Fases 5 e 6). */
    default void ranking(Resposta<List<PosicaoGlobal>> resposta) {
        ranking(Modo.CLASSICO, resposta);
    }

    void historico(Sessao sessao, Resposta<List<PartidaDoHistorico>> resposta);

    /**
     * Fase 7 (modo shadow): uma conexão nova com o canal das sombras do mesmo servidor, ou vazio se não houver
     * servidor (as sombras ficam desligadas).
     */
    default Optional<ConexaoDeSombras> conexaoDeSombras() {
        return Optional.empty();
    }

    /** Sem servidor configurado (e nos testes antigos): todo pedido falha como "sem conexão". */
    ServidorDoJogo DESLIGADO = new ServidorDoJogo() {
        @Override public void cadastrar(String nome, String senha, Resposta<Void> r) { r.falha(Falha.semConexao()); }
        @Override public void entrar(String nome, String senha, Resposta<Sessao> r) { r.falha(Falha.semConexao()); }
        @Override public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { r.falha(Falha.semConexao()); }
        @Override public void ranking(Modo modo, Resposta<List<PosicaoGlobal>> r) { r.falha(Falha.semConexao()); }
        @Override public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { r.falha(Falha.semConexao()); }
    };
}
