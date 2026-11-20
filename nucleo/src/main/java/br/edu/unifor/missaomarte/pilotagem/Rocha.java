package br.edu.unifor.missaomarte.pilotagem;

/** Bloco sólido da caverna (paredes e corpo das plataformas). Canto inferior esquerdo em (x, y). */
public record Rocha(double x, double y, double largura, double altura) {

    public double direita() { return x + largura; }
    public double topo() { return y + altura; }

    public boolean contem(double px, double py) {
        return px >= x && px <= direita() && py >= y && py <= topo();
    }

    /** O círculo invade a rocha (encostar não conta). */
    public boolean tocaCirculo(double cx, double cy, double raio) {
        double maisPertoX = Math.max(x, Math.min(cx, direita()));
        double maisPertoY = Math.max(y, Math.min(cy, topo()));
        double dx = cx - maisPertoX;
        double dy = cy - maisPertoY;
        return dx * dx + dy * dy < raio * raio;
    }
}
