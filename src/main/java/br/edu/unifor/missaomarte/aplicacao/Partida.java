package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de uma partida (GRASP Controller): recebe um comando por turno, aplica as regras na ordem
 * da versão original e devolve os eventos. Não lê teclado nem imprime nada.
 */
public class Partida {

    public enum Estado { EM_ANDAMENTO, VITORIA, DERROTA, ABORTADA }

    private final String piloto;
    private final Dificuldade dificuldade;
    private final Missao missao;
    private final Relogio relogio;
    private final long inicioEmMillis;
    private int pontos;
    private int movimentos;
    private long tempoSegundos;
    private Estado estado = Estado.EM_ANDAMENTO;

    public Partida(String piloto, Dificuldade dificuldade, Missao missao, Relogio relogio) {
        this.piloto = piloto;
        this.dificuldade = dificuldade;
        this.missao = missao;
        this.relogio = relogio;
        this.pontos = dificuldade.getPontosIniciais();
        this.inicioEmMillis = relogio.agoraEmMillis();
    }

    /**
     * Um turno: executa o comando; depois os inimigos andam, a colisão é verificada, os pontos são
     * conferidos e, por fim, a condição de vitória.
     */
    public List<Evento> executar(Comando comando) {
        if (estado != Estado.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida já terminou: " + estado);
        }
        List<Evento> eventos = new ArrayList<>();
        Nave nave = missao.getNave();

        switch (comando) {
            case ABORTAR:
                estado = Estado.ABORTADA;
                eventos.add(Evento.de(Evento.Tipo.ABORTADA));
                return eventos;
            case INVALIDO:
                // comando inválido não gasta o turno: inimigos não andam
                eventos.add(Evento.de(Evento.Tipo.COMANDO_INVALIDO));
                return eventos;
            case EMBARCAR:
                embarcar(eventos);
                break;
            default:
                nave.mover(comando.getDirecao().orElseThrow(), missao.getMapa());
                pontos--; // cada movimento gasta 1 ponto, mesmo encostado na borda
                movimentos++;
        }

        missao.moverInimigos();

        if (missao.naveColidiu()) {
            nave.perderVida();
            if (nave.estaDestruida()) {
                estado = Estado.DERROTA;
                eventos.add(Evento.de(Evento.Tipo.NAVE_DESTRUIDA));
            } else {
                eventos.add(new Evento(Evento.Tipo.COLISAO, null, nave.getVidas()));
            }
        }

        if (pontos <= 0) {
            estado = Estado.DERROTA;
            eventos.add(Evento.de(Evento.Tipo.PONTOS_ESGOTADOS));
        }

        if (missao.todosEmbarcados() && estado == Estado.EM_ANDAMENTO) {
            if (missao.naveNaPlataforma()) {
                estado = Estado.VITORIA;
                tempoSegundos = (relogio.agoraEmMillis() - inicioEmMillis) / 1000;
                eventos.add(Evento.de(Evento.Tipo.VITORIA));
            } else {
                eventos.add(Evento.de(Evento.Tipo.RETORNE_A_PLATAFORMA));
            }
        }
        return eventos;
    }

    private void embarcar(List<Evento> eventos) {
        Optional<Passageiro> aqui = missao.passageiroNaPosicaoDaNave();
        if (aqui.isEmpty()) {
            eventos.add(Evento.de(Evento.Tipo.SEM_PASSAGEIRO_NA_POSICAO));
        } else if (missao.embarcar(aqui.get())) {
            Passageiro p = aqui.get();
            pontos += p.getPontuacao();
            eventos.add(new Evento(Evento.Tipo.PASSAGEIRO_EMBARCADO, p.getNome(), p.getPontuacao()));
        } else {
            eventos.add(Evento.de(Evento.Tipo.NAVE_CHEIA));
        }
    }

    /** Disponível depois da vitória. */
    public ResultadoPartida getResultado() {
        if (estado != Estado.VITORIA) {
            throw new IllegalStateException("Só há resultado depois da vitória.");
        }
        return new ResultadoPartida(piloto, dificuldade, pontos, movimentos, tempoSegundos,
                missao.getNave().getPassageiros().size());
    }

    public boolean emAndamento() { return estado == Estado.EM_ANDAMENTO; }
    public Estado getEstado() { return estado; }
    public String getPiloto() { return piloto; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public Missao getMissao() { return missao; }
    public int getPontos() { return pontos; }
    public int getMovimentos() { return movimentos; }
}
