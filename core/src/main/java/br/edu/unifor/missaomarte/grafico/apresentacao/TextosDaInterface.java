package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Evento;
import br.edu.unifor.missaomarte.aplicacao.Partida;
import br.edu.unifor.missaomarte.aplicacao.ResultadoPartida;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Missao;
import br.edu.unifor.missaomarte.dominio.Nave;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import br.edu.unifor.missaomarte.grafico.online.PartidaDoHistorico;
import br.edu.unifor.missaomarte.grafico.online.PosicaoGlobal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Textos da janela. São os mesmos do console, sem os emojis (a fonte padrão da libGDX não os tem) e
 * quebrados em linhas curtas. Só formata: os números vêm da partida e do ranking.
 */
public final class TextosDaInterface {

    private TextosDaInterface() {
    }

    public static String evento(Evento e) {
        switch (e.tipo()) {
            case ABORTADA: return "Missão abortada pelo piloto.";
            case COMANDO_INVALIDO: return "Comando inválido.";
            case SEM_PASSAGEIRO_NA_POSICAO: return "Nenhum passageiro nesta posição.";
            case PASSAGEIRO_EMBARCADO:
                return String.format("Passageiro %s embarcado com sucesso! +%d pontos!", e.passageiro(), e.valor());
            case NAVE_CHEIA: return "Nave cheia! Não há espaço para mais passageiros.";
            case COLISAO: return String.format("Alerta! Colisão detectada! Vidas restantes: %d", e.valor());
            case NAVE_DESTRUIDA: return "GAME OVER! A nave foi destruída.";
            case PONTOS_ESGOTADOS: return "Combustível/Pontuação zerada! Missão perdida.";
            case RETORNE_A_PLATAFORMA:
                return "ALERTA: Todos os passageiros resgatados! Retorne para a Plataforma de Pouso 'L' em (0,0).";
            case VITORIA: return "DECOLAGEM AUTORIZADA! Missão cumprida!";
            default: throw new IllegalArgumentException("Evento sem texto: " + e.tipo());
        }
    }

    /** Painel ao lado do mapa: os mesmos dados da linha de status do console. */
    public static List<String> status(Partida partida) {
        Missao missao = partida.getMissao();
        Nave nave = missao.getNave();
        return List.of(
                "Piloto: " + partida.getPiloto(),
                "Dificuldade: " + partida.getDificuldade(),
                "Pontos: " + partida.getPontos(),
                "Vidas: " + nave.getVidas(),
                String.format("A bordo: %d/%d", nave.getPassageiros().size(), nave.getCapacidade()),
                "Restantes: " + missao.getPassageiros().size(),
                "Movimentos: " + partida.getMovimentos(),
                String.format("Nave em (%d,%d)", nave.getPosicao().x(), nave.getPosicao().y()));
    }

    public static String tituloDoFim(FimDePartida fim) {
        switch (fim.estado()) {
            case VITORIA: return "Missão cumprida!";
            case ABORTADA: return "Missão abortada";
            default:
                return fim.eventosFinais().stream().anyMatch(e -> e.tipo() == Evento.Tipo.NAVE_DESTRUIDA)
                        ? "GAME OVER!" : "Missão perdida.";
        }
    }

