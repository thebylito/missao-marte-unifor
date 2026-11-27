package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.historia.MissaoHistoria;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import com.badlogic.gdx.graphics.Color;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;

/** Primeira missão autoral, reaproveitando voo e combate já validados. */
public final class TelaMissaoHistoria extends TelaEncontro {
    private final ServicoHistoria servico;
    public TelaMissaoHistoria(MissaoMarteGame jogo, ServicoHistoria servico) {
        this(jogo, servico, MissaoHistoria.primeiroSinal());
    }
    private TelaMissaoHistoria(MissaoMarteGame jogo, ServicoHistoria servico, EncontroDeResgate encontro) {
        super(jogo, encontro, new ControladorDoEncontro(encontro, "M01 — PRIMEIRO SINAL"));
        this.servico = servico;
    }
    @Override protected String objetivoPrincipal() {
        if (getEncontro().partida().getEstado() == br.edu.unifor.missaomarte.aplicacao.Partida.Estado.VITORIA) return "Resgate concluído: Maria está na base";
        return getEncontro().partida().getNave().getABordo().isEmpty() ? "Objetivo: resgatar Maria na baliza azul"
                : "Objetivo: levar Maria de volta à base";
    }
    @Override protected String ajudaDoEncontro() {
        return "Rota livre acima das rochas; base no marcador verde.";
    }
    @Override protected void desenharElementosExtras() {
        super.desenharElementosExtras();
        var voo = getEncontro().partida();
        var base = voo.getCaverna().getBase();
        var resgate = voo.getCaverna().getPlataformas().get(1);
        int quadro = (int) (voo.getTempo() * 8) % 8;
        lote().setColor(sinalDaBaseAtivo() ? Color.WHITE : Color.GRAY);
        // Marcos de fundo: fora do centro livre reservado ao pouso.
        lote().draw(recursos().quadro(Folha.POSTO_M01, quadro), (float) base.getX() - 70,
                (float) base.getSuperficie(), 180, 90);
        lote().setColor(Color.WHITE);
        if (sinalDaBaseAtivo()) escrever("BASE ATIVA", (float) base.getX() - 45, (float) base.getSuperficie() + 112, Color.LIME, .8f);
        if (resgate.getEsperando().isPresent()) {
            lote().draw(recursos().quadro(Folha.BALIZA_SOS, quadro), (float) resgate.getX() - 35,
                    (float) resgate.getSuperficie(), 48, 96);
        }
    }
    @Override protected void desenharInterfaceExtra() {
        super.desenharInterfaceExtra();
        lote().begin();
        String radio = mensagemDoRadio();
        escreverCentralizado(radio,visivel().y()+254,Color.CYAN,.83f);
        lote().end();
    }
    protected boolean sinalDaBaseAtivo() {
        return getEncontro().partida().getEntregues() > 0 || !getEncontro().partida().getNave().getABordo().isEmpty();
    }
    protected String mensagemDoRadio() {
        if (getEncontro().partida().getEstado() == br.edu.unifor.missaomarte.aplicacao.Partida.Estado.VITORIA) {
            return "MARIA: Pouso confirmado. Obrigada pelo resgate!";
        }
        return getEncontro().partida().getNave().getABordo().isEmpty()
                ? "MARIA: Siga minha baliza. Cuidado com a patrulha."
                : "MARIA: Estou a bordo. Vamos voltar à base!";
    }
    @Override protected void concluirPartida(FimDePartida fim) {
        jogo.trocarTela(new TelaResultadoHistoria(jogo, servico, fim.estado(), getEncontro().partida().getPontos()));
    }
}
