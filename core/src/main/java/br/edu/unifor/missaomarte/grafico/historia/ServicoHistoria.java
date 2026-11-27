package br.edu.unifor.missaomarte.grafico.historia;

import br.edu.unifor.missaomarte.historia.ProgressoDaHistoria;
import br.edu.unifor.missaomarte.historia.RepositorioHistoria;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

/** Serializa o progresso em uma thread própria; as telas nunca esperam I/O. */
public final class ServicoHistoria implements AutoCloseable {
    private final RepositorioHistoria repositorio;
    private final ExecutorService fila = Executors.newSingleThreadExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "progresso-historia");
        thread.setDaemon(true);
        return thread;
    });

    public ServicoHistoria(RepositorioHistoria repositorio) { this.repositorio = repositorio; }

    public CompletableFuture<RepositorioHistoria.Leitura> carregar() {
        return enviar(repositorio::carregar);
    }

    public CompletableFuture<RepositorioHistoria.Leitura> reiniciarAposCorrupcao() {
        return enviar(() -> { repositorio.reiniciarAposCorrupcao(); return repositorio.carregar(); });
    }

    public CompletableFuture<ProgressoDaHistoria> iniciarM01() { return atualizar(ProgressoDaHistoria::iniciarM01); }

    public CompletableFuture<ProgressoDaHistoria> concluirM01(int pontos) {
        return atualizar(progresso -> {
            if (progresso.retomada().isEmpty()) {
                if (progresso.m01Concluida()) return progresso;
                throw new IllegalStateException("Nenhuma missão iniciada");
            }
            return progresso.concluirM01(pontos);
        });
    }

    private CompletableFuture<ProgressoDaHistoria> atualizar(UnaryOperator<ProgressoDaHistoria> alterar) {
        return enviar(() -> {
            var leitura = repositorio.carregar();
            if (!leitura.gravavel()) throw new IOException(leitura.aviso());
            var novo = alterar.apply(leitura.progresso());
            repositorio.salvar(novo);
            return novo;
        });
    }

    private <T> CompletableFuture<T> enviar(Callable<T> trabalho) {
        return CompletableFuture.supplyAsync(() -> {
            try { return trabalho.call(); }
            catch (Exception erro) { throw new CompletionException(erro); }
        }, fila);
    }

    /** Tarefas já aceitas terminam antes de a fila encerrar. */
    @Override public void close() { fila.shutdown(); }
}
