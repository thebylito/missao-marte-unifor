package br.edu.unifor.missaomarte.servidor.loja;

import java.util.List;

/** Fase 8: o Hangar do jogador. {@code skins} são os ids que ele tem ({@code padrao} sempre primeiro). */
public record Hangar(int saldo, List<String> skins, String equipada) {
}
