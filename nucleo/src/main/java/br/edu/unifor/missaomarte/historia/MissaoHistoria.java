package br.edu.unifor.missaomarte.historia;

import br.edu.unifor.missaomarte.pilotagem.*;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import br.edu.unifor.missaomarte.dominio.*;
import java.util.List;

/** Três operações autorais do capítulo Sinais perdidos. Instâncias de partida nunca são compartilhadas. */
public record MissaoHistoria(String id,String titulo,String briefing,String orientacao,String encerramento) {
    public static final String ID="M01", TITULO="Primeiro sinal";
    public static final String BRIEFING="Uma tempestade interrompeu as comunicações. Siga a baliza azul e traga Maria de volta à base.";
    public static List<MissaoHistoria> todas() {
        return List.of(new MissaoHistoria("M01",TITULO,BRIEFING,
            "Siga a baliza azul. A rota alta permite passar acima da patrulha.",
            "MARIA: Obrigada! Detectei uma antena ainda em silêncio."),
            new MissaoHistoria("M02","A torre muda",
                "ROSA: Minha equipe perdeu o contato. Resgate-me, leve-me até a antena e mantenha a nave pousada durante três segundos para transmitir.",
                "A sentinela varre o corredor. Espere a abertura, passe pela rota alta ou use o pulso. Depois da ativação, volte à base.",
                "ROSA: A antena está transmitindo! Recebemos dois novos pedidos de socorro."),
            new MissaoHistoria("M03","Duas rotas, duas chamadas",
                "MARIA: Há sobreviventes no vale e na prateleira alta. Escolha quem buscar primeiro e traga os dois à base.",
                "O caminho inferior é curto e vigiado. A rota superior evita as máquinas. Volte à base para reabastecer entre os resgates.",
                "SILVA: Todos a salvo. A comunicação está de volta. Operação Sinais perdidos concluída!"));
    }
    public static MissaoHistoria porId(String id) {
        return todas().stream().filter(m->m.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Missão desconhecida: "+id));
    }
    public static EncontroDeResgate primeiroSinal(){return EncontroDeResgate.novo();}
    public EncontroDeResgate criarEncontro() {
        if(id.equals("M01"))return primeiroSinal();
        Caverna c;
        if(id.equals("M02")) {
            c=contorno(2800,1300).rocha(850,80,180,300).rocha(2000,80,450,212).rocha(1690,80,120,284)
                .base(100,128).passageiro(new Engenheiro("Eng. Rosa",Posicao.ORIGEM),1250,128).instalacao(2100,328);
            var p=new PartidaDePilotagem("Expedição",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL));
            return new EncontroDeResgate(p).semPatrulha().comSentinela(1750,450).comAntena(c.getPlataformas().get(2));
        }
        c=contorno(3300,1400).rocha(1050,80,180,280).rocha(2100,80,180,220).rocha(2550,80,600,312).rocha(2310,80,120,394)
            .base(100,128).passageiro(new Astronauta("Ast. Maria",Posicao.ORIGEM),1450,128)
            .passageiro(new Professor("Dr. Silva",Posicao.ORIGEM),2700,428);
        return new EncontroDeResgate(new PartidaDePilotagem("Expedição",Dificuldade.FACIL,c,ConfigDeVoo.para(Dificuldade.FACIL)))
            .comSentinela(2370,560);
    }
    private static Caverna contorno(double w,double h) {
        return new Caverna(w,h).rocha(0,0,w,80).rocha(0,h-80,w,80).rocha(0,0,40,h).rocha(w-40,0,40,h);
    }
}
