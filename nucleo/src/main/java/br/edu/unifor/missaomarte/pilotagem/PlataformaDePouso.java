package br.edu.unifor.missaomarte.pilotagem;

import br.edu.unifor.missaomarte.dominio.Passageiro;
import java.util.Optional;

/**
 * Onde a nave pode pousar: a base (entrega e reabastecimento), a plataforma de um passageiro ou a cápsula VIP. A superfície é a
 * altura em que os pés tocam; o corpo sólido fica abaixo dela.
 */
public final class PlataformaDePouso {

    public enum Tipo { BASE, PASSAGEIRO, VIP, INSTALACAO }

    private final Tipo tipo;
    private final double x;
    private final double superficie;
    private final double largura;
    private final double altura;
    private Passageiro esperando;

    public PlataformaDePouso(Tipo tipo, double x, double superficie, double largura, double altura) {
        this.tipo = tipo;
        this.x = x;
        this.superficie = superficie;
        this.largura = largura;
        this.altura = altura;
    }

    public Tipo getTipo() { return tipo; }
    public double getX() { return x; }
    public double getSuperficie() { return superficie; }
    public double getLargura() { return largura; }
    public double getAltura() { return altura; }
    public double getCentroX() { return x + largura / 2; }

    /** Quem espera resgate aqui (vazio na base e depois do embarque). */
    public Optional<Passageiro> getEsperando() {
        return Optional.ofNullable(esperando);
    }

    /** O ponto x está sobre a plataforma (bordas incluídas). */
    public boolean cobre(double px) {
        return px >= x && px <= x + largura;
    }

    public Rocha corpo() {
        return new Rocha(x, superficie - altura, largura, altura);
    }

    void receber(Passageiro passageiro) {
        this.esperando = passageiro;
    }

    Passageiro retirar() {
        Passageiro saindo = esperando;
        esperando = null;
        return saindo;
    }
}
