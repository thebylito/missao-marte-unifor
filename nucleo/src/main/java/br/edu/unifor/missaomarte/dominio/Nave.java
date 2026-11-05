package br.edu.unifor.missaomarte.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A nave do jogador: posição, vidas e passageiros a bordo. */
public class Nave {

    public static final int VIDAS_INICIAIS = 3;

    private final int capacidade;
    private final List<Passageiro> passageiros = new ArrayList<>();
    private Posicao posicao = Posicao.ORIGEM;
    private int vidas = VIDAS_INICIAIS;

    public Nave(int capacidade) {
        this.capacidade = capacidade;
    }

    public Posicao getPosicao() { return posicao; }
    public int getCapacidade() { return capacidade; }
    public int getVidas() { return vidas; }

    /** Somente leitura: embarcar é responsabilidade da própria nave. */
    public List<Passageiro> getPassageiros() {
        return Collections.unmodifiableList(passageiros);
    }

    public boolean estaEm(Posicao p) {
        return posicao.equals(p);
    }

    /** Anda uma célula; se o destino estiver fora do mapa, a nave fica onde está. */
    public void mover(Direcao direcao, Mapa mapa) {
        Posicao destino = direcao.aplicar(posicao);
        if (mapa.contem(destino)) {
            posicao = destino;
        }
    }

    /** Embarca se houver lugar. */
    public boolean embarcar(Passageiro p) {
        if (passageiros.size() < capacidade) {
            passageiros.add(p);
            return true;
        }
        return false;
    }

    public void perderVida() {
        if (vidas > 0) {
            vidas--;
        }
    }

    public boolean estaDestruida() {
        return vidas == 0;
    }
}
