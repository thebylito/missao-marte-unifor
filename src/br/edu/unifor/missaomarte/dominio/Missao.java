package br.edu.unifor.missaomarte.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** O cenário de uma partida: mapa, nave, passageiros e obstáculos, e as regras de ocupação das células. */
public class Missao {

    private final Mapa mapa;
    private final Nave nave;
    private final MovimentoInimigo movimentoInimigo;
    private final List<Passageiro> passageiros = new ArrayList<>();
    private final List<Asteroide> asteroides = new ArrayList<>();
    private final List<Inimigo> inimigos = new ArrayList<>();

    public Missao(Mapa mapa, Nave nave, MovimentoInimigo movimentoInimigo) {
        this.mapa = mapa;
        this.nave = nave;
        this.movimentoInimigo = movimentoInimigo;
    }

    public Mapa getMapa() { return mapa; }
    public Nave getNave() { return nave; }
    public List<Passageiro> getPassageiros() { return Collections.unmodifiableList(passageiros); }
    public List<Asteroide> getAsteroides() { return Collections.unmodifiableList(asteroides); }
    public List<Inimigo> getInimigos() { return Collections.unmodifiableList(inimigos); }

    public void adicionarPassageiro(Passageiro p) { passageiros.add(p); }
    public void adicionarAsteroide(Asteroide a) { asteroides.add(a); }
    public void adicionarInimigo(Inimigo i) { inimigos.add(i); }

    /** Há nave, passageiro, asteroide ou inimigo nesta célula? */
    public boolean ocupada(Posicao p) {
        return nave.estaEm(p)
                || passageiros.stream().anyMatch(x -> x.getPosicao().equals(p))
                || asteroides.stream().anyMatch(x -> x.getPosicao().equals(p))
                || inimigos.stream().anyMatch(x -> x.getPosicao().equals(p));
    }

    /** O primeiro passageiro (na ordem de criação) que está na célula da nave. */
    public Optional<Passageiro> passageiroNaPosicaoDaNave() {
        return passageiros.stream().filter(p -> nave.estaEm(p.getPosicao())).findFirst();
    }

    /** Embarca o passageiro se couber na nave; ele sai do mapa. */
    public boolean embarcar(Passageiro p) {
        if (nave.embarcar(p)) {
            passageiros.remove(p);
            return true;
        }
        return false;
    }

    public void moverInimigos() {
        for (Inimigo i : inimigos) {
            i.mover(movimentoInimigo, mapa);
        }
    }

    public boolean naveColidiu() {
        return asteroides.stream().anyMatch(a -> a.atingiu(nave))
                || inimigos.stream().anyMatch(i -> i.atingiu(nave));
    }

    public boolean todosEmbarcados() {
        return passageiros.isEmpty();
    }

    public boolean naveNaPlataforma() {
        return nave.estaEm(mapa.getPlataforma());
    }
}
