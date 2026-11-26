package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** O envio do resultado no fim da partida: convidado não envia, e a falha do servidor vira só um aviso. */
class EnvioDoResultadoTeste {

    private final Apoio.ServidorFalso servidor = new Apoio.ServidorFalso();
    private final PartidaParaEnviar partida = new PartidaParaEnviar("VITORIA", "FACIL", 60, 4, 20, 5);

    @Test
    void convidadoNaoEnviaNada() {
        EnvioDoResultado e = EnvioDoResultado.iniciar(Optional.empty(), servidor, partida);
        assertEquals(EnvioDoResultado.Estado.CONVIDADO, e.getEstado());
        assertTrue(servidor.pedidos.isEmpty());
        assertEquals("Modo convidado: o resultado fica só no ranking local.", e.texto());
    }

    @Test
    void conectadoEnviaComASessaoEMostraOAndamento() {
        Sessao ana = new Sessao("Ana", "tok");
        EnvioDoResultado e = EnvioDoResultado.iniciar(Optional.of(ana), servidor, partida);
        assertEquals(EnvioDoResultado.Estado.ENVIANDO, e.getEstado());
        assertEquals(ana, servidor.sessaoUsada);
        assertEquals(partida, servidor.partidaEnviada);
        servidor.registro.sucesso(null);
        assertEquals(EnvioDoResultado.Estado.ENVIADO, e.getEstado());
        assertEquals("Resultado registrado no servidor.", e.texto());
    }

    @Test
    void falhaDoServidorViraAviso() {
        EnvioDoResultado e = EnvioDoResultado.iniciar(Optional.of(new Sessao("Ana", "tok")), servidor, partida);
        servidor.registro.falha(Falha.semConexao());
        assertEquals(EnvioDoResultado.Estado.FALHOU, e.getEstado());
        assertEquals("Não foi possível registrar no servidor. Servidor indisponível. Você pode jogar como convidado.",
                e.texto());
        assertFalse(e.isSessaoRecusada(), "servidor fora do ar não desconecta o piloto");
    }

    @Test
    void tokenRecusadoAvisaQueASessaoAcabou() {
        EnvioDoResultado e = EnvioDoResultado.iniciar(Optional.of(new Sessao("Ana", "tok")), servidor, partida);
        servidor.registro.falha(new Falha(Falha.Motivo.NAO_AUTORIZADO,
                "Token ausente, inválido ou expirado. Entre novamente."));
        assertEquals(EnvioDoResultado.Estado.FALHOU, e.getEstado());
        assertTrue(e.isSessaoRecusada());
    }
}
