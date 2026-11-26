package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkinDaSombraTeste {
    @Test
    void mantemSkinQueVeioNaMensagem() {
        var sombras = SombrasDaPilotagem.desligadas();
        sombras.receber("""
                {"tipo":"sombra","piloto":7,"x":1,"y":2,"angulo":22.5,"skin":"azul-2"}
                """);
        assertEquals("azul-2", sombras.getSombras().get(0).skin());
    }

    @Test
    void servidorAntigoOmiteSkinEUsaPadrao() {
        var sombras = SombrasDaPilotagem.desligadas();
        sombras.receber("""
                {"tipo":"sombra","piloto":7,"x":1,"y":2,"angulo":22.5}
                """);
        assertEquals("padrao", sombras.getSombras().get(0).skin());
    }
}
