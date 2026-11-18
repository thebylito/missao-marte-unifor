package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Comando;
import br.edu.unifor.missaomarte.dominio.Direcao;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.grafico.apresentacao.AnimacaoDoTurno.Instantaneo;
import br.edu.unifor.missaomarte.grafico.apresentacao.ReacaoAosEventos.Reacao;
import br.edu.unifor.missaomarte.grafico.audio.Sons;
import com.badlogic.gdx.Input.Keys;
import java.util.Optional;

/**
 * Tudo o que a tela de partida decide, sem OpenGL (Humble Object): repassa a tecla ao {@link ControladorDaPartida},
 * guarda a foto de antes e de depois do turno para a animação, toca os sons e dispara os efeitos a partir dos
 * eventos, e só libera a tela de fim depois que a explosão termina. A {@code TelaPartida} apenas desenha o que
 * esta classe expõe.
 */
public final class CenaDaPartida {

    private final ControladorDaPartida controlador;
    private final Sons sons;
    private final AnimacaoDoTurno animacao;
    private final EfeitosDaPartida efeitos = new EfeitosDaPartida();
    private Direcao direcaoDaNave = Direcao.CIMA;
    private float relogio;

    public CenaDaPartida(ControladorDaPartida controlador, Sons sons) {
        this.controlador = controlador;
        this.sons = sons;
        this.animacao = new AnimacaoDoTurno(Instantaneo.de(missao()));
    }

    /** Devolve true se a tecla virou um turno. M liga e desliga o som e não gasta turno (não é comando do jogo). */
    public boolean teclaPressionada(int tecla) {
        if (tecla == Keys.M) {
            sons.alternarMudo();
            return false;
        }
        Instantaneo antes = Instantaneo.de(missao());
        Optional<Comando> comando = MapaDeTeclas.comandoPara(tecla);
        if (!controlador.teclaPressionada(tecla)) {
            return false;
        }
        comando.get().getDirecao().ifPresent(d -> direcaoDaNave = d);
        animacao.iniciar(antes, Instantaneo.de(missao()));
        Reacao reacao = ReacaoAosEventos.para(comando.get(), controlador.getUltimosEventos());
        reacao.sons().forEach(sons::tocar);
        efeitos.aplicar(reacao.impacto(), missao().getNave().getPosicao());
        return true;
    }

    /** Chamado a cada quadro com o tempo desde o anterior. */
    public void avancar(float delta) {
        relogio += delta;
        animacao.avancar(delta);
        efeitos.avancar(delta);
    }

    /** O fim da partida, quando já pode ser mostrado: espera o deslize e a explosão terminarem. */
    public Optional<FimDePartida> fimParaMostrar() {
        if (animacao.emAndamento() || efeitos.emAndamento()) {
            return Optional.empty();
        }
        return controlador.getFim();
    }

    private Missao missao() {
        return controlador.getPartida().getMissao();
    }

    public ControladorDaPartida getControlador() { return controlador; }
    public AnimacaoDoTurno getAnimacao() { return animacao; }
    public EfeitosDaPartida getEfeitos() { return efeitos; }
    /** Para onde a nave aponta: a direção do último movimento (começa para cima). */
    public Direcao getDirecaoDaNave() { return direcaoDaNave; }
    /** Segundos desde o início da partida, para as animações em laço (passageiros, asteroides, inimigos). */
    public float getRelogio() { return relogio; }
    public boolean isSomLigado() { return !sons.isMudo(); }
}
