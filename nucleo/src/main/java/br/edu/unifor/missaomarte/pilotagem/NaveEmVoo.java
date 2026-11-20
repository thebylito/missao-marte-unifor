package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.dominio.Passageiro;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado físico da nave na pilotagem. (x, y) é o centro do casco. Ângulo em graus: 0 com o bico para cima,
 * positivo no sentido anti-horário (girar para a esquerda), sempre entre -180 e 180.
 *
 * <p>Os campos são do pacote: só a {@link PartidaDePilotagem} (e os testes do pacote) mudam o estado.
 */
public final class NaveEmVoo {

    public enum Estado { VOANDO, POUSADA, DESTRUIDA }

    private final int capacidade;
    private final List<Passageiro> aBordo = new ArrayList<>();
    double x;
    double y;
    double velocidadeX;
    double velocidadeY;
    double angulo;
    double combustivel;
    Estado estado = Estado.POUSADA;
    PlataformaDePouso plataforma;
    boolean motorLigado;

    NaveEmVoo(int capacidade) {
        this.capacidade = capacidade;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocidadeX() { return velocidadeX; }
    public double getVelocidadeY() { return velocidadeY; }
    public double getAngulo() { return angulo; }
    public double getCombustivel() { return combustivel; }
    public Estado getEstado() { return estado; }
    /** Onde está pousada (null em voo ou destruída). */
    public PlataformaDePouso getPlataforma() { return plataforma; }
    public boolean isMotorLigado() { return motorLigado; }
    public int getCapacidade() { return capacidade; }
    public List<Passageiro> getABordo() { return Collections.unmodifiableList(aBordo); }

    boolean temLugar() {
        return aBordo.size() < capacidade;
    }

    void embarcar(Passageiro passageiro) {
        aBordo.add(passageiro);
    }

    List<Passageiro> desembarcarTodos() {
        List<Passageiro> saindo = new ArrayList<>(aBordo);
        aBordo.clear();
        return saindo;
    }

    /** Um ponto da nave (em coordenadas locais, com o bico para cima) levado para o mundo. */
    public double[] paraMundo(double localX, double localY) {
        double r = Math.toRadians(angulo);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new double[] {x + localX * cos - localY * sin, y + localX * sin + localY * cos};
    }

    /** Para onde o bico aponta (vetor unitário): é a direção do empuxo. */
    public double[] direcaoDoBico() {
        double r = Math.toRadians(angulo);
        return new double[] {-Math.sin(r), Math.cos(r)};
    }
}
