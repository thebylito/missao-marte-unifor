package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDaDificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** DA-48: a Pilotagem Livre joga com os últimos ajustes que o servidor mandou; sem servidor, com o padrão. */
class AjustesDoServidorTeste {

    private static final AjustesDeVoo OUTROS = new AjustesDeVoo(AjustesDeVoo.PADRAO.facil(),
            new AjustesDaDificuldade(100, 75, 50, 30, 1.0), AjustesDeVoo.PADRAO.dificil(),
            AjustesDeVoo.PADRAO.eventos());

    /** Servidor falso: responde os ajustes dados, ou falha sem conexão quando não há. */
    private static final class Servidor implements ServidorDoJogo {
        AjustesDeVoo ajustes;
        int pedidos;

        @Override public void ajustes(Resposta<AjustesDeVoo> resposta) {
            pedidos++;
            if (ajustes == null) resposta.falha(Falha.semConexao());
            else resposta.sucesso(ajustes);
        }
        @Override public void cadastrar(String nome, String senha, Resposta<Void> r) { DESLIGADO.cadastrar(nome, senha, r); }
        @Override public void entrar(String nome, String senha, Resposta<Sessao> r) { DESLIGADO.entrar(nome, senha, r); }
        @Override public void registrarPartida(Sessao s, PartidaParaEnviar p, Resposta<Void> r) { DESLIGADO.registrarPartida(s, p, r); }
        @Override public void ranking(Modo modo, Resposta<List<PosicaoGlobal>> r) { DESLIGADO.ranking(modo, r); }
        @Override public void historico(Sessao s, Resposta<List<PartidaDoHistorico>> r) { DESLIGADO.historico(s, r); }
    }

    private final Servidor servidor = new Servidor();
    private final AjustesDoServidor ajustes = new AjustesDoServidor();

    @Test
    void semRespostaAindaOPadrao() {
        assertEquals(AjustesDeVoo.PADRAO, ajustes.atuais());
        ajustes.atualizar(servidor);
        assertEquals(AjustesDeVoo.PADRAO, ajustes.atuais());
        assertEquals(1, servidor.pedidos);
    }

    @Test
    void ficaComOsAjustesQueOServidorMandou() {
        servidor.ajustes = OUTROS;
        ajustes.atualizar(servidor);
        assertEquals(OUTROS, ajustes.atuais());
    }

    @Test
    void umaFalhaDepoisMantemOsUltimosLidos() {
        servidor.ajustes = OUTROS;
        ajustes.atualizar(servidor);
        servidor.ajustes = null;
        ajustes.atualizar(servidor);
        assertEquals(OUTROS, ajustes.atuais());
    }

    @Test
    void oServidorDesligadoNaoMudaNada() {
        ajustes.atualizar(ServidorDoJogo.DESLIGADO);
        assertEquals(AjustesDeVoo.PADRAO, ajustes.atuais());
    }
}
