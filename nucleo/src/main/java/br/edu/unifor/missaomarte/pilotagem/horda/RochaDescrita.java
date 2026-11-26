package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Uma rocha da caverna, como dado (lida do JSON pelo servidor e pelo cliente). Canto inferior esquerdo em (x, y),
 * com y para cima, como a {@link br.edu.unifor.missaomarte.pilotagem.Rocha}.
 */
public record RochaDescrita(double x, double y, double largura, double altura) {}
