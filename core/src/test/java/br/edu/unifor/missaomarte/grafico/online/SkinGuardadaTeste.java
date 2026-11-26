package br.edu.unifor.missaomarte.grafico.online;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkinGuardadaTeste {
    @Test
    void recuperaSkinSomenteComASessaoQueAGuardou() {
        var guardada = SessaoGuardada.emMemoria();
        String servidor = "http://localhost:8080";
        guardada.guardar(new Sessao("Ana", "token", null, "azul"), servidor);
        assertEquals("azul", guardada.recuperar(servidor).orElseThrow().skin());
        guardada.guardar(new Sessao("Bia", "outro-token"), servidor);
        assertEquals("padrao", guardada.recuperar(servidor).orElseThrow().skin());
    }
}
