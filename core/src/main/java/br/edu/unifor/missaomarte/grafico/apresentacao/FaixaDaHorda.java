package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.HordaAtual;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.pilotagem.horda.Modificador;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/** A duração vem do servidor e passa a contar na chegada da resposta, inclusive se o aplicativo for suspenso. */
public final class FaixaDaHorda {
    private static final long INTERVALO_MS = 60_000;

    private final ServidorDoJogo servidor;
    private final LongSupplier milissegundos;
    private HordaAtual atual;
    private long recebidaEm;
    private long consultarEm;
    private boolean consultando;
    private long segundoExibido = -1;
    private List<String> linhas = List.of("Consultando a horda...");

    public FaixaDaHorda(ServidorDoJogo servidor) {
        this(servidor, () -> System.nanoTime() / 1_000_000);
    }

    FaixaDaHorda(ServidorDoJogo servidor, LongSupplier milissegundos) {
        this.servidor = servidor;
        this.milissegundos = milissegundos;
        consultar();
    }

    public void atualizar() {
        if (!consultando && milissegundos.getAsLong() >= consultarEm) consultar();
    }

    private void consultar() {
        consultando = true;
        servidor.hordaAtual(Resposta.de(horda -> {
            consultando = false;
            recebidaEm = milissegundos.getAsLong();
            atual = horda;
            segundoExibido = -1;
            // Uma resposta já no limite da virada pode ainda ser da horda anterior.
            consultarEm = recebidaEm + Math.max(1, Math.min(60, horda.segundosParaAcabar())) * 1000;
        }, falha -> {
            consultando = false;
            atual = null;
            linhas = List.of("Horda indisponível");
            consultarEm = milissegundos.getAsLong() + INTERVALO_MS;
        }));
    }

    private long restante() {
        return Math.max(0, atual.segundosParaAcabar() - (milissegundos.getAsLong() - recebidaEm) / 1000);
    }

    public HordaAtual disponivel() {
        return atual != null && restante() > 0 ? atual : null;
    }

    public List<String> linhas() {
        if (atual == null) return linhas;
        long segundos = restante();
        if (segundos == segundoExibido) return linhas;
        segundoExibido = segundos;
        var descricao = atual.descricao();
        String modificadores = descricao.modificadores().stream().map(FaixaDaHorda::modificador)
                .collect(Collectors.joining("  ·  "));
        long minutos = segundos / 60 % 60;
        String relogio = segundos >= 3600 ? segundos / 3600 + "h " + minutos + "min"
                : segundos >= 60 ? minutos + "min " + segundos % 60 + "s" : segundos + "s";
        linhas = List.of("Horda " + atual.numero() + "  ·  " + descricao.caverna().nome() + "  ·  " + descricao.dificuldade(),
                modificadores.isEmpty() ? "Sem modificadores" : modificadores,
                "Termina em " + relogio + "  ·  Próxima às " + atual.proximaAs() + " (Fortaleza)");
        return linhas;
    }

    private static String modificador(Modificador m) {
        return switch (m.tipo()) {
            case VENTO -> String.format(Locale.forLanguageTag("pt-BR"), "Vento %.1f para a %s",
                    Math.abs(m.valor()), m.valor() < 0 ? "esquerda" : "direita");
            case GRAVIDADE -> "Gravidade " + Math.round(100 * m.valor()) + "%";
            case TANQUE -> "Tanque " + Math.round(100 * m.valor()) + "%";
        };
    }
}
