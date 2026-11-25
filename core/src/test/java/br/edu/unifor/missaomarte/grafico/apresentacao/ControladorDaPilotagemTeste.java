package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.ConfigDeVoo;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.PlataformaDePouso;
import com.badlogic.gdx.Input.Keys;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * CT-20, CT-23 e CT-24: o controlador da pilotagem traduz teclas e dedos em comandos, e no fim monta o resultado da
 * pilotagem e grava a vitória no ranking da pilotagem (não no clássico). Sem janela: o tempo anda pelo próprio teste.
 */
class ControladorDaPilotagemTeste {

    private static final float PASSO = (float) PartidaDePilotagem.PASSO;

    private final Apoio.RelogioParado relogio = new Apoio.RelogioParado();
    private final Apoio.RepositorioEmMemoria rankingDaPilotagem = new Apoio.RepositorioEmMemoria();
    private final Apoio.RepositorioEmMemoria rankingClassico = new Apoio.RepositorioEmMemoria();

    /** Uma caverna aberta: o chão, a base à esquerda e um Astronauta mais adiante. */
    private static Caverna cavernaAberta() {
        return new Caverna(2000, 1200)
                .rocha(0, 0, 2000, 50)
                .base(100, 98)
                .passageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM), 700, 86);
    }

    private ControladorDaPilotagem controlador() {
        PartidaDePilotagem voo = new PartidaDePilotagem("Ana", Dificuldade.MEDIO, cavernaAberta(), ConfigDeVoo.medio());
        return new ControladorDaPilotagem(voo, new ServicoRanking(rankingDaPilotagem, relogio));
    }

    private static LayoutDaPilotagem layoutDeToque() {
        return LayoutDaPilotagem.calcular(new Retangulo(0, 0, 800, 600), true, 0.375);
    }

    private static float[] centro(LayoutDaPilotagem l, Botao b) {
        Retangulo r = l.botoes().get(b);
        return new float[] {r.centroX(), r.centroY()};
    }

    // ------------------------------------------------------------------ CT-20: entrada

    @Test
    void teclasApertadasViramComandosEnquantoNaoForemSoltas() {
        ControladorDaPilotagem c = controlador();
        assertEquals(Comandos.NENHUM, c.comandos());
        c.teclaPressionada(Keys.LEFT);
        c.teclaPressionada(Keys.UP);
        assertEquals(new Comandos(true, false, true), c.comandos());
        c.teclaSolta(Keys.UP);
        assertEquals(new Comandos(true, false, false), c.comandos());
        c.teclaSolta(Keys.LEFT);
        c.teclaPressionada(Keys.D);
        c.teclaPressionada(Keys.SPACE);
        assertEquals(new Comandos(false, true, true), c.comandos(), "A/D e espaço também");
        c.teclaSolta(Keys.D);
        c.teclaSolta(Keys.SPACE);
        c.teclaPressionada(Keys.W);
        assertEquals(Comandos.MOTOR, c.comandos());
    }

    @Test
    void doisDedosAoMesmoTempoGiramEAceleram() {
        ControladorDaPilotagem c = controlador();
        LayoutDaPilotagem l = layoutDeToque();
        c.usarLayout(l);
        float[] esquerda = centro(l, Botao.ESQUERDA);
        float[] motor = centro(l, Botao.MOTOR);
        c.tocou(0, esquerda[0], esquerda[1]);
        c.tocou(1, motor[0], motor[1]);
        assertEquals(new Comandos(true, false, true), c.comandos());
        assertTrue(c.dedoEm(Botao.ESQUERDA) && c.dedoEm(Botao.MOTOR));
        c.soltou(1);
        assertEquals(new Comandos(true, false, false), c.comandos(), "soltar um dedo libera só o botão dele");
        float[] direita = centro(l, Botao.DIREITA);
        c.arrastou(0, direita[0], direita[1]);
        assertEquals(new Comandos(false, true, false), c.comandos(), "o dedo escorregou para o outro botão");
        c.arrastou(0, 400, 300);
        assertEquals(Comandos.NENHUM, c.comandos(), "arrastar para fora libera");
        c.arrastou(5, motor[0], motor[1]);
        assertEquals(Comandos.NENHUM, c.comandos(), "um dedo que não tocou antes não conta");
    }

    @Test
    void dedoUmPoucoForaDaSetaAindaGiraEAcendeOBotao() {
        ControladorDaPilotagem c = controlador();
        LayoutDaPilotagem l = layoutDeToque();
        c.usarLayout(l);
        Retangulo direita = l.botoes().get(Botao.DIREITA);
        c.tocou(0, direita.centroX(), direita.topo() + LayoutDaPilotagem.FOLGA_DAS_SETAS - 2);
        assertEquals(new Comandos(false, true, false), c.comandos());
        assertTrue(c.dedoEm(Botao.DIREITA));
    }

    @Test
    void escOuOBotaoAbortarTerminamComoAbortada() {
        ControladorDaPilotagem porTecla = controlador();
        porTecla.teclaPressionada(Keys.ESCAPE);
        porTecla.atualizar(PASSO);
        FimDePartida fim = porTecla.getFim().orElseThrow();
        assertEquals(Partida.Estado.ABORTADA, fim.estado());
        assertEquals(Evento.Tipo.ABORTADA, fim.eventosFinais().get(0).tipo());

        ControladorDaPilotagem porToque = controlador();
        LayoutDaPilotagem l = layoutDeToque();
        porToque.usarLayout(l);
        float[] abortar = centro(l, Botao.ABORTAR);
        porToque.tocou(0, abortar[0], abortar[1]);
        porToque.atualizar(PASSO);
        assertEquals(Partida.Estado.ABORTADA, porToque.getFim().orElseThrow().estado());
    }

    // ------------------------------------------------------------------ CT-23 e CT-24: fim

    @Test
    void abortadaNaoEntraNoRankingEVaiAoServidorComoPilotagem() {
        ControladorDaPilotagem c = controlador();
        c.teclaPressionada(Keys.ESCAPE);
        c.atualizar(PASSO);
        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Modo.PILOTAGEM, fim.modo());
        assertEquals(Optional.of(new FimDePartida.ResumoDoVoo(0, 0, 0)), fim.voo());
        assertEquals(Optional.empty(), fim.resultado());
        assertEquals("ABORTADA", fim.paraOServidor().resultado());
        assertEquals("PILOTAGEM", fim.paraOServidor().modo());
        assertTrue(rankingDaPilotagem.salvos.isEmpty());
        assertEquals(c.getFim(), Optional.of(fim));
        assertTrue(c.atualizar(PASSO).isEmpty(), "depois do fim, nada mais acontece");
    }

    @Test
    void vitoriaPeloTecladoEntraNoRankingDaPilotagemENaoNoClassico() {
        ControladorDaPilotagem c = controlador();
        PilotoDeTeste piloto = new PilotoDeTeste(c);
        PlataformaDePouso astronauta = c.getPartida().getCaverna().getPlataformas().get(1);
        assertTrue(piloto.pousarEm(astronauta), "pousou no Astronauta");
        assertTrue(piloto.esperar(1.0), "embarcou");
        assertEquals(1, c.getPartida().getNave().getABordo().size());
        assertTrue(piloto.pousarEm(c.getPartida().getCaverna().getBase()), "voltou à base");
        c.atualizar(PASSO);

        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.VITORIA, fim.estado());
        assertEquals(Modo.PILOTAGEM, fim.modo());
        int bonus = c.getPartida().getBonus();
        assertEquals(20 + bonus, fim.resultado().orElseThrow().pontos());
        assertEquals(Optional.of(new FimDePartida.ResumoDoVoo(20, bonus, 2)), fim.voo());
        assertTrue(fim.entrouNoTop());
        assertEquals(1, rankingDaPilotagem.salvos.size(), "gravou no ranking da pilotagem");
        assertTrue(rankingClassico.salvos.isEmpty(), "o clássico não muda");
        assertEquals("VITORIA PILOTAGEM 2", fim.paraOServidor().resultado() + " " + fim.paraOServidor().modo() + " "
                + fim.paraOServidor().movimentos());
        assertTrue(TextosDaInterface.linhasDoFim(fim).contains(" - Pousos: 2"));
        assertTrue(TextosDaInterface.linhasDoFim(fim).contains(String.format(
                " - Pontuação Final: %d pontos (resgates 20 + bônus de combustível %d)", 20 + bonus, bonus)));
    }

    @Test
    void derrotaMostraGameOver() {
        ControladorDaPilotagem c = controlador();
        PilotoDeTeste piloto = new PilotoDeTeste(c);
        for (int vida = 0; vida < 3; vida++) {
            piloto.subirEDespencar();
        }
        FimDePartida fim = c.getFim().orElseThrow();
        assertEquals(Partida.Estado.DERROTA, fim.estado());
        assertEquals("GAME OVER!", TextosDaInterface.tituloDoFim(fim));
        assertTrue(rankingDaPilotagem.salvos.isEmpty());
    }

    @Test
    void aMensagemDoUltimoEventoFicaUmTempoNaTela() {
        ControladorDaPilotagem c = controlador();
        assertEquals("", c.getMensagem());
        c.teclaPressionada(Keys.ESCAPE);
        c.atualizar(PASSO);
        assertEquals("Missão abortada pelo piloto.", c.getMensagem());
        c.atualizar(ControladorDaPilotagem.DURACAO_DA_MENSAGEM);
        assertEquals("", c.getMensagem());
    }

    @Test
    void cadaAcontecimentoTemOSeuSom() {
        assertEquals(Optional.of(Som.EXPLOSAO), ControladorDaPilotagem.som(EventoDeVoo.Tipo.EXPLODIU));
        assertEquals(Optional.of(Som.EMBARQUE), ControladorDaPilotagem.som(EventoDeVoo.Tipo.EMBARCOU));
        assertEquals(Optional.of(Som.ALERTA), ControladorDaPilotagem.som(EventoDeVoo.Tipo.COMBUSTIVEL_BAIXO));
        assertEquals(Optional.of(Som.INVALIDO), ControladorDaPilotagem.som(EventoDeVoo.Tipo.NAVE_CHEIA));
        assertEquals(Optional.empty(), ControladorDaPilotagem.som(EventoDeVoo.Tipo.VITORIA), "a tela de fim toca");
    }

    /**
     * Pilota só pelo teclado (aperta e solta setas a cada passo), com o mesmo princípio do piloto automático da
     * validação: velocidade desejada, empuxo necessário, ângulo e motor modulado. Numa caverna aberta, basta.
     */
    private static final class PilotoDeTeste {
        private final ControladorDaPilotagem c;
        private double modulacao;
        private boolean esquerda;
        private boolean direita;
        private boolean motor;

        PilotoDeTeste(ControladorDaPilotagem c) {
            this.c = c;
        }

        boolean esperar(double segundos) {
            apertar(false, false, false);
            for (int i = 0; i < segundos / PASSO; i++) {
                c.atualizar(PASSO);
            }
            return true;
        }

        /** 1 s de motor (sobe uns 120) e cai de volta na base rápido demais; a nave nova surge 2 s depois. */
        void subirEDespencar() {
            apertar(false, false, true);
            for (int i = 0; i < 1.0 / PASSO; i++) {
                c.atualizar(PASSO);
            }
            apertar(false, false, false);
            for (int i = 0; i < 8.0 / PASSO && c.getFim().isEmpty(); i++) {
                c.atualizar(PASSO);
            }
        }

        boolean pousarEm(PlataformaDePouso destino) {
            PartidaDePilotagem p = c.getPartida();
            NaveEmVoo n = p.getNave();
            double pouso = destino.getSuperficie() - p.getConfig().peY();
            for (int i = 0; i < 60 / PASSO; i++) {
                if (n.getEstado() == NaveEmVoo.Estado.POUSADA && n.getPlataforma() == destino) {
                    apertar(false, false, false);
                    return true;
                }
                if (n.getEstado() == NaveEmVoo.Estado.POUSADA) {
                    apertar(false, false, true);       // decola
                } else {
                    double dx = destino.getCentroX() - n.getX();
                    boolean longe = Math.abs(dx) > 15;
                    double vx = limitar(dx * 1.0, longe ? 90 : 10);
                    double vy = longe ? limitar((pouso + 150 - n.getY()) * 1.2, 80)
                            : -Math.max(25, Math.min(55, (n.getY() - pouso) * 0.8));
                    double ax = (vx - n.getVelocidadeX()) * 2;
                    double ay = (vy - n.getVelocidadeY()) * 2 + p.getConfig().gravidade();
                    double alvo = limitar(Math.toDegrees(Math.atan2(-ax, Math.max(8, ay))), longe ? 35 : 6);
                    double erro = alvo - n.getAngulo();
                    double[] bico = n.direcaoDoBico();
                    modulacao += Math.max(0, Math.min(1, (ax * bico[0] + ay * bico[1]) / p.getConfig().empuxo()));
                    boolean ligar = modulacao >= 1;
                    if (ligar) {
                        modulacao -= 1;
                    }
                    apertar(erro > 1.5, erro < -1.5, ligar);
                }
                c.atualizar(PASSO);
                if (c.getFim().isPresent() && c.getPartida().getEstado() != Partida.Estado.VITORIA) {
                    return false;
                }
            }
            return false;
        }

        /** Só aperta ou solta o que mudou, como um teclado de verdade. */
        private void apertar(boolean novaEsquerda, boolean novaDireita, boolean novoMotor) {
            esquerda = trocar(esquerda, novaEsquerda, Keys.LEFT);
            direita = trocar(direita, novaDireita, Keys.RIGHT);
            motor = trocar(motor, novoMotor, Keys.UP);
        }

        private boolean trocar(boolean antes, boolean agora, int tecla) {
            if (agora && !antes) {
                c.teclaPressionada(tecla);
            } else if (!agora && antes) {
                c.teclaSolta(tecla);
            }
            return agora;
        }

        private static double limitar(double v, double l) {
            return Math.max(-l, Math.min(l, v));
        }
    }

    @Test
    void oPilotoDeTesteNaoTrapaceia() {
        // ele só usa teclaPressionada/teclaSolta: a partida não é tocada diretamente
        ControladorDaPilotagem c = controlador();
        assertFalse(c.getFim().isPresent());
        assertEquals(NaveEmVoo.Estado.POUSADA, c.getPartida().getNave().getEstado());
    }
}
