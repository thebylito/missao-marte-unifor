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
        assertEquals(Optional.empty(), l.botaoEm(cima.x() - 5, cima.centroY()), "o canto do direcional é vazio");
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
}
