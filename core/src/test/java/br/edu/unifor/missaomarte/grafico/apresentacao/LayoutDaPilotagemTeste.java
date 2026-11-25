package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** CT-21: os botões da pilotagem em vários tamanhos de tela (as mesmas áreas que o ExtendViewport produz). */
class LayoutDaPilotagemTeste {

    private static final double PROPORCAO = 1440.0 / 3840.0;

    private static Retangulo visivel(float larguraPx, float alturaPx) {
        float escala = Math.min(larguraPx / 800f, alturaPx / 600f);
        float w = larguraPx / escala;
        float h = alturaPx / escala;
        return new Retangulo(400 - w / 2, 300 - h / 2, w, h);
    }

    private static final List<Retangulo> TELAS = List.of(
            new Retangulo(0, 0, 800, 600), visivel(2340, 1080), visivel(1080, 2340), visivel(720, 1280),
            visivel(1280, 720), visivel(1024, 768));

    @Test
    void emTodosOsTamanhosNadaFicaCortadoNemSobrepostoEOsAlvosTemOTamanhoMinimo() {
        for (Retangulo tela : TELAS) {
            LayoutDaPilotagem l = LayoutDaPilotagem.calcular(tela, true, PROPORCAO);
            assertEquals(4, l.botoes().size(), tela.toString());
            List<Retangulo> partes = new ArrayList<>(l.botoes().values());
            partes.add(l.minimapa());
            for (Retangulo r : partes) {
                assertTrue(r.dentroDe(tela), tela + ": " + r + " fora da tela");
            }
            for (int i = 0; i < partes.size(); i++) {
                for (int j = i + 1; j < partes.size(); j++) {
                    assertFalse(partes.get(i).sobrepoe(partes.get(j)), tela + ": " + partes.get(i) + " x " + partes.get(j));
                }
            }
            for (Map.Entry<Botao, Retangulo> b : l.botoes().entrySet()) {
                assertTrue(b.getValue().largura() >= LayoutDaPartida.ALVO_MINIMO
                        && b.getValue().altura() >= LayoutDaPartida.ALVO_MINIMO, tela + ": " + b);
            }
        }
    }

    @Test
    void girarFicaEmbaixoAEsquerdaMotorEmbaixoADireitaEAbortarNoAlto() {
        Retangulo tela = visivel(2340, 1080);
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(tela, true, PROPORCAO);
        Retangulo esquerda = l.botoes().get(Botao.ESQUERDA);
        Retangulo direita = l.botoes().get(Botao.DIREITA);
        Retangulo motor = l.botoes().get(Botao.MOTOR);
        Retangulo abortar = l.botoes().get(Botao.ABORTAR);
        assertTrue(esquerda.direita() <= direita.x(), "◄ antes de ►");
        assertTrue(direita.direita() < tela.centroX() && motor.x() > tela.centroX(), "um polegar para cada lado");
        assertTrue(motor.largura() > direita.largura(), "o motor é o maior");
        assertTrue(abortar.y() > tela.centroY(), "Abortar longe dos polegares");
        assertTrue(abortar.topo() <= l.minimapa().y(), "embaixo do minimapa");
    }

    @Test
    void semToqueNaoHaBotoesSoOMinimapaComAProporcaoDaCaverna() {
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(new Retangulo(0, 0, 800, 600), false, PROPORCAO);
        assertTrue(l.botoes().isEmpty());
        assertEquals(PROPORCAO, l.minimapa().altura() / l.minimapa().largura(), 1e-4);
        assertEquals(Optional.empty(), l.botaoEm(400, 300));
    }

    @Test
    void oBotaoCresceComATelaMasTemLimites() {
        assertEquals(LayoutDaPartida.ALVO_MINIMO, LayoutDaPilotagem.tamanhoDoBotao(new Retangulo(0, 0, 800, 300)));
        assertEquals(128, LayoutDaPilotagem.tamanhoDoBotao(visivel(1080, 2340)), 0.5f);
        assertEquals(140, LayoutDaPilotagem.tamanhoDoBotao(new Retangulo(0, 0, 3000, 3000)));
    }

    @Test
    void toqueNoCentroDeCadaBotaoAchaOBotao() {
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(visivel(1080, 2340), true, PROPORCAO);
        for (Map.Entry<Botao, Retangulo> b : l.botoes().entrySet()) {
            assertEquals(Optional.of(b.getKey()), l.botaoEm(b.getValue().centroX(), b.getValue().centroY()));
        }
        assertEquals(Optional.empty(), l.botaoEm(400, 300), "o meio da tela é do voo");
    }

    // --- folga de toque: o polegar que cai um pouco fora da seta ainda gira a nave ---

    @Test
    void tocarUmPoucoForaDaSetaAindaContaComoASeta() {
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(visivel(2340, 1080), true, PROPORCAO);
        Retangulo esquerda = l.botoes().get(Botao.ESQUERDA);
        Retangulo direita = l.botoes().get(Botao.DIREITA);
        float folga = LayoutDaPilotagem.FOLGA_DAS_SETAS;
        assertEquals(Optional.of(Botao.ESQUERDA), l.botaoEm(esquerda.x() - folga + 1, esquerda.centroY()), "à esquerda");
        assertEquals(Optional.of(Botao.ESQUERDA), l.botaoEm(esquerda.centroX(), esquerda.topo() + folga - 1), "acima");
        assertEquals(Optional.of(Botao.DIREITA), l.botaoEm(direita.centroX(), direita.y() - folga + 1), "abaixo");
        assertEquals(Optional.of(Botao.DIREITA), l.botaoEm(direita.direita() + 10, direita.topo() + 10), "na diagonal");
        assertEquals(Optional.empty(), l.botaoEm(esquerda.centroX(), esquerda.topo() + folga + 1), "longe demais");
    }

    @Test
    void noVaoEntreAsSetasVenceAMaisProxima() {
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(visivel(2340, 1080), true, PROPORCAO);
        Retangulo esquerda = l.botoes().get(Botao.ESQUERDA);
        Retangulo direita = l.botoes().get(Botao.DIREITA);
        assertEquals(Optional.of(Botao.ESQUERDA), l.botaoEm(esquerda.direita() + 2, esquerda.centroY()));
        assertEquals(Optional.of(Botao.DIREITA), l.botaoEm(direita.x() - 2, direita.centroY()));
    }

    @Test
    void motorEAbortarNaoGanhamFolga() {
        LayoutDaPilotagem l = LayoutDaPilotagem.calcular(visivel(2340, 1080), true, PROPORCAO);
        Retangulo motor = l.botoes().get(Botao.MOTOR);
        Retangulo abortar = l.botoes().get(Botao.ABORTAR);
        assertEquals(Optional.empty(), l.botaoEm(motor.x() - 5, motor.centroY()));
        assertEquals(Optional.empty(), l.botaoEm(abortar.x() - 5, abortar.centroY()), "Abortar só com toque em cima");
    }
}
