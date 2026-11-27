package ensaio;
import br.edu.unifor.missaomarte.pilotagem.*;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
/** Piloto de teste: navega até plataformas por comandos de motor e giro, nunca teleporta. */
public final class PilotoCapituloUm {
    private int fase;
    private int decolagem;
    private double modulacao;
    private PlataformaDePouso anterior;
    private final boolean inverso;
    public PilotoCapituloUm(boolean inverso) {this.inverso=inverso;}
    public Comandos decidir(EncontroDeResgate encontro) {
        var p=encontro.partida();var n=p.getNave();
        if(n.getEstado()==NaveEmVoo.Estado.DESTRUIDA)return Comandos.NENHUM;
        PlataformaDePouso destino=p.getCaverna().getBase();
        if(n.getABordo().isEmpty()) {
            var esperando=p.getCaverna().getPlataformas().stream().filter(t->t.getEsperando().isPresent()).toList();
            if(!esperando.isEmpty()) destino=esperando.get(inverso?esperando.size()-1:0);
        } else if(encontro.antena()!=null && !encontro.antena().ativa()) destino=encontro.antena().plataforma();
        if(destino!=anterior){fase=0;anterior=destino;}
        if(n.getEstado()==NaveEmVoo.Estado.POUSADA) {
            if(n.getPlataforma()==destino) return Comandos.NENHUM;
            if(n.getPlataforma()==p.getCaverna().getBase() && n.getCombustivel()<p.getConfig().tanque()-.1) return Comandos.NENHUM;
            fase=0;decolagem=36;return Comandos.MOTOR;
        }
        if(decolagem>0){decolagem--;return Comandos.MOTOR;}
        double alturaCruzeiro=Math.min(1000,p.getCaverna().getAltura()-350);
        double x=fase==0?n.getX():destino.getCentroX();
        // Durante a subida, conservar o eixo do ponto de origem.
        if(fase==0) {
            if(origemX==null) origemX=n.getX();
            x=origemX;
        }
        if(fase==2) {
            double altura=n.getY()-(destino.getSuperficie()-p.getConfig().peY());
            return conduzir(p,limitar((x-n.getX())*1.5,35),-Math.max(20,Math.min(100,Math.sqrt(70*Math.max(0,altura)))),altura<100?10:25);
        }
        double dx=x-n.getX(),dy=alturaCruzeiro-n.getY();
        if(Math.hypot(dx,dy)<25 && Math.hypot(n.getVelocidadeX(),n.getVelocidadeY())<25){fase++;origemX=null;}
        return conduzir(p,limitar(dx*1.3,Math.min(210,Math.sqrt(120*Math.abs(dx)))),
            limitar(dy*1.3,Math.min(115,Math.sqrt(80*Math.abs(dy))+3)),45);
    }
    private Double origemX;
    private Comandos conduzir(PartidaDePilotagem p, double vx, double vy, double limite) {
        var n = p.getNave(); var c = p.getConfig();
        double ax = (vx - n.getVelocidadeX()) * 2.2;
        double ay = (vy - n.getVelocidadeY()) * 2.2 + c.gravidade();
        double angulo = limitar(Math.toDegrees(Math.atan2(-ax, Math.max(8, ay))), limite);
        double erro = angulo - n.getAngulo();
        double[] bico = n.direcaoDoBico();
        double fracao = Math.max(0, Math.min(1, (ax * bico[0] + ay * bico[1]) / c.empuxo()));
        if (Math.abs(erro) > 50 && n.getVelocidadeY() > -60) fracao = 0;
        modulacao += fracao;
        boolean motor = modulacao >= 1;
        if (motor) modulacao--;
        return new Comandos(erro > 1.5, erro < -1.5, motor);
    }
    private static double limitar(double valor, double limite) { return Math.max(-limite, Math.min(limite, valor)); }
}
