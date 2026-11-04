package br.edu.unifor.missaomarte.dominio;

import static br.edu.unifor.missaomarte.teste.Verifica.falso;
import static br.edu.unifor.missaomarte.teste.Verifica.igual;
import static br.edu.unifor.missaomarte.teste.Verifica.verdadeiro;

import br.edu.unifor.missaomarte.teste.Teste;

class MissaoTeste {

    private final Missao missao = new Missao(new Mapa(2), new Nave(5), MovimentoInimigo.PARADO);

    @Teste
    void sabeQuaisCelulasEstaoOcupadas() {
        missao.adicionarPassageiro(new Professor("P", new Posicao(1, 0)));
        missao.adicionarAsteroide(new Asteroide(new Posicao(0, 1)));
        missao.adicionarInimigo(new Inimigo(new Posicao(-1, 0)));
        verdadeiro(missao.ocupada(Posicao.ORIGEM), "nave");
        verdadeiro(missao.ocupada(new Posicao(1, 0)), "passageiro");
        verdadeiro(missao.ocupada(new Posicao(0, 1)), "asteroide");
        verdadeiro(missao.ocupada(new Posicao(-1, 0)), "inimigo");
        falso(missao.ocupada(new Posicao(2, 2)), "vazia");
    }

    @Teste
    void encontraOPrimeiroPassageiroNaCelulaDaNave() {
        Professor primeiro = new Professor("Primeiro", Posicao.ORIGEM);
        missao.adicionarPassageiro(new Engenheiro("Longe", new Posicao(2, 2)));
        missao.adicionarPassageiro(primeiro);
        missao.adicionarPassageiro(new Astronauta("Segundo", Posicao.ORIGEM));
        igual(primeiro, missao.passageiroNaPosicaoDaNave().orElseThrow());
    }

    @Teste
    void embarcarTiraOPassageiroDoMapa() {
        Professor p = new Professor("P", Posicao.ORIGEM);
        missao.adicionarPassageiro(p);
        falso(missao.todosEmbarcados(), "antes");
        verdadeiro(missao.embarcar(p), "embarcou");
        verdadeiro(missao.todosEmbarcados(), "depois");
        igual(1, missao.getNave().getPassageiros().size());
    }

    @Teste
    void naveCheiaNaoTiraOPassageiroDoMapa() {
        Missao cheia = new Missao(new Mapa(2), new Nave(0), MovimentoInimigo.PARADO);
        Professor p = new Professor("P", Posicao.ORIGEM);
        cheia.adicionarPassageiro(p);
        falso(cheia.embarcar(p), "sem lugar");
        igual(1, cheia.getPassageiros().size());
    }

    @Teste
    void colisaoComAsteroideOuInimigo() {
        falso(missao.naveColidiu(), "mapa vazio");
        missao.adicionarAsteroide(new Asteroide(Posicao.ORIGEM));
        verdadeiro(missao.naveColidiu(), "asteroide");

        Missao comInimigo = new Missao(new Mapa(2), new Nave(5), (atual, mapa) -> Posicao.ORIGEM);
        comInimigo.adicionarInimigo(new Inimigo(new Posicao(1, 1)));
        falso(comInimigo.naveColidiu(), "inimigo longe");
        comInimigo.moverInimigos();
        verdadeiro(comInimigo.naveColidiu(), "inimigo veio até a nave");
    }

    @Teste
    void plataformaEhAOrigem() {
        verdadeiro(missao.naveNaPlataforma(), "começa na plataforma");
        missao.getNave().mover(Direcao.DIREITA, missao.getMapa());
        falso(missao.naveNaPlataforma(), "saiu");
    }
}
