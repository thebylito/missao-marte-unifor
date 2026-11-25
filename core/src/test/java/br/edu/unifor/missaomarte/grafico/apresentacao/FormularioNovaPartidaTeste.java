package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import org.junit.jupiter.api.Test;

/** As perguntas do console antes da partida, com os mesmos padrões. */
class FormularioNovaPartidaTeste {

    @Test
    void oModoComecaNaPilotagemEAlterna() {                    // CT-19 (Fase 7); a pilotagem é o padrão desde 2026-11-25
        FormularioNovaPartida f = new FormularioNovaPartida();
        assertEquals(Modo.PILOTAGEM, f.getModo());
        f.alternarModo();
        assertEquals(Modo.CLASSICO, f.getModo());
        f.alternarModo();
        assertEquals(Modo.PILOTAGEM, f.getModo());
    }

    private final FormularioNovaPartida form = new FormularioNovaPartida();

    @Test
    void padroesSaoOsDoConsole() {
        assertEquals("Piloto Anônimo", form.getPiloto());
        assertEquals(Dificuldade.MEDIO, form.getDificuldade());
        assertEquals(5, form.getTamanho());
    }

    @Test
    void nomeDigitadoSemEspacosNasPontas() {
        for (char c : "  Ana  ".toCharArray()) {
            form.digitar(c);
        }
        assertEquals("  Ana  ", form.getNomeDigitado());
        assertEquals("Ana", form.getPiloto());
    }

    @Test
    void soEspacosViramOPadrao() {
        form.digitar(' ');
        form.digitar(' ');
        assertEquals("Piloto Anônimo", form.getPiloto());
    }

    @Test
    void teclasDeControleNaoEntramEApagarRemoveAUltimaLetra() {
        form.digitar('A');
        form.digitar('\n');
        form.digitar('\b');
        form.digitar('\t');
        form.digitar('n');
        form.digitar('x');
        form.apagarLetra();
        assertEquals("An", form.getNomeDigitado());
    }

    @Test
    void apagarComNomeVazioNaoFalha() {
        form.apagarLetra();
        assertEquals("", form.getNomeDigitado());
    }

    @Test
    void nomeTemNoMaximoVinteLetras() {
        for (int i = 0; i < 30; i++) {
            form.digitar('a');
        }
        assertEquals(FormularioNovaPartida.NOME_MAXIMO, form.getNomeDigitado().length());
    }

    @Test
    void dificuldadeGiraNosDoisSentidos() {
        form.proximaDificuldade();
        assertEquals(Dificuldade.DIFICIL, form.getDificuldade());
        form.proximaDificuldade();
        assertEquals(Dificuldade.FACIL, form.getDificuldade());
        form.dificuldadeAnterior();
        assertEquals(Dificuldade.DIFICIL, form.getDificuldade());
    }

    @Test
    void tamanhoFicaEntreDoisEDez() {
        for (int i = 0; i < 20; i++) {
            form.aumentarMapa();
        }
        assertEquals(10, form.getTamanho());
        for (int i = 0; i < 20; i++) {
            form.diminuirMapa();
        }
        assertEquals(2, form.getTamanho());
    }
}
