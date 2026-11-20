package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo.Tipo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * CT-18: a caverna marciana pode ser vencida com as regras de verdade, sem mãos e sem janela. O piloto automático é
 * conservador (para e espera o asteroide em cada passagem estreita), então gasta mais combustível que um piloto
 * bom: vence a missão inteira no Fácil e, no Médio e no Difícil, faz as viagens do Astronauta e do Engenheiro. A do
 * Professor, a mais longa, passa dos 100 de combustível do jeito que ele voa (achado registrado no tutorial).
 */
class PilotoAutomaticoTeste {

    private static final double LIMITE = 400;

    @Test
    void noFacilVenceACavernaInteiraSemNenhumaExplosao() {
        PartidaDePilotagem p = PartidaDePilotagem.nova("Auto", Dificuldade.FACIL);
        List<EventoDeVoo> eventos = PilotoAutomatico.jogar(p, LIMITE);
        assertEquals(Partida.Estado.VITORIA, p.getEstado());
        assertTrue(eventos.stream().noneMatch(e -> e.tipo() == Tipo.EXPLODIU));
        assertEquals(3, p.getEntregues());
        assertEquals(45, p.getPontosDosResgates(), "20 + 15 + 10, os pontos do jogo clássico");
        assertEquals(p.getPontosDosResgates() + p.getBonus(), p.getPontos());
        assertEquals(6, p.getPousos(), "três viagens: o passageiro e a base, três vezes");
        assertEquals(3, eventos.stream().filter(e -> e.tipo() == Tipo.EMBARCOU).count());
        assertEquals(Tipo.VITORIA, eventos.get(eventos.size() - 1).tipo());
    }

    @Test
    void oVooEDeterministico() {
        PartidaDePilotagem primeira = PartidaDePilotagem.nova("Auto", Dificuldade.FACIL);
        PartidaDePilotagem segunda = PartidaDePilotagem.nova("Auto", Dificuldade.FACIL);
        List<EventoDeVoo> a = PilotoAutomatico.jogar(primeira, LIMITE);
        List<EventoDeVoo> b = PilotoAutomatico.jogar(segunda, LIMITE);
        assertEquals(a, b, "mesmos eventos, nos mesmos lugares");
        assertEquals(primeira.getTempo(), segunda.getTempo());
        assertEquals(primeira.getNave().getCombustivel(), segunda.getNave().getCombustivel());
    }

    @Test
    void noMedioENoDificilFazAsViagensDoAstronautaEDoEngenheiro() {
        for (Dificuldade d : List.of(Dificuldade.MEDIO, Dificuldade.DIFICIL)) {
            PartidaDePilotagem p = PartidaDePilotagem.nova("Auto", d);
            List<EventoDeVoo> eventos = PilotoAutomatico.jogar(p, LIMITE);
            int primeiraExplosao = indiceDe(eventos, Tipo.EXPLODIU);
            List<EventoDeVoo> antes = eventos.subList(0, primeiraExplosao < 0 ? eventos.size() : primeiraExplosao);
            List<Integer> entregas = antes.stream().filter(e -> e.tipo() == Tipo.ENTREGOU).map(EventoDeVoo::valor)
                    .toList();
            assertEquals(List.of(20, 15), entregas.subList(0, Math.min(2, entregas.size())), d.name());
        }
    }

    private static int indiceDe(List<EventoDeVoo> eventos, Tipo tipo) {
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).tipo() == tipo) {
                return i;
            }
        }
        return -1;
    }
}
