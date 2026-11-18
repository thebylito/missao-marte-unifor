package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.MovimentoInimigo;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O HUD só lê a Partida: os números conferem com o domínio a cada turno. */
class HudTeste {

    private final Missao missao = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);
    private final Partida partida = new Partida("Ana", Dificuldade.MEDIO, missao, new Apoio.RelogioParado());

    @Test
    void comecoDaPartida() {
        missao.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        Hud hud = Hud.de(partida);
        assertEquals("Ana", hud.piloto());
        assertEquals("Médio", hud.dificuldade());
        assertEquals(20, hud.pontos());
        assertEquals(1f, hud.combustivel());
        assertEquals(3, hud.vidas());
        assertEquals(3, hud.vidasIniciais());
        assertEquals(List.of(), hud.aBordo());
        assertEquals(5, hud.capacidade());
        assertEquals(1, hud.restantes());
        assertEquals(0, hud.movimentos());
    }

    @Test
    void combustivelCaiComOsMovimentosEColisaoTiraVida() {
        missao.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        missao.adicionarAsteroide(new Asteroide(new Posicao(1, 0)));
        partida.executar(Comando.DIREITA);                 // bate no asteroide
        partida.executar(Comando.ESQUERDA);
        Hud hud = Hud.de(partida);
        assertEquals(18, hud.pontos());
        assertEquals(18f / 20f, hud.combustivel(), 1e-6);
        assertEquals(2, hud.vidas());
        assertEquals(2, hud.movimentos());
    }

    @Test
    void passageirosABordoNaOrdemDeEmbarqueECombustivelLimitadoA1() {
        missao.adicionarPassageiro(new Engenheiro("Eng. Rosa", new Posicao(1, 0)));
        missao.adicionarPassageiro(new Professor("longe", new Posicao(-2, -2)));
        partida.executar(Comando.DIREITA);
        partida.executar(Comando.EMBARCAR);                // +15: 19 + 15 = 34 pontos, acima dos 20 iniciais
        Hud hud = Hud.de(partida);
        assertEquals(List.of(TipoPassageiro.ENGENHEIRO), hud.aBordo());
        assertEquals(34, hud.pontos());
        assertEquals(1f, hud.combustivel());
        assertEquals(1, hud.restantes());
    }

    @Test
    void tomDasMensagens() {
        assertEquals(Hud.Tom.SUCESSO, Hud.tom(Evento.de(Evento.Tipo.PASSAGEIRO_EMBARCADO)));
        assertEquals(Hud.Tom.SUCESSO, Hud.tom(Evento.de(Evento.Tipo.VITORIA)));
        assertEquals(Hud.Tom.ALERTA, Hud.tom(Evento.de(Evento.Tipo.RETORNE_A_PLATAFORMA)));
        assertEquals(Hud.Tom.ALERTA, Hud.tom(Evento.de(Evento.Tipo.NAVE_CHEIA)));
        assertEquals(Hud.Tom.ALERTA, Hud.tom(Evento.de(Evento.Tipo.SEM_PASSAGEIRO_NA_POSICAO)));
        assertEquals(Hud.Tom.PERIGO, Hud.tom(Evento.de(Evento.Tipo.COLISAO)));
        assertEquals(Hud.Tom.PERIGO, Hud.tom(Evento.de(Evento.Tipo.NAVE_DESTRUIDA)));
        assertEquals(Hud.Tom.PERIGO, Hud.tom(Evento.de(Evento.Tipo.PONTOS_ESGOTADOS)));
        assertEquals(Hud.Tom.NEUTRO, Hud.tom(Evento.de(Evento.Tipo.ABORTADA)));
    }
}
