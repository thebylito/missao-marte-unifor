package br.edu.unifor.missaomarte.pilotagem.historia;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.dominio.Astronauta;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.pilotagem.*;
import java.util.ArrayList;
import java.util.List;

/** Primeiro encontro da história. Combate e voo compartilham o passo fixo do núcleo. */
public final class EncontroDeResgate {
    public enum Estado { PATRULHA, AVISO, ESPERA, DESATIVADO, RECUPERANDO }
    public record Projetil(double x, double y) {}

    private static final class Disparo {
        double x, y, idade;
        final double vx, vy;
        Disparo(double x, double y, double vx, double vy) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy;
        }
    }

    private final PartidaDePilotagem partida;
    private final List<Disparo> disparos = new ArrayList<>();
    private Estado estado = Estado.PATRULHA;
    private double x = 800;
    private double y = 540;
    private double limiteEsquerdo=800, limiteDireito=1500;
    private boolean patrulhaAtiva=true;
    private SentinelaDeVarredura sentinela;
    private ObjetivoDaAntena antena;
    private double direcao = 1;
    private double tempoEstado;
    private double acumulador;
    private double intervaloPulso;
    private double invulneravel;
    private double recarga;
    private double pulsoRestante;
    private double pulsoX, pulsoY, pulsoAngulo;
    private int bateria = 4;
    private int escudo = 2;
    private int vidas;
    private int tiros;
    private int impactos;

    public EncontroDeResgate(PartidaDePilotagem partida) {
        this.partida = partida;
        vidas = partida.getVidas();
    }

    public EncontroDeResgate semPatrulha() {patrulhaAtiva=false;return this;}
    public EncontroDeResgate comSentinela(double px,double py) {sentinela=new SentinelaDeVarredura(px,py);return this;}
    public EncontroDeResgate comAntena(PlataformaDePouso plataforma) {
        antena=new ObjetivoDaAntena(plataforma);partida.exigirObjetivoAdicional();return this;
    }
    public SentinelaDeVarredura sentinela(){return sentinela;}
    public ObjetivoDaAntena antena(){return antena;}
    public boolean patrulhaAtiva(){return patrulhaAtiva;}

    public static EncontroDeResgate novo() {
        Caverna caverna = new Caverna(2400, 1200)
                .rocha(0,0,2400,80).rocha(0,1120,2400,80)
                .rocha(0,0,40,1200).rocha(2360,0,40,1200)
                .rocha(1050,80,180,280)
                .base(100,128)
                .passageiro(new Astronauta("Ast. Maria", Posicao.ORIGEM),1850,128);
        return new EncontroDeResgate(new PartidaDePilotagem("Explorador", Dificuldade.FACIL,
                caverna, ConfigDeVoo.para(Dificuldade.FACIL)));
    }

    public void atualizar(double segundos, Comandos comandos, boolean disparar, boolean assistido) {
        if (!Double.isFinite(segundos) || segundos <= 0) return;
        acumulador += Math.min(.25, segundos);
        while (acumulador + 1e-9 >= PartidaDePilotagem.PASSO
                && partida.getEstado() == Partida.Estado.EM_ANDAMENTO) {
            passo(comandos, disparar, assistido);
            acumulador -= PartidaDePilotagem.PASSO;
        }
    }

    private void passo(Comandos comandos, boolean disparar, boolean assistido) {
        double dt = PartidaDePilotagem.PASSO;
        partida.avancarUmPasso(comandos);
        if (partida.getEstado() != Partida.Estado.EM_ANDAMENTO) return;
        intervaloPulso = Math.max(0, intervaloPulso-dt);
        invulneravel = Math.max(0, invulneravel-dt);
        pulsoRestante = Math.max(0, pulsoRestante-dt);
        if (partida.getVidas() < vidas) {
            vidas = partida.getVidas();
            escudo=2;
            bateria=4;
            disparos.clear();
        }
        NaveEmVoo nave = partida.getNave();
        if (antena!=null) antena.atualizar(dt,partida);
        if (nave.getEstado() == NaveEmVoo.Estado.DESTRUIDA) return;
        if (nave.getEstado() == NaveEmVoo.Estado.POUSADA
                && nave.getPlataforma().getTipo() == PlataformaDePouso.Tipo.BASE) {
            bateria=4;
            escudo=2;
        }
        if(patrulhaAtiva) atualizarPatrulheiro(dt);
        if(sentinela!=null && sentinela.atualizar(dt,nave.getX(),nave.getY(),
                !bloqueado(sentinela.x(),sentinela.y(),nave.getX(),nave.getY()))) {
            atirar(sentinela.x(),sentinela.y(),nave);
        }
        boolean mira = alvoNoCone();
        if (intervaloPulso == 0 && bateria > 0 && (disparar || assistido && mira)) {
            bateria--;
            intervaloPulso=.8;
            pulsoRestante=.22;
            pulsoX=nave.getX();
            pulsoY=nave.getY();
            pulsoAngulo=nave.getAngulo();
            if (patrulhaAtiva && patrulheiroNoCone()) {
                estado=Estado.DESATIVADO;
                tempoEstado=0;
            }
            if(sentinelaNoCone()) sentinela.desativar();
        }
        if (!emAlerta() && bateria < 4) {
            recarga += dt;
            if (recarga + 1e-9 >= 3) {
                bateria++;
                recarga=0;
            }
        } else {
            recarga=0;
        }
        for (Disparo tiro : disparos) {
            tiro.x += tiro.vx*dt;
            tiro.y += tiro.vy*dt;
            tiro.idade += dt;
            if (Math.hypot(tiro.x-nave.getX(), tiro.y-nave.getY()) < 36) {
                atingir();
                tiro.idade=5;
            }
        }
        disparos.removeIf(tiro -> tiro.idade>4 || dentroDeRocha(tiro.x,tiro.y));
        if (patrulhaAtiva && estado != Estado.DESATIVADO && estado != Estado.RECUPERANDO
                && Math.hypot(x-nave.getX(),y-nave.getY())<57) atingir();
    }

    private void atualizarPatrulheiro(double dt) {
        tempoEstado += dt;
        if (estado == Estado.DESATIVADO) {
            if (tempoEstado>=4) trocar(Estado.RECUPERANDO);
            return;
        }
        if (estado == Estado.RECUPERANDO) {
            if (tempoEstado>=1) trocar(Estado.PATRULHA);
            return;
        }
        x += direcao*55*dt;
        if (x>=limiteDireito) { x=limiteDireito; direcao=-1; }
        if (x<=limiteEsquerdo) { x=limiteEsquerdo; direcao=1; }
        NaveEmVoo nave=partida.getNave();
        boolean detecta = nave.getX()>500 && Math.hypot(x-nave.getX(),y-nave.getY())<=480
                && !bloqueado(x,y,nave.getX(),nave.getY());
        if (estado == Estado.PATRULHA && detecta) trocar(Estado.AVISO);
        else if (estado == Estado.AVISO) {
            if (!detecta) trocar(Estado.PATRULHA);
            else if (tempoEstado+1e-9>=1) {
                atirar(x,y,nave);
                trocar(Estado.ESPERA);
            }
        } else if (estado == Estado.ESPERA && tempoEstado>=2) trocar(Estado.PATRULHA);
    }

    private void trocar(Estado novo) {
        estado=novo;
        tempoEstado=0;
    }

    private void atingir() {
        if (invulneravel>0 || partida.getNave().getEstado()==NaveEmVoo.Estado.DESTRUIDA) return;
        impactos++;
        invulneravel=1;
        if (escudo>0) escudo--;
        else partida.receberAtaque();
    }

    private void atirar(double x,double y,NaveEmVoo nave) {
        double distancia=Math.hypot(nave.getX()-x,nave.getY()-y);
        if(distancia>0) {
            disparos.add(new Disparo(x,y,(nave.getX()-x)*180/distancia,(nave.getY()-y)*180/distancia));
            tiros++;
        }
    }
    public boolean emAlerta() {
        return (patrulhaAtiva && (estado==Estado.AVISO || estado==Estado.ESPERA))
            || (sentinela!=null && (sentinela.estado()==SentinelaDeVarredura.Estado.AVISO
                || sentinela.estado()==SentinelaDeVarredura.Estado.ESPERA));
    }
    public boolean alvoNoCone() {return patrulheiroNoCone() || sentinelaNoCone();}
    public boolean patrulheiroNoCone() {
        return patrulhaAtiva && estado!=Estado.DESATIVADO && estado!=Estado.RECUPERANDO && noCone(x,y);
    }
    public boolean sentinelaNoCone() {return sentinela!=null && sentinela.vulneravel() && noCone(sentinela.x(),sentinela.y());}
    private boolean noCone(double x,double y) {
        NaveEmVoo nave=partida.getNave();
        double dx=x-nave.getX(), dy=y-nave.getY();
        double distancia=Math.hypot(dx,dy);
        double angulo=Math.toRadians(nave.getAngulo());
        return distancia>0 && distancia<=300
                && (-Math.sin(angulo)*dx+Math.cos(angulo)*dy)/distancia>=Math.cos(Math.toRadians(20))
                && !bloqueado(nave.getX(),nave.getY(),x,y);
    }

    private boolean dentroDeRocha(double px,double py) {
        return partida.getCaverna().getRochas().stream().anyMatch(r -> r.contem(px,py));
    }

    private boolean bloqueado(double x1,double y1,double x2,double y2) {
        for (Rocha rocha : partida.getCaverna().getRochas()) {
            double entrada=0, saida=1;
            double[] inicio={x1,y1}, delta={x2-x1,y2-y1};
            double[] minimo={rocha.x(),rocha.y()}, maximo={rocha.direita(),rocha.topo()};
            for (int eixo=0;eixo<2;eixo++) {
                if (Math.abs(delta[eixo])<1e-9) {
                    if (inicio[eixo]<minimo[eixo] || inicio[eixo]>maximo[eixo]) { saida=-1; break; }
                } else {
                    double a=(minimo[eixo]-inicio[eixo])/delta[eixo];
                    double b=(maximo[eixo]-inicio[eixo])/delta[eixo];
                    entrada=Math.max(entrada,Math.min(a,b));
                    saida=Math.min(saida,Math.max(a,b));
                }
            }
            if (entrada<=saida) return true;
        }
        return false;
    }

    public PartidaDePilotagem partida() { return partida; }
    public Estado estado() { return estado; }
    public double inimigoX() { return x; }
    public double inimigoY() { return y; }
    public double tempoEstado() { return tempoEstado; }
    public int bateria() { return bateria; }
    public int escudo() { return escudo; }
    public int tiros() { return tiros; }
    public int impactos() { return impactos; }
    public double invulneravel() { return invulneravel; }
    public double pulsoRestante() { return pulsoRestante; }
    public double pulsoX() { return pulsoX; }
    public double pulsoY() { return pulsoY; }
    public double pulsoAngulo() { return pulsoAngulo; }
    public List<Projetil> projeteis() {
        return disparos.stream().map(t -> new Projetil(t.x,t.y)).collect(java.util.stream.Collectors.toList());
    }
}
