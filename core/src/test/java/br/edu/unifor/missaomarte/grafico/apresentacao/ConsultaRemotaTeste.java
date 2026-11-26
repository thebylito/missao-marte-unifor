package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import org.junit.jupiter.api.Test;

/** A consulta do histórico: um 401 quer dizer que o token deixou de valer, e o jogo desconecta o piloto. */
class ConsultaRemotaTeste {

    @Test
    void tokenRecusadoAvisaQueASessaoAcabou() {
        ConsultaRemota<String> c = new ConsultaRemota<>();
        c.falha(new Falha(Falha.Motivo.NAO_AUTORIZADO, "Token ausente, inválido ou expirado. Entre novamente."));
        assertEquals(ConsultaRemota.Estado.FALHOU, c.getEstado());
        assertEquals("Token ausente, inválido ou expirado. Entre novamente.", c.getMensagem());
        assertTrue(c.isSessaoRecusada());
    }

    @Test
    void outrasFalhasEAsRespostasNaoMexemNaSessao() {
        ConsultaRemota<String> semRede = new ConsultaRemota<>();
        semRede.falha(Falha.semConexao());
        assertFalse(semRede.isSessaoRecusada());

        ConsultaRemota<String> pronta = new ConsultaRemota<>();
        pronta.sucesso("ok");
        assertFalse(pronta.isSessaoRecusada());
        assertFalse(new ConsultaRemota<String>().isSessaoRecusada());
    }
}
