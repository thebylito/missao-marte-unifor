package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDoMenu.Acao;
import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;

/** As 4 opções do menu do console, agora por tecla. */
class ControladorDoMenuTeste {

    private final Apoio.RepositorioEmMemoria repositorio = new Apoio.RepositorioEmMemoria();
    private final ControladorDoMenu menu =
            new ControladorDoMenu(new ServicoRanking(repositorio, new Apoio.RelogioParado()));

    @Test
    void numerosDaLinhaENumpadEscolhemAsOpcoes() {
        assertEquals(Acao.NOVA_PARTIDA, menu.teclaPressionada(Keys.NUM_1));
        assertEquals(Acao.NOVA_PARTIDA, menu.teclaPressionada(Keys.NUMPAD_1));
        assertEquals(Acao.RANKING, menu.teclaPressionada(Keys.NUM_2));
        assertEquals(Acao.RANKING, menu.teclaPressionada(Keys.NUMPAD_2));
        assertEquals(Acao.SAIR, menu.teclaPressionada(Keys.NUM_4));
        assertEquals(Acao.SAIR, menu.teclaPressionada(Keys.NUMPAD_4));
    }

    @Test
    void opcaoInvalidaAvisaENaoSaiDoMenu() {
        assertEquals(Acao.NENHUMA, menu.teclaPressionada(Keys.NUM_9));
        assertEquals("Opção inválida. Tente novamente.", menu.getMensagem());
    }

    @Test
    void apagarPedeConfirmacaoESApaga() {
        assertEquals(Acao.NENHUMA, menu.teclaPressionada(Keys.NUM_3));
        assertTrue(menu.isConfirmandoApagar());
        assertEquals(ControladorDoMenu.PERGUNTA_APAGAR, menu.getMensagem());

        assertEquals(Acao.NENHUMA, menu.teclaPressionada(Keys.S));
        assertTrue(repositorio.apagado);
        assertFalse(menu.isConfirmandoApagar());
        assertEquals("Ranking resetado com sucesso!", menu.getMensagem());
    }

    @Test
    void qualquerOutraTeclaCancelaOApagar() {
        menu.teclaPressionada(Keys.NUM_3);
        // na confirmação, até o "1" só cancela: não começa uma partida
        assertEquals(Acao.NENHUMA, menu.teclaPressionada(Keys.NUM_1));
        assertFalse(repositorio.apagado);
        assertEquals("Operação cancelada.", menu.getMensagem());
    }

    @Test
    void escolherUmaOpcaoValidaLimpaAMensagemAnterior() {
        menu.teclaPressionada(Keys.NUM_9);
        menu.teclaPressionada(Keys.NUM_2);
        assertEquals("", menu.getMensagem());
    }
}
