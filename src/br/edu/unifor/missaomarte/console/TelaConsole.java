package br.edu.unifor.missaomarte.console;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.Asteroide;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Inimigo;
import br.edu.unifor.missaomarte.dominio.Mapa;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Passageiro;
import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.dominio.TipoPassageiro;
import java.io.PrintStream;
import java.util.Optional;

/** Tudo o que o jogo escreve no console. Só apresenta: nenhuma regra é decidida aqui. */
public class TelaConsole {

    private static final String LINHA = "================================================================";

    private final PrintStream out;

    public TelaConsole(PrintStream out) {
        this.out = out;
    }

    public void mensagem(String texto) {
        out.println(texto);
    }

    public void boasVindas() {
        out.println(LINHA);
        out.println("             MISSÃO MARTE UNIFOR - VERSÃO 10.0                  ");
        out.println(LINHA);
        out.println("  Pilote sua nave, salve os passageiros e desvie dos perigos!   ");
        out.println(LINHA);
    }

    public void menu() {
        out.println("\n--- MENU PRINCIPAL ---");
        out.println("1. Iniciar Nova Missão");
        out.println("2. Visualizar Ranking Top 5");
        out.println("3. Resetar Histórico de Ranking");
        out.println("4. Sair do Jogo");
        out.println("----------------------");
    }

    public void iniciandoMissao(Dificuldade dificuldade) {
        out.println("\nIniciando missão na dificuldade " + dificuldade + "...");
        out.println("Pressione Enter para decolar!");
    }

    public void mapa(Partida partida) {
        Missao missao = partida.getMissao();
        Mapa mapa = missao.getMapa();
        out.println();
        out.printf("Mapa da Missão (Pontos: %d) - Piloto: %s%n", partida.getPontos(), partida.getPiloto());
        out.print("    ");
        for (int x = mapa.getMin(); x <= mapa.getMax(); x++) {
            out.printf(" %2d", x);
        }
        out.println();
        out.print("    ");
        for (int x = mapa.getMin(); x <= mapa.getMax(); x++) {
            out.print(" __");
        }
        out.println();
        for (int y = mapa.getMin(); y <= mapa.getMax(); y++) {
            out.printf("%3d|", y);
            for (int x = mapa.getMin(); x <= mapa.getMax(); x++) {
                out.printf(" %2c", simbolo(missao, new Posicao(x, y)));
            }
            out.println();
        }
        out.println("Legenda: @=Nave, P=Professor, E=Engenheiro, T=Astronauta, #=Asteroide, X=Inimigo, L=Plataforma de Pouso, .=Vazio");
        out.println("Comandos: w/s/a/d (mover), c (embarcar), q (sair)");
        out.println("Passageiros na superfície marciana:");
        for (Passageiro p : missao.getPassageiros()) {
            out.printf(" - %s (%s) em (%d,%d)%n", p.getNome(), p.getTipo().getNome(),
                    p.getPosicao().x(), p.getPosicao().y());
        }
    }

    /** Prioridade de desenho: nave, passageiro, asteroide, inimigo, plataforma. */
    private static char simbolo(Missao missao, Posicao celula) {
        if (missao.getNave().estaEm(celula)) {
            return '@';
        }
        Optional<Passageiro> passageiro = missao.getPassageiros().stream()
                .filter(p -> p.getPosicao().equals(celula)).findFirst();
        if (passageiro.isPresent()) {
            return simbolo(passageiro.get().getTipo());
        }
        for (Asteroide a : missao.getAsteroides()) {
            if (a.getPosicao().equals(celula)) return '#';
        }
        for (Inimigo i : missao.getInimigos()) {
            if (i.getPosicao().equals(celula)) return 'X';
        }
        return celula.equals(missao.getMapa().getPlataforma()) ? 'L' : '.';
    }

