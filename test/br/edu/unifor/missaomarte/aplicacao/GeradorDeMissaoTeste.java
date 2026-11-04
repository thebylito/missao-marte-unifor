package br.edu.unifor.missaomarte.aplicacao;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Obstaculo;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import br.edu.unifor.missaomarte.teste.Teste;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class GeradorDeMissaoTeste {

    @Teste
    void quantidadesSeguemADificuldade() {
        for (Dificuldade d : Dificuldade.values()) {
            for (long semente = 0; semente < 20; semente++) {
                Missao m = new GeradorDeMissao(new Random(semente)).gerar(new Mapa(2), d);
                igual(d.getPassageiros(), m.getPassageiros().size(), d + " passageiros");
                igual(d.getAsteroides(), m.getAsteroides().size(), d + " asteroides");
                igual(d.getInimigos(), m.getInimigos().size(), d + " inimigos");
                igual(5, m.getNave().getCapacidade());
            }
        }
    }

    @Teste
    void cadaCoisaEmUmaCelulaLivreDentroDoMapaEForaDaPlataforma() {
        for (long semente = 0; semente < 50; semente++) {
            Missao m = new GeradorDeMissao(new Random(semente)).gerar(new Mapa(2), Dificuldade.DIFICIL);
            List<Posicao> todas = new ArrayList<>();
            m.getPassageiros().forEach(p -> todas.add(p.getPosicao()));
            Stream.concat(m.getAsteroides().stream(), m.getInimigos().stream())
                    .map(Obstaculo::getPosicao).forEach(todas::add);
            Set<Posicao> distintas = new HashSet<>(todas);
            igual(todas.size(), distintas.size(), "duas coisas na mesma célula (semente " + semente + ")");
            falso(distintas.contains(Posicao.ORIGEM), "algo na plataforma");
            verdadeiro(distintas.stream().allMatch(m.getMapa()::contem), "algo fora do mapa");
        }
    }

    @Teste
    void passageirosSaemNaOrdemFixaESemAstronautaNoFacil() {
        Missao medio = new GeradorDeMissao(new Random(1)).gerar(new Mapa(3), Dificuldade.MEDIO);
        igual(List.of("Dr. Silva", "Eng. Rosa", "Dr. Lima", "Eng. Carlos", "Ast. Maria"),
                medio.getPassageiros().stream().map(Passageiro::getNome).collect(Collectors.toList()));
        Missao facil = new GeradorDeMissao(new Random(1)).gerar(new Mapa(3), Dificuldade.FACIL);
        falso(facil.getPassageiros().stream().anyMatch(p -> p.getTipo() == TipoPassageiro.ASTRONAUTA),
                "Fácil tem só 4 passageiros: Professor, Engenheiro, Professor, Engenheiro");
    }

    @Teste
    void mesmaSementeMesmaMissao() {
        Missao a = new GeradorDeMissao(new Random(99)).gerar(new Mapa(4), Dificuldade.DIFICIL);
        Missao b = new GeradorDeMissao(new Random(99)).gerar(new Mapa(4), Dificuldade.DIFICIL);
        igual(posicoes(a), posicoes(b));
    }

    private static List<Posicao> posicoes(Missao m) {
        List<Posicao> todas = new ArrayList<>();
        m.getPassageiros().forEach(p -> todas.add(p.getPosicao()));
        m.getAsteroides().forEach(x -> todas.add(x.getPosicao()));
        m.getInimigos().forEach(x -> todas.add(x.getPosicao()));
        return todas;
    }
}
