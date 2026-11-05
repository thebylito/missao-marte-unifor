package br.edu.unifor.missaomarte.aplicacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Obstaculo;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class GeradorDeMissaoTeste {

    @Test
    void quantidadesSeguemADificuldade() {
        for (Dificuldade d : Dificuldade.values()) {
            for (long semente = 0; semente < 20; semente++) {
                Missao m = new GeradorDeMissao(new Random(semente)).gerar(new Mapa(2), d);
                assertEquals(d.getPassageiros(), m.getPassageiros().size(), d + " passageiros");
                assertEquals(d.getAsteroides(), m.getAsteroides().size(), d + " asteroides");
                assertEquals(d.getInimigos(), m.getInimigos().size(), d + " inimigos");
                assertEquals(5, m.getNave().getCapacidade());
            }
        }
    }

    @Test
    void cadaCoisaEmUmaCelulaLivreDentroDoMapaEForaDaPlataforma() {
        for (long semente = 0; semente < 50; semente++) {
            Missao m = new GeradorDeMissao(new Random(semente)).gerar(new Mapa(2), Dificuldade.DIFICIL);
            List<Posicao> todas = new ArrayList<>();
            m.getPassageiros().forEach(p -> todas.add(p.getPosicao()));
            Stream.concat(m.getAsteroides().stream(), m.getInimigos().stream())
                    .map(Obstaculo::getPosicao).forEach(todas::add);
            Set<Posicao> distintas = new HashSet<>(todas);
            assertEquals(todas.size(), distintas.size(), "duas coisas na mesma célula (semente " + semente + ")");
            assertFalse(distintas.contains(Posicao.ORIGEM), "algo na plataforma");
            assertTrue(distintas.stream().allMatch(m.getMapa()::contem), "algo fora do mapa");
        }
    }

    @Test
    void passageirosSaemNaOrdemFixaESemAstronautaNoFacil() {
        Missao medio = new GeradorDeMissao(new Random(1)).gerar(new Mapa(3), Dificuldade.MEDIO);
        assertEquals(List.of("Dr. Silva", "Eng. Rosa", "Dr. Lima", "Eng. Carlos", "Ast. Maria"),
                medio.getPassageiros().stream().map(Passageiro::getNome).collect(Collectors.toList()));
        Missao facil = new GeradorDeMissao(new Random(1)).gerar(new Mapa(3), Dificuldade.FACIL);
        assertFalse(facil.getPassageiros().stream().anyMatch(p -> p.getTipo() == TipoPassageiro.ASTRONAUTA),
                "Fácil tem só 4 passageiros: Professor, Engenheiro, Professor, Engenheiro");
    }

    @Test
    void mesmaSementeMesmaMissao() {
        Missao a = new GeradorDeMissao(new Random(99)).gerar(new Mapa(4), Dificuldade.DIFICIL);
        Missao b = new GeradorDeMissao(new Random(99)).gerar(new Mapa(4), Dificuldade.DIFICIL);
        assertEquals(posicoes(a), posicoes(b));
    }

    private static List<Posicao> posicoes(Missao m) {
        List<Posicao> todas = new ArrayList<>();
        m.getPassageiros().forEach(p -> todas.add(p.getPosicao()));
        m.getAsteroides().forEach(x -> todas.add(x.getPosicao()));
        m.getInimigos().forEach(x -> todas.add(x.getPosicao()));
        return todas;
    }
}
