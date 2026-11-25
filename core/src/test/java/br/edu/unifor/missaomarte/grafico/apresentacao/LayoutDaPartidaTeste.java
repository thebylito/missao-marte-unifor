package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * O layout da partida em vários tamanhos de tela (issue #11): nada cortado, nada sobreposto, alvos de toque com o
 * tamanho mínimo. As áreas visíveis são as que o ExtendViewport produz (mínimo 800x600, centro em 400,300).
 */
class LayoutDaPartidaTeste {

    /** Área visível para uma tela de largura x altura pixels (o ExtendViewport mantém 800x600 e estica um lado). */
    private static Retangulo visivel(float larguraPx, float alturaPx) {
        float escala = Math.min(larguraPx / 800f, alturaPx / 600f);
        float w = larguraPx / escala;
        float h = alturaPx / escala;
        return new Retangulo(400 - w / 2, 300 - h / 2, w, h);
    }

    private static final Retangulo DESKTOP = new Retangulo(0, 0, 800, 600);
    private static final Retangulo CELULAR_PAISAGEM = visivel(2340, 1080);   // Galaxy S24 Ultra deitado
    private static final Retangulo CELULAR_RETRATO = visivel(1080, 2340);
    private static final Retangulo EMULADOR_RETRATO = visivel(720, 1280);
    private static final Retangulo TABLET_PAISAGEM = visivel(1024, 768);     // 4:3, o caso mais apertado

    @Test
    void desktopSemToqueFicaIgualAsFasesAnteriores() {
        LayoutDaPartida l = LayoutDaPartida.calcular(DESKTOP, false);
        assertFalse(l.retrato());
        assertEquals(new Retangulo(20, 20, 560, 560), l.mapa());
        assertEquals(new Retangulo(596, 20, 192, 560), l.painel());
        assertTrue(l.botoes().isEmpty());
    }

    @Test
    void retratoPoeOMapaEmCimaOPainelNoMeioEOsBotoesEmbaixo() {
        LayoutDaPartida l = LayoutDaPartida.calcular(CELULAR_RETRATO, true);
        assertTrue(l.retrato());
        assertTrue(l.mapa().y() > l.painel().topo(), "mapa acima do painel");
        for (Retangulo b : l.botoes().values()) {
            assertTrue(b.topo() <= l.painel().y(), "botões abaixo do painel");
        }
        assertEquals(l.mapa().largura(), l.mapa().altura(), 0.01f);
    }

    @Test
    void emTodosOsTamanhosNadaFicaCortadoNemSobreposto() {
        Map<String, Retangulo> telas = Map.of("desktop", DESKTOP, "celular paisagem", CELULAR_PAISAGEM,
                "celular retrato", CELULAR_RETRATO, "emulador retrato", EMULADOR_RETRATO,
                "tablet paisagem", TABLET_PAISAGEM, "janela alta", visivel(600, 1000));
        for (Map.Entry<String, Retangulo> t : telas.entrySet()) {
            for (boolean toque : new boolean[] {false, true}) {
                String caso = t.getKey() + (toque ? " com toque" : " sem toque");
                LayoutDaPartida l = LayoutDaPartida.calcular(t.getValue(), toque);
                List<Retangulo> partes = new ArrayList<>(l.botoes().values());
                partes.add(l.mapa());
                partes.add(l.painel());
                for (Retangulo r : partes) {
                    assertTrue(r.dentroDe(t.getValue()), caso + ": " + r + " fora de " + t.getValue());
                }
                for (int i = 0; i < partes.size(); i++) {
                    for (int j = i + 1; j < partes.size(); j++) {
                        assertFalse(partes.get(i).sobrepoe(partes.get(j)), caso + ": " + partes.get(i) + " x " + partes.get(j));
                    }
                }
                assertTrue(l.mapa().largura() >= 250, caso + ": mapa pequeno demais (" + l.mapa().largura() + ")");
                assertEquals(toque ? Botao.values().length : 0, l.botoes().size(), caso);
            }
        }
    }

    @Test
    void todoBotaoTemOTamanhoMinimoDeAlvo() {
        for (Retangulo tela : List.of(CELULAR_PAISAGEM, CELULAR_RETRATO, EMULADOR_RETRATO, TABLET_PAISAGEM)) {
            for (Retangulo b : LayoutDaPartida.calcular(tela, true).botoes().values()) {
                assertTrue(b.largura() >= LayoutDaPartida.ALVO_MINIMO && b.altura() >= LayoutDaPartida.ALVO_MINIMO,
                        tela + ": " + b);
            }
        }
    }

    @Test
    void abortarFicaAfastadoDoEmbarcar() {
        LayoutDaPartida l = LayoutDaPartida.calcular(CELULAR_PAISAGEM, true);
        Retangulo embarcar = l.botoes().get(Botao.EMBARCAR);
        Retangulo abortar = l.botoes().get(Botao.ABORTAR);
        assertTrue(abortar.y() - embarcar.topo() >= LayoutDaPartida.ALVO_MINIMO, "o botão Som fica entre os dois");
    }

    @Test
    void toqueNoCentroDeUmBotaoAchaOBotaoENoVazioNaoAchaNada() {
        LayoutDaPartida l = LayoutDaPartida.calcular(CELULAR_RETRATO, true);
        for (Map.Entry<Botao, Retangulo> b : l.botoes().entrySet()) {
            assertEquals(Optional.of(b.getKey()), l.botaoEm(b.getValue().centroX(), b.getValue().centroY()));
        }
        Retangulo cima = l.botoes().get(Botao.CIMA);
        // o meio do canto do direcional é vazio (perto da seta, a folga de toque conta como a seta)
        float meioDoCanto = cima.x() - LayoutDaPartida.ESPACO_ENTRE_BOTOES - cima.largura() / 2;
        assertEquals(Optional.empty(), l.botaoEm(meioDoCanto, cima.centroY()), "o canto do direcional é vazio");
        assertEquals(Optional.empty(), l.botaoEm(l.mapa().centroX(), l.mapa().centroY()));
    }

    @Test
    void emPaisagemComToqueOMapaFicaCentralizadoNaVertical() {
        // 16:9: os botões tiram largura, o mapa fica menor que a altura e não deve grudar embaixo
        Retangulo tela = visivel(1280, 720);
        LayoutDaPartida l = LayoutDaPartida.calcular(tela, true);
        assertTrue(l.mapa().altura() < tela.altura() - 2 * LayoutDaPartida.MARGEM, "o caso precisa sobrar altura");
        assertEquals(tela.centroY(), l.mapa().centroY(), 0.01f);
    }

    @Test
    void botaoCresceEmTelasMaioresMasTemLimites() {
        assertEquals(LayoutDaPartida.ALVO_MINIMO, LayoutDaPartida.tamanhoDoBotao(new Retangulo(0, 0, 800, 300)));
        assertEquals(104, LayoutDaPartida.tamanhoDoBotao(CELULAR_RETRATO), 0.5f);
        assertEquals(120, LayoutDaPartida.tamanhoDoBotao(new Retangulo(0, 0, 3000, 3000)));
    }

    // --- folga de toque: o polegar que cai um pouco fora da seta ainda move a nave ---

    @Test
    void tocarUmPoucoForaDoDirecionalAindaContaComoASeta() {
        float folga = LayoutDaPartida.FOLGA_DAS_SETAS;
        for (Retangulo tela : List.of(CELULAR_PAISAGEM, CELULAR_RETRATO)) {
            LayoutDaPartida l = LayoutDaPartida.calcular(tela, true);
            Retangulo cima = l.botoes().get(Botao.CIMA);
            Retangulo baixo = l.botoes().get(Botao.BAIXO);
            Retangulo esquerda = l.botoes().get(Botao.ESQUERDA);
            Retangulo direita = l.botoes().get(Botao.DIREITA);
            // acima da ▲ há pelo menos 16 unidades livres (em paisagem, o Embarcar fica logo depois)
            assertEquals(Optional.of(Botao.CIMA), l.botaoEm(cima.centroX(), cima.topo() + 10), tela.toString());
            assertEquals(Optional.of(Botao.BAIXO), l.botaoEm(baixo.centroX(), baixo.y() - folga + 1), tela.toString());
            assertEquals(Optional.of(Botao.ESQUERDA), l.botaoEm(esquerda.x() - folga + 1, esquerda.centroY()));
            assertEquals(Optional.of(Botao.DIREITA), l.botaoEm(direita.direita() + folga - 1, direita.centroY()));
            assertEquals(Optional.empty(), l.botaoEm(esquerda.x() - folga - 1, esquerda.centroY()), "longe demais");
        }
    }

    @Test
    void toqueDentroDeOutroBotaoVenceAFolgaDaSeta() {
        LayoutDaPartida l = LayoutDaPartida.calcular(CELULAR_PAISAGEM, true);
        Retangulo cima = l.botoes().get(Botao.CIMA);
        Retangulo embarcar = l.botoes().get(Botao.EMBARCAR);
        float y = embarcar.y() + 2;   // dentro do Embarcar e ainda na folga da ▲
        assertTrue(y - cima.topo() <= LayoutDaPartida.FOLGA_DAS_SETAS);
        assertEquals(Optional.of(Botao.EMBARCAR), l.botaoEm(cima.centroX(), y));
    }

    @Test
    void oMeioDoDirecionalNaoApertaNadaEmNenhumaTela() {
        // no tablet o botão é o menor (78): o meio fica a 49 unidades das setas, e a folga tem de ser menor que isso
        for (Retangulo tela : List.of(CELULAR_PAISAGEM, CELULAR_RETRATO, EMULADOR_RETRATO, TABLET_PAISAGEM)) {
            LayoutDaPartida l = LayoutDaPartida.calcular(tela, true);
            float x = l.botoes().get(Botao.CIMA).centroX();
            float y = l.botoes().get(Botao.ESQUERDA).centroY();
            assertEquals(Optional.empty(), l.botaoEm(x, y), tela.toString());
        }
    }

    @Test
    void embarcarSomEAbortarNaoGanhamFolga() {
        LayoutDaPartida l = LayoutDaPartida.calcular(CELULAR_RETRATO, true);
        Retangulo embarcar = l.botoes().get(Botao.EMBARCAR);
        Retangulo abortar = l.botoes().get(Botao.ABORTAR);
        assertEquals(Optional.empty(), l.botaoEm(embarcar.direita() + 5, embarcar.centroY()));
        assertEquals(Optional.empty(), l.botaoEm(abortar.centroX(), abortar.topo() + 5), "Abortar só com toque em cima");
    }
}
