package br.edu.unifor.missaomarte.pilotagem.horda;

/**
 * Uma variação fixa da horda. Faixas do sorteio ({@link SorteioDaHorda}):
 * <ul>
 *   <li>{@link TipoDeModificador#VENTO}: aceleração lateral constante, |valor| de 8 a 20 (sinal = direção,
 *       + para a direita);</li>
 *   <li>{@link TipoDeModificador#GRAVIDADE}: fator de 1,1 a 1,2;</li>
 *   <li>{@link TipoDeModificador#TANQUE}: fator de 0,85 a 1,0, nunca no Difícil.</li>
 * </ul>
 */
public record Modificador(TipoDeModificador tipo, double valor) {}