    /** As mesmas informações que o console mostra no fim da partida. */
    public static List<String> linhasDoFim(FimDePartida fim) {
        List<String> linhas = new ArrayList<>();
        fim.eventosFinais().stream()
                .filter(e -> e.tipo() != Evento.Tipo.VITORIA)
                .forEach(e -> linhas.add(evento(e)));
        if (fim.resultado().isEmpty()) {
            return linhas;
        }
        ResultadoPartida r = fim.resultado().get();
        linhas.add("Estatísticas da Partida:");
        if (fim.voo().isPresent()) {   // Fase 7: na pilotagem, o bônus de combustível e os pousos
            FimDePartida.ResumoDoVoo voo = fim.voo().get();
            linhas.add(String.format(" - Pontuação Final: %d pontos (resgates %d + bônus de combustível %d)",
                    r.pontos(), voo.pontosDosResgates(), voo.bonus()));
            linhas.add(String.format(" - Pousos: %d", voo.pousos()));
        } else {
            linhas.add(String.format(" - Pontuação Final: %d pontos", r.pontos()));
            linhas.add(String.format(" - Movimentos Efetuados: %d", r.movimentos()));
        }
        linhas.add(String.format(" - Tempo de Jogo: %d segundos", r.tempoSegundos()));
        linhas.add(String.format(" - Passageiros Resgatados: %d", r.passageirosResgatados()));
        int recorde = fim.liderAnterior().map(RegistroRanking::pontos).orElse(0);
        if (r.pontos() > recorde && recorde > 0) {
            linhas.add("Novo recorde absoluto do sistema!");
        } else if (recorde > 0) {
            linhas.add(String.format(" - Recorde atual a ser batido: %d pontos (Piloto: %s)",
                    recorde, fim.liderAnterior().get().nome()));
        }
        fim.erroAoSalvar().ifPresent(erro -> linhas.add("Não foi possível salvar o ranking: " + erro));
        if (fim.entrouNoTop()) {
            linhas.add("Parabéns! Você entrou para o Top 5 de pilotos!");
        }
        return linhas;
    }

    public static List<String> ranking(Ranking ranking) {
        if (ranking.estaVazio()) {
            return List.of("Nenhum registro encontrado. Seja o primeiro a jogar!");
        }
        List<String> linhas = new ArrayList<>();
        int posicao = 1;
        for (RegistroRanking r : ranking.getRegistros()) {
            linhas.add(String.format("%d. %s - %d pts | %s | Coletados: %d | Tempo: %ds | %s",
                    posicao++, r.nome(), r.pontos(), r.dificuldade(), r.passageirosColetados(),
                    r.tempoJogoSegundos(), r.dataHora()));
        }
        return linhas;
    }

    // ------------------------------------------------------------------ Fase 5: servidor

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Top 10 global, no mesmo estilo do Top 5 local. */
    public static List<String> rankingGlobal(List<PosicaoGlobal> ranking) {
        if (ranking.isEmpty()) {
            return List.of("Nenhuma vitória registrada no servidor ainda.");
        }
        List<String> linhas = new ArrayList<>();
        for (PosicaoGlobal p : ranking) {
            linhas.add(String.format("%d. %s - %d pts | %s | Coletados: %d | Tempo: %ds | %s", p.posicao(), p.nome(),
                    p.pontos(), dificuldade(p.dificuldade()), p.passageiros(), p.tempoSegundos(), data(p.registradaEm())));
        }
        return linhas;
    }

    /** Histórico do jogador conectado, da partida mais recente para a mais antiga. */
    public static List<String> historico(List<PartidaDoHistorico> partidas) {
        if (partidas.isEmpty()) {
            return List.of("Nenhuma partida registrada ainda.");
        }
        List<String> linhas = new ArrayList<>();
        for (PartidaDoHistorico p : partidas) {
            if ("PILOTAGEM".equals(p.modo())) {   // Fase 7: os "movimentos" da pilotagem são os pousos
                linhas.add(String.format("%s | Pilotagem | %s | %d pts | %s | %d passageiro(s) | %d pousos",
                        data(p.registradaEm()), resultado(p.resultado()), p.pontos(), dificuldade(p.dificuldade()),
                        p.passageiros(), p.movimentos()));
                continue;
            }
            linhas.add(String.format("%s | %s | %d pts | %s | %d passageiro(s) | %d movimentos", data(p.registradaEm()),
                    resultado(p.resultado()), p.pontos(), dificuldade(p.dificuldade()), p.passageiros(), p.movimentos()));
        }
        return linhas;
    }

    static String resultado(String r) {
        switch (r) {
            case "VITORIA": return "Vitória";
            case "DERROTA": return "Derrota";
            case "ABORTADA": return "Abortada";
            default: return r;
        }
    }

    static String dificuldade(String d) {
        try {
            return Dificuldade.valueOf(d).toString();     // "Fácil", "Médio", "Difícil", como no jogo
        } catch (IllegalArgumentException e) {
            return d;
        }
    }

    /** Data do servidor (UTC, ISO-8601) no fuso da máquina do jogador. */
    static String data(String iso) {
        try {
            return DATA.format(Instant.parse(iso).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException | NullPointerException e) {
            return String.valueOf(iso);
        }
    }
}
