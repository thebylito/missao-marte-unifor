package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPilotagem.Botao;
import br.edu.unifor.missaomarte.grafico.audio.Som;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import com.badlogic.gdx.Input.Keys;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Liga o teclado e os dedos à {@link PartidaDePilotagem} (Fase 7, padrão Humble Object, como o
 * {@link ControladorDaPartida}): toda decisão da tela da pilotagem fica aqui, testável sem abrir janela.
 *
 * <p>Na pilotagem os comandos valem enquanto estão apertados (não é um turno por tecla). O controlador guarda as
 * teclas apertadas e onde está cada dedo (cada toque tem um número), e a cada quadro traduz isso em {@link Comandos}.
 * Assim dá para girar com um polegar e acelerar com o outro.
 */
public class ControladorDaPilotagem {

    /** Quanto tempo uma mensagem fica na tela, em segundos. */
    static final float DURACAO_DA_MENSAGEM = 2.5f;

    private final PartidaDePilotagem partida;
    private final ServicoRanking servicoRanking;
    private final Ranking rankingAntes;
    private final Long horda;
    private final String idDaCaverna;
    private final String bilhete;
    private final Set<Integer> teclas = new HashSet<>();
    private final Map<Integer, float[]> dedos = new HashMap<>();
    private LayoutDaPilotagem layout = new LayoutDaPilotagem(new Retangulo(0, 0, 0, 0), Map.of());
    private String mensagem = "";
    private float tempoDaMensagem;
    private FimDePartida fim;

    public ControladorDaPilotagem(PartidaDePilotagem partida, ServicoRanking servicoRanking) {
        this.partida = partida;
        this.servicoRanking = servicoRanking;
        this.horda = null;
        this.idDaCaverna = "marciana";
        this.bilhete = null;
        this.rankingAntes = servicoRanking.carregar();   // como no clássico: para o recorde e para o Top 5
    }

    ControladorDaPilotagem(PartidaDePilotagem partida, long horda, String bilhete, String idDaCaverna) {
        this.partida = partida;
        this.horda = horda;
        this.idDaCaverna = idDaCaverna;
        this.bilhete = bilhete;
        this.servicoRanking = null;
        this.rankingAntes = Ranking.vazio();
    }

    public String getIdDaCaverna() { return idDaCaverna; }

    public boolean noPocoPolar() {
        return "poco-polar".equals(idDaCaverna);
    }

    public String sala() {
        return horda == null ? "livre" : "horda-" + horda;
    }

    // ------------------------------------------------------------------ entrada

    /** Onde estão os botões agora (a tela recalcula a cada mudança de tamanho). */
    public void usarLayout(LayoutDaPilotagem novo) {
        this.layout = novo;
    }

    /** Esc aborta; as outras teclas ficam apertadas até serem soltas. */
    public void teclaPressionada(int tecla) {
        if (tecla == Keys.ESCAPE) {
            abortar();
            return;
        }
        teclas.add(tecla);
    }

    public void teclaSolta(int tecla) {
        teclas.remove(tecla);
    }

    /** Um dedo tocou a tela (coordenadas da tela). O botão Abortar age na hora; os outros valem enquanto apertados. */
    public void tocou(int dedo, float x, float y) {
        if (layout.botaoEm(x, y).orElse(null) == Botao.ABORTAR) {
            abortar();
            return;
        }
        dedos.put(dedo, new float[] {x, y});
    }

    /** O dedo escorregou: o botão que vale é o que está embaixo dele agora. */
    public void arrastou(int dedo, float x, float y) {
        if (dedos.containsKey(dedo)) {
            dedos.put(dedo, new float[] {x, y});
        }
    }

    public void soltou(int dedo) {
        dedos.remove(dedo);
    }

    /** Perda de foco ou mudança de geometria invalida as entradas mantidas. */
    public void limparComandos() {
        teclas.clear();
        dedos.clear();
    }

    public void abortar() {
        partida.abortar();
    }

    /** Os comandos deste instante: tecla apertada ou algum dedo em cima do botão. */
    public Comandos comandos() {
        boolean esquerda = apertada(Keys.LEFT, Keys.A) || dedoEm(Botao.ESQUERDA);
        boolean direita = apertada(Keys.RIGHT, Keys.D) || dedoEm(Botao.DIREITA);
        boolean motor = apertada(Keys.UP, Keys.W, Keys.SPACE) || dedoEm(Botao.MOTOR);
        return new Comandos(esquerda, direita, motor);
    }

