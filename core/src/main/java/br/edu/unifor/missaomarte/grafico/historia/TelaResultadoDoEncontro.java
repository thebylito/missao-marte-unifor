package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.telas.TelaBase;
import br.edu.unifor.missaomarte.pilotagem.historia.EncontroDeResgate;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import java.util.List;

final class TelaResultadoDoEncontro extends TelaBase {
    private final EncontroDeResgate encontro;
    TelaResultadoDoEncontro(MissaoMarteGame jogo, EncontroDeResgate encontro) {
        super(jogo);
        this.encontro=encontro;
    }
    @Override protected InputAdapter entrada() {
        return new InputAdapter() {
            @Override public boolean keyDown(int tecla) {
                if (tecla==Keys.ENTER) jogo.iniciarEncontroHistoria();
                if (tecla==Keys.ESCAPE) jogo.irParaMenu();
                return true;
            }
        };
    }
    @Override protected List<AlvoDeToque> alvosDeToque() {
        return List.of(tecla(new Retangulo(190,140,200,65),Keys.ENTER,(char) 10),
                tecla(new Retangulo(410,140,200,65),Keys.ESCAPE,(char) 0));
    }
    @Override protected void desenhar() {
        lote().begin();
        escreverTitulo(encontro.partida().getEstado()==Partida.Estado.VITORIA
                ? "RESGATE CONCLUÍDO" : "ENCONTRO ENCERRADO",440,Color.ORANGE,1.8f);
        escreverCentralizado("Encontro protótipo da história",385,Color.WHITE,1.15f);
        escreverCentralizado("Ataques do patrulheiro: "+encontro.tiros()+"   Impactos: "+encontro.impactos(),
                325,Color.LIGHT_GRAY,1f);
        escreverCentralizado("Pontos: "+encontro.partida().getPontos(),285,Color.WHITE,1.1f);
        lote().end();
        desenharBotoes(List.of(new Retangulo(190,140,200,65),new Retangulo(410,140,200,65)),
                List.of("Repetir","Menu"),1.1f);
    }
}
