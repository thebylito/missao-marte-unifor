package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.MovimentoAleatorio;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Fábrica da missão (padrão Factory / GRASP Creator): sorteia passageiros, asteroides e inimigos em células
 * livres. O Random é injetado: com a mesma semente, a mesma missão (testes reproduzíveis).
 */
public final class GeradorDeMissao {

    public static final int CAPACIDADE_DA_NAVE = 5;

    private final Random random;

    public GeradorDeMissao(Random random) {
        this.random = random;
    }

    public Missao gerar(Mapa mapa, Dificuldade dificuldade) {
        Missao missao = new Missao(mapa, new Nave(CAPACIDADE_DA_NAVE), new MovimentoAleatorio(random));
        while (missao.getPassageiros().size() < dificuldade.getPassageiros()) {
            int ordem = missao.getPassageiros().size();
            colocarEmCelulaLivre(missao, p -> missao.adicionarPassageiro(criarPassageiro(ordem, p)));
        }
        while (missao.getAsteroides().size() < dificuldade.getAsteroides()) {
            colocarEmCelulaLivre(missao, p -> missao.adicionarAsteroide(new Asteroide(p)));
        }
        while (missao.getInimigos().size() < dificuldade.getInimigos()) {
            colocarEmCelulaLivre(missao, p -> missao.adicionarInimigo(new Inimigo(p)));
        }
        return missao;
    }

    /** Sorteia até achar uma célula vazia (a nave começa na origem, então a origem nunca é usada). */
    private void colocarEmCelulaLivre(Missao missao, Consumer<Posicao> colocar) {
        Posicao p;
        do {
            p = missao.getMapa().sortear(random);
        } while (missao.ocupada(p));
        colocar.accept(p);
    }

    /** Ordem fixa da versão original. Com 4 passageiros (Fácil) o Astronauta não aparece. */
    static Passageiro criarPassageiro(int ordem, Posicao p) {
        switch (ordem % 5) {
            case 0: return new Professor("Dr. Silva", p);
            case 1: return new Engenheiro("Eng. Rosa", p);
            case 2: return new Professor("Dr. Lima", p);
            case 3: return new Engenheiro("Eng. Carlos", p);
            default: return new Astronauta("Ast. Maria", p);
        }
    }
}
