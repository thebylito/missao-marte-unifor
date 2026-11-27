package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.FimDePartida;
import br.edu.unifor.missaomarte.historia.MissaoHistoria;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import com.badlogic.gdx.graphics.Color;
import br.edu.unifor.missaomarte.grafico.recursos.Folha;

/** Missão autoral com objetivo e comunicação próprios, sobre o voo compartilhado. */
public final class TelaMissaoHistoria extends TelaEncontro {
    private final ServicoHistoria servico;
    private final MissaoHistoria missao;
    public TelaMissaoHistoria(MissaoMarteGame jogo,ServicoHistoria servico) {this(jogo,servico,MissaoHistoria.porId("M01"));}
    public TelaMissaoHistoria(MissaoMarteGame jogo,ServicoHistoria servico,MissaoHistoria missao) {
        this(jogo,servico,missao,missao.criarEncontro());
    }
    private TelaMissaoHistoria(MissaoMarteGame jogo,ServicoHistoria servico,MissaoHistoria missao,EncontroDeResgate encontro) {
        super(jogo,encontro,new ControladorDoEncontro(encontro,missao.id()+" — "+missao.titulo().toUpperCase(java.util.Locale.ROOT)));
        this.servico=servico;this.missao=missao;
    }
    public MissaoHistoria missao(){return missao;}
    @Override protected String objetivoPrincipal() {
        var e=getEncontro();var p=e.partida();
        if(p.getEstado()==Partida.Estado.VITORIA)return "Operação concluída. Equipe em segurança!";
        if(e.antena()!=null && !e.antena().ativa()) {
            if(p.getNave().getABordo().isEmpty())return "Resgate Rosa e leve-a à antena marcada";
            if(e.antena().progresso()>0)return "Ativando antena: "+Math.round(e.antena().progresso()*100)+"% · mantenha o pouso";
            return "Leve Rosa à antena e permaneça pousado por 3 segundos";
        }
        if(missao.id().equals("M02"))return p.getNave().getABordo().isEmpty()?"Resgate Rosa e retorne à base":"Antena ativa: retorne à base com Rosa";
        if(missao.id().equals("M03"))return "Resgates: "+p.getEntregues()+"/2 · escolha a ordem e volte à base";
        return p.getNave().getABordo().isEmpty()?"Objetivo: resgatar Maria na baliza azul":"Objetivo: retornar à base verde";
    }
    @Override protected String ajudaDoEncontro() {
        return missao.id().equals("M01")?"Rota livre acima das rochas; base no marcador verde."
            :missao.id().equals("M02")?"Antena ciano no mapa. Feixe âmbar: ataque. F: pulso."
            :"Rota alta protegida; atalhos vigiados. A base reabastece a nave.";
    }
    @Override protected void desenharElementosExtras() {
        super.desenharElementosExtras();
        var p=getEncontro().partida();var base=p.getCaverna().getBase();int q=(int)(p.getTempo()*8)%8;
        lote().setColor(sinalDaBaseAtivo()?Color.WHITE:Color.GRAY);
        lote().draw(recursos().quadro(Folha.POSTO_M01,q),(float)base.getX()-70,(float)base.getSuperficie(),180,90);
        lote().setColor(Color.WHITE);
        if(sinalDaBaseAtivo())escrever("BASE ATIVA",(float)base.getX()-45,(float)base.getSuperficie()+112,Color.LIME,.8f);
        for(var plataforma:p.getCaverna().getPlataformas())if(plataforma.getEsperando().isPresent())
            lote().draw(recursos().quadro(Folha.BALIZA_SOS,q),(float)plataforma.getX()-35,(float)plataforma.getSuperficie(),48,96);
        var a=getEncontro().antena();
        if(a!=null) {
            int estado=a.ativa()?8:a.progresso()>0?4:0;
            int quadro=estado+(int)(p.getTempo()*6)%4;
            float x=(float)a.plataforma().getX()-100,y=(float)a.plataforma().getSuperficie();
            lote().draw(recursos().quadro(Folha.ANTENA_MISSAO,quadro),x-128,y-14,256,192);
            escrever(a.ativa()?"ANTENA ATIVA":"ANTENA · POUSO 3 s",(float)a.plataforma().getX(),y+60,a.ativa()?Color.LIME:Color.CYAN,.9f);
        }
    }
    @Override protected void desenharInterfaceExtra() {
        super.desenharInterfaceExtra();lote().begin();
        escreverComQuebra(mensagemDoRadio(),visivel().x()+32,visivel().y()+258,visivel().largura()-64,Color.CYAN,.82f);
        lote().end();
    }
    protected boolean sinalDaBaseAtivo() {
        return getEncontro().partida().getEntregues()>0 || !getEncontro().partida().getNave().getABordo().isEmpty();
    }
    protected String mensagemDoRadio() {
        var e=getEncontro();var p=e.partida();
        if(p.getEstado()==Partida.Estado.VITORIA)return missao.encerramento();
        if(missao.id().equals("M02")) {
            if(e.antena().ativa())return p.getNave().getABordo().isEmpty()?"BASE: Antena ativa. Falta resgatar Rosa na baliza.":"ROSA: Sinal restaurado! Pode nos levar para casa.";
            return p.getNave().getABordo().isEmpty()?"ROSA: Minha baliza está no vale. Preciso chegar à antena."
                :"ROSA: Pouse na plataforma da antena. Preciso de três segundos sem decolar.";
        }
        if(missao.id().equals("M03"))return p.getEntregues()>0?"SILVA: Falta um resgate. Reabasteça e siga a outra baliza."
            :"MARIA: Dois pedidos, duas rotas. Você decide por onde começar.";
        return p.getNave().getABordo().isEmpty()?"MARIA: Siga minha baliza. Cuidado com a patrulha."
            :"MARIA: Estou a bordo. Vamos voltar à base!";
    }
    @Override protected void concluirPartida(FimDePartida fim) {
        jogo.trocarTela(new TelaResultadoHistoria(jogo,servico,fim.estado(),getEncontro().partida().getPontos(),missao));
    }
}
