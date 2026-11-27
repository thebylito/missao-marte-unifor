package br.edu.unifor.missaomarte.historia;

import java.io.IOException;

public interface RepositorioHistoria {
    record Leitura(ProgressoDaHistoria progresso, String aviso, boolean gravavel, boolean permiteReiniciar) {
        public Leitura(ProgressoDaHistoria progresso, String aviso, boolean gravavel) {
            this(progresso, aviso, gravavel, false);
        }
    }
    Leitura carregar() throws IOException;
    void salvar(ProgressoDaHistoria progresso) throws IOException;
    default void reiniciarAposCorrupcao() throws IOException { throw new IOException("Recuperação indisponível"); }
}