    private boolean apertada(int... algumas) {
        for (int t : algumas) {
            if (teclas.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /** Algum dedo está no botão, contando a folga de toque das setas (para a tela acender o botão também). */
    public boolean dedoEm(Botao botao) {
        for (float[] ponto : dedos.values()) {
            if (layout.botaoEm(ponto[0], ponto[1]).orElse(null) == botao) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ tempo

    /** Um quadro: avança a partida com os comandos de agora e devolve o que aconteceu. */
    public List<EventoDeVoo> atualizar(float segundos) {
        tempoDaMensagem = Math.max(0, tempoDaMensagem - segundos);
        if (fim != null) {
            return List.of();
        }
        avancarSimulacao(segundos);
        List<EventoDeVoo> eventos = partida.consumirEventos();
        for (EventoDeVoo e : eventos) {
            if (!e.mensagem().isEmpty()) {
                mensagem = switch (e.tipo()) {
                    case PERIGO_INICIOU -> TextosDaInterface.nomeDoEvento(TipoDeEvento.valueOf(e.mensagem()));
                    case PERIGO_TERMINOU -> "Fim do perigo: " + TextosDaInterface.nomeDoEvento(TipoDeEvento.valueOf(e.mensagem()));
                    case VIP_PEGO -> "Pouso na cápsula! Aguardando confirmação.";
                    default -> e.mensagem();
                };
                tempoDaMensagem = DURACAO_DA_MENSAGEM;
            }
        }
        if (partida.getEstado() != Partida.Estado.EM_ANDAMENTO) {
            fim = concluir();
        }
        return eventos;
    }

    protected void avancarSimulacao(float segundos) {
        partida.atualizar(segundos, comandos());
    }

    /** O som de cada acontecimento do voo (o fim da partida tem o seu, tocado pela tela de fim). */
    public static Optional<Som> som(EventoDeVoo.Tipo tipo) {
        switch (tipo) {
            case POUSOU: return Optional.of(Som.MOVER);
            case EMBARCOU:
            case ENTREGOU: return Optional.of(Som.EMBARQUE);
            case NAVE_CHEIA: return Optional.of(Som.INVALIDO);
            case COMBUSTIVEL_BAIXO: return Optional.of(Som.ALERTA);
            case EXPLODIU: return Optional.of(Som.EXPLOSAO);
            default: return Optional.empty();
        }
    }

    private FimDePartida concluir() {
        Partida.Estado estado = partida.getEstado();
        FimDePartida.ResumoDoVoo voo = new FimDePartida.ResumoDoVoo(partida.getPontosDosResgates(),
                partida.getBonus(), partida.getPousos());
        PartidaParaEnviar paraOServidor = new PartidaParaEnviar(estado.name(), partida.getDificuldade().name(),
                Math.max(0, partida.getPontos()), partida.getEntregues(), partida.getPousos(),
                (long) partida.getTempo(), horda == null ? Modo.PILOTAGEM.name() : "HORDA", bilhete, horda);
        if (estado != Partida.Estado.VITORIA) {
            Evento ultimo = Evento.de(estado == Partida.Estado.ABORTADA ? Evento.Tipo.ABORTADA
                    : Evento.Tipo.NAVE_DESTRUIDA);
            return new FimDePartida(estado, List.of(ultimo), Optional.empty(), rankingAntes.getLider(), false,
                    Optional.empty(), paraOServidor, Modo.PILOTAGEM, Optional.of(voo));
        }
        ResultadoPartida resultado = partida.getResultado().orElseThrow();
        if (horda != null) {
            return new FimDePartida(estado, List.of(Evento.de(Evento.Tipo.VITORIA)), Optional.of(resultado),
                    Optional.empty(), false, Optional.empty(), paraOServidor, Modo.PILOTAGEM, Optional.of(voo));
        }
        ServicoRanking.Registro registro = servicoRanking.registrarVitoria(rankingAntes, resultado);
        return new FimDePartida(estado, List.of(Evento.de(Evento.Tipo.VITORIA)), Optional.of(resultado),
                rankingAntes.getLider(), registro.entrou(), Optional.ofNullable(registro.erroAoSalvar()),
                paraOServidor, Modo.PILOTAGEM, Optional.of(voo));
    }

    // ------------------------------------------------------------------ leitura

    public String tituloDoVoo() {
        return horda == null ? "PILOTAGEM" : "HORDA " + horda;
    }

    public PartidaDePilotagem getPartida() {
        return partida;
    }

    /** A última mensagem, enquanto ainda deve aparecer (senão vazia). */
    public String getMensagem() {
        return tempoDaMensagem > 0 ? mensagem : "";
    }

    /** Presente quando a partida terminou: a tela deve ir para o fim de partida. */
    public Optional<FimDePartida> getFim() {
        return Optional.ofNullable(fim);
    }
}
