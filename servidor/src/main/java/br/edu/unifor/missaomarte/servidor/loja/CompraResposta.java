package br.edu.unifor.missaomarte.servidor.loja;

import java.util.List;

/** Fase 8: o resultado de uma compra: o saldo depois dela e as skins que o jogador tem agora. */
public record CompraResposta(int saldo, List<String> skins) {
}
