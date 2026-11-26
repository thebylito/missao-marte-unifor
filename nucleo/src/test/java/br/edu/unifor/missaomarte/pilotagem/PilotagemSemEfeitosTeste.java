package br.edu.unifor.missaomarte.pilotagem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import java.util.List;
import org.junit.jupiter.api.Test;

class PilotagemSemEfeitosTeste {

    @Test
    void mesmosComandosReproduzemExatamenteAFisicaAnteriorEmCadaPasso() {
        PartidaDePilotagem partida = PartidaDePilotagem.nova("Ana", Dificuldade.MEDIO);
        NaveEmVoo nave = partida.getNave();
        Comandos motorComGiro = new Comandos(false, true, true);
        long assinatura = 0;
        for (int i = 0; i < 600; i++) {
            Comandos comandos = i < 90 ? Comandos.MOTOR
                    : i < 108 ? motorComGiro : i < 150 ? Comandos.MOTOR : Comandos.NENHUM;
            partida.avancarUmPasso(comandos);
            assinatura = 31 * assinatura + List.of(partida.getTempo(), nave.getX(), nave.getY(),
                    nave.getVelocidadeX(), nave.getVelocidadeY(), nave.getAngulo(), nave.getCombustivel(),
                    nave.getEstado().name(), nave.isMotorLigado(), partida.getVidas(), partida.getPousos(),
                    partida.getPontos(), partida.consumirEventos().toString()).hashCode();
        }
        // Gabarito obtido na Fase 7, antes de alterar a física (7a06b4b).
        assertEquals(6434777540470490235L, assinatura);
        assertEquals(498.36545808385944, nave.getX());
        assertEquals(214.15171905841896, nave.getY());
        assertEquals(96.24999999999915, nave.getCombustivel());
        assertEquals(NaveEmVoo.Estado.VOANDO, nave.getEstado());
    }
}