    private static char simbolo(TipoPassageiro tipo) {
        switch (tipo) {
            case ENGENHEIRO: return 'E';
            case ASTRONAUTA: return 'T';
            default: return 'P';
        }
    }

    public void status(Partida partida) {
        Missao missao = partida.getMissao();
        Nave nave = missao.getNave();
        out.printf("Nave em (%d,%d) | Pontos: %d | Vidas: %d | A bordo: %d/%d | Restantes: %d%n",
                nave.getPosicao().x(), nave.getPosicao().y(), partida.getPontos(), nave.getVidas(),
                nave.getPassageiros().size(), nave.getCapacidade(), missao.getPassageiros().size());
    }

    public void evento(Evento e) {
        switch (e.tipo()) {
            case ABORTADA:
                out.println("Missão abortada pelo piloto.");
                break;
            case COMANDO_INVALIDO:
                out.println("Comando inválido.");
                break;
            case SEM_PASSAGEIRO_NA_POSICAO:
                out.println("Nenhum passageiro nesta posição.");
                break;
            case PASSAGEIRO_EMBARCADO:
                out.printf("Passageiro %s embarcado com sucesso! +%d pontos!%n", e.passageiro(), e.valor());
                break;
            case NAVE_CHEIA:
                out.println("Nave cheia! Não há espaço para mais passageiros.");
                break;
            case COLISAO:
                out.printf("Alerta! Colisão detectada! Vidas restantes: %d%n", e.valor());
                break;
            case NAVE_DESTRUIDA:
                out.println("GAME OVER! A nave foi destruída.");
                break;
            case PONTOS_ESGOTADOS:
                out.println("Combustível/Pontuação zerada! Missão perdida.");
                break;
            case RETORNE_A_PLATAFORMA:
                out.println("✨ ALERTA: Todos os passageiros resgatados! Retorne para a Plataforma de Pouso 'L' em (0,0) para completar a missão.");
                break;
            case VITORIA:
                out.println("\n" + LINHA);
                out.println("🚀 DECOLAGEM AUTORIZADA! Nave acoplada à plataforma em (0,0).");
                out.println("Retornando à órbita marciana com todos os passageiros. Missão cumprida!");
                out.println(LINHA);
                break;
            default:
                throw new IllegalArgumentException("Evento sem apresentação: " + e.tipo());
        }
    }

    public void estatisticas(ResultadoPartida r, Ranking ranking) {
        out.println("Estatísticas da Partida:");
        out.printf(" - Pontuação Final: %d pontos%n", r.pontos());
        out.printf(" - Movimentos Efetuados: %d%n", r.movimentos());
        out.printf(" - Tempo de Jogo: %d segundos%n", r.tempoSegundos());
        out.printf(" - Passageiros Resgatados: %d%n", r.passageirosResgatados());
        Optional<RegistroRanking> lider = ranking.getLider();
        int recorde = lider.map(RegistroRanking::pontos).orElse(0);
        if (r.pontos() > recorde && recorde > 0) {
            out.println("🏆 Novo recorde absoluto do sistema!");
        } else if (recorde > 0) {
            out.printf(" - Recorde atual a ser batido: %d pontos (Piloto: %s)%n", recorde, lider.get().nome());
        }
        out.println(LINHA);
    }

    public void ranking(Ranking ranking) {
        out.println("\n====== RANKING TOP 5 PILOTOS ======");
        if (ranking.estaVazio()) {
            out.println(" - Nenhum registro encontrado. Seja o primeiro a jogar!");
        } else {
            int posicao = 1;
            for (RegistroRanking r : ranking.getRegistros()) {
                out.printf("%d. %s - %d pts | Dificuldade: %s | Coletados: %d | Tempo: %ds | %s%n",
                        posicao++, r.nome(), r.pontos(), r.dificuldade(), r.passageirosColetados(),
                        r.tempoJogoSegundos(), r.dataHora());
            }
        }
        out.println("===================================");
    }
}
