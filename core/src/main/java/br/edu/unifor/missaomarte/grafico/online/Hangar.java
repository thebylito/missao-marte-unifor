package br.edu.unifor.missaomarte.grafico.online;

import java.util.List;

/** Saldo e posse confirmados pelo servidor. A resposta de compra omite a skin equipada. */
public record Hangar(int saldo, List<String> skins, String equipada) {
    public Hangar {
        skins = List.copyOf(skins);
    }
}
