package br.edu.unifor.missaomarte.grafico.telas;

import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.aplicacao.*;
import br.edu.unifor.missaomarte.dominio.*;
import br.edu.unifor.missaomarte.grafico.*;
import br.edu.unifor.missaomarte.grafico.apresentacao.*;
import br.edu.unifor.missaomarte.grafico.recursos.RecursosDasNaves;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import br.edu.unifor.missaomarte.pilotagem.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CicloDoPassageiroTeste {
    @TempDir Path pasta;
    @Test void passageiroDevolvidoAposExplosaoRecomecaNaPlataforma() throws Exception {
        var c=new Caverna(2000,1200).base(100,98).rocha(1200,0,100,500)
            .passageiro(new Astronauta("Ensaio",Posicao.ORIGEM),700,86);
        var partida=new PartidaDePilotagem("Ensaio",Dificuldade.MEDIO,c,ConfigDeVoo.medio());
        var ranking=new ServicoRanking(new RankingArquivoJson(pasta.resolve("r.json")),Relogio.DO_SISTEMA);
        var jogo=new MissaoMarteGame(new ServicosDoJogo(new GeradorDeMissao(new Random(1)),ranking,Relogio.DO_SISTEMA));
        campo(jogo,"naves",new RecursosDasNaves(new com.badlogic.gdx.files.FileHandle(pasta.toFile())));
        var tela=new TelaPilotagem(jogo,new ControladorDaPilotagem(partida,ranking));
        var pl=c.getPlataformas().get(1);var nave=partida.getNave();
        campo(nave,"plataforma",pl);campo(nave,"x",pl.getCentroX());campo(nave,"y",pl.getSuperficie()-partida.getConfig().peY());
        tela.atualizar(.25f);tela.atualizar(.25f);
        var animacoes=(Map<?,?>)valor(tela,"animacoes");
        assertTrue(((AnimacaoDoPassageiro)animacoes.get(pl)).progressoDoEmbarque()>0);
        tela.atualizar(.25f);
        assertTrue(pl.getEsperando().isEmpty());
        assertFalse(animacoes.containsKey(pl),"um embarque concluído não mantém progresso de caminhada");
        campo(nave,"estado",NaveEmVoo.Estado.VOANDO);campo(nave,"plataforma",null);campo(nave,"x",1250.0);campo(nave,"y",200.0);
        tela.atualizar(.05f);
        assertTrue(pl.getEsperando().isPresent());
        assertEquals(0,((AnimacaoDoPassageiro)animacoes.get(pl)).progressoDoEmbarque());
    }
    @Test void vitoriaNaoAcionaBracoQuandoCombustivelJaEstaCongelado() throws Exception {
        var c=new Caverna(2000,1200).base(100,98).passageiro(new Astronauta("Ensaio",Posicao.ORIGEM),700,86);
        var partida=new PartidaDePilotagem("Ensaio",Dificuldade.MEDIO,c,ConfigDeVoo.medio());
        var ranking=new ServicoRanking(new RankingArquivoJson(pasta.resolve("r.json")),Relogio.DO_SISTEMA);
        var jogo=new MissaoMarteGame(new ServicosDoJogo(new GeradorDeMissao(new Random(1)),ranking,Relogio.DO_SISTEMA));
        campo(jogo,"naves",new RecursosDasNaves(new com.badlogic.gdx.files.FileHandle(pasta.toFile())));
        campo(partida.getNave(),"combustivel",50.0);
        campo(partida,"estado",Partida.Estado.VITORIA);
        var tela=new TelaPilotagem(jogo,new ControladorDaPilotagem(partida,ranking));
        tela.atualizar(.1f);
        assertEquals(0,((AnimacaoDaNave)valor(tela,"animacaoDaNave")).braco());
    }
    static void campo(Object o,String nome,Object valor)throws Exception {Field f=o.getClass().getDeclaredField(nome);f.setAccessible(true);f.set(o,valor);}
    static Object valor(Object o,String nome)throws Exception {Field f=o.getClass().getDeclaredField(nome);f.setAccessible(true);return f.get(o);}
}
