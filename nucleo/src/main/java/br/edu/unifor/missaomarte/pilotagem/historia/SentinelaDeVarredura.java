package br.edu.unifor.missaomarte.pilotagem.historia;

/** Torre estacionária: varre, anuncia o disparo, atira e dá tempo para cruzar. */
public final class SentinelaDeVarredura {
    public enum Estado { VARRENDO, AVISO, ESPERA, DESATIVADO, RECUPERANDO }
    private final double x,y;
    private Estado estado=Estado.VARRENDO;
    private double tempo, ciclo, direcaoDoAlvo=180;
    public SentinelaDeVarredura(double x,double y) { this.x=x;this.y=y; }
    public boolean atualizar(double dt,double alvoX,double alvoY,boolean visivel) {
        if(!Double.isFinite(dt) || dt<=0) return false;
        tempo+=dt;ciclo+=dt;
        if(estado==Estado.DESATIVADO) { if(tempo>=4) trocar(Estado.RECUPERANDO);return false; }
        if(estado==Estado.RECUPERANDO) { if(tempo>=1) trocar(Estado.VARRENDO);return false; }
        if(estado==Estado.ESPERA) { if(tempo>=2) trocar(Estado.VARRENDO);return false; }
        double dx=alvoX-x,dy=alvoY-y;
        direcaoDoAlvo=Math.toDegrees(Math.atan2(dy,dx));
        boolean alcance=visivel && Math.hypot(dx,dy)<=520;
        if(estado==Estado.AVISO) {
            if(!alcance) trocar(Estado.VARRENDO);
            else if(tempo+1e-9>=1) {trocar(Estado.ESPERA);return true;}
        } else {
            double direcao=Math.toDegrees(Math.atan2(dy,dx));
            double erro=((direcao-angulo()+540)%360)-180;
            if(alcance && Math.abs(erro)<=12)trocar(Estado.AVISO);
        }
        return false;
    }
    private void trocar(Estado novo) { estado=novo;tempo=0; }
    public void desativar() { trocar(Estado.DESATIVADO); }
    public boolean vulneravel() { return estado!=Estado.DESATIVADO && estado!=Estado.RECUPERANDO; }
    public Estado estado(){return estado;}
    public double x(){return x;}
    public double y(){return y;}
    public double angulo(){return estado==Estado.AVISO || estado==Estado.ESPERA ? direcaoDoAlvo : 180+50*Math.sin(ciclo*.7);}
}
