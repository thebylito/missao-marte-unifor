package br.edu.unifor.missaomarte.pilotagem.historia;
import br.edu.unifor.missaomarte.dominio.Engenheiro;
import br.edu.unifor.missaomarte.pilotagem.*;

/** Ativação contínua enquanto a engenheira está a bordo e a nave repousa na instalação. */
public final class ObjetivoDaAntena {
    private final PlataformaDePouso plataforma;
    private double segundos;
    private boolean ativa;
    public ObjetivoDaAntena(PlataformaDePouso plataforma) { this.plataforma=plataforma; }
    public void atualizar(double dt,PartidaDePilotagem partida) {
        if (ativa || !Double.isFinite(dt) || dt<=0) return;
        var n=partida.getNave();
        boolean pode=n.getEstado()==NaveEmVoo.Estado.POUSADA && n.getPlataforma()==plataforma
                && n.getABordo().stream().anyMatch(p->p instanceof Engenheiro);
        segundos=pode ? Math.min(3,segundos+dt) : 0;
        if(segundos+1e-9>=3) {ativa=true;partida.cumprirObjetivoAdicional();}
    }
    public boolean ativa(){return ativa;}
    public double progresso(){return segundos/3;}
    public PlataformaDePouso plataforma(){return plataforma;}
}
