package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.aplicacao.*;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaPilotagem;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import java.util.List;

/** Comandos do protótipo, com resultado temporário. */
public final class ControladorDoEncontro extends ControladorDaPilotagem {
    private final EncontroDeResgate encontro;
    private final String titulo;
    private boolean pulso;
    private boolean pulsoPendente;
    private boolean assistido;

    public ControladorDoEncontro(EncontroDeResgate encontro) { this(encontro, "ENCONTRO PROTÓTIPO"); }

    public ControladorDoEncontro(EncontroDeResgate encontro, String titulo) {
        super(encontro.partida(), new ServicoRanking(new RepositorioRanking() {
            private List<RegistroRanking> registros = List.of();
            public List<RegistroRanking> carregar() { return registros; }
            public void salvar(List<RegistroRanking> novos) { registros = List.copyOf(novos); }
            public void apagar() { registros = List.of(); }
        }, Relogio.DO_SISTEMA));
        this.encontro=encontro;
        this.titulo=titulo;
    }

    @Override protected void avancarSimulacao(float segundos) {
        double antes = encontro.partida().getTempo();
        encontro.atualizar(segundos, comandos(), pulso || pulsoPendente, assistido);
        if (encontro.partida().getTempo() > antes) pulsoPendente = false;
    }

    @Override public String tituloDoVoo() { return titulo; }
    public void pulso(boolean ligado) {
        if (ligado && !pulso) pulsoPendente = true;
        pulso = ligado;
    }

    @Override public void limparComandos() {
        super.limparComandos();
        pulso = false;
        pulsoPendente = false;
    }
    public void alternarAssistencia() { assistido=!assistido; }
    public boolean assistido() { return assistido; }
}
