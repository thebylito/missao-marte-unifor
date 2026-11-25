package br.edu.unifor.missaomarte.grafico.apresentacao;

/** Retângulo em coordenadas do mundo da tela (canto inferior esquerdo em x, y). */
public record Retangulo(float x, float y, float largura, float altura) {

    public float direita() { return x + largura; }
    public float topo() { return y + altura; }
    public float centroX() { return x + largura / 2; }
    public float centroY() { return y + altura / 2; }

    public boolean contem(float px, float py) {
        return px >= x && px <= direita() && py >= y && py <= topo();
    }

    /** Distância do ponto até o retângulo: 0 dentro ou na borda (para a folga de toque dos botões). */
    public float distancia(float px, float py) {
        float dx = Math.max(Math.max(x - px, 0), px - direita());
        float dy = Math.max(Math.max(y - py, 0), py - topo());
        return (float) Math.hypot(dx, dy);
    }

    /** Há área em comum (encostar na borda não conta). */
    public boolean sobrepoe(Retangulo outro) {
        return x < outro.direita() && outro.x < direita() && y < outro.topo() && outro.y < topo();
    }

    /** Este retângulo está inteiro dentro do outro. */
    public boolean dentroDe(Retangulo outro) {
        float tolerancia = 0.01f;
        return x >= outro.x - tolerancia && y >= outro.y - tolerancia
                && direita() <= outro.direita() + tolerancia && topo() <= outro.topo() + tolerancia;
    }
}
