package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Onde fica uma plataforma: {@code x} é a borda esquerda e {@code superficie} a altura em que os pés da nave tocam.
 * O corpo sólido fica de {@code superficie - altura} até {@code superficie}; a largura e a altura vêm do tipo da
 * plataforma (base ou passageiro, ver {@link ValidadorDeCaverna}).
 */
public record PontoDePouso(double x, double superficie) {}
