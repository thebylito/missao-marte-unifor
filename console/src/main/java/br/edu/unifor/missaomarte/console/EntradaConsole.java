package br.edu.unifor.missaomarte.console;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;

/** Leitura do teclado. Concentra o tratamento de fim da entrada (EOF) num só lugar. */
public class EntradaConsole {

    private final Scanner scanner;
    private final PrintStream saida;

    public EntradaConsole(Scanner scanner, PrintStream saida) {
        this.scanner = scanner;
        this.saida = saida;
    }

    /** Bloqueia até haver uma linha ou a entrada acabar. */
    public boolean temLinha() {
        return scanner.hasNextLine();
    }

    /** Mostra o prompt (se houver) e lê uma linha sem espaços nas pontas; no fim da entrada devolve o padrão. */
    public String lerLinha(String prompt, String padrao) {
        if (prompt != null && !prompt.isEmpty()) {
            saida.print(prompt);
        }
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return padrao;
    }

    /** Mostra o prompt e só então espera a linha; vazio se a entrada acabou. */
    public Optional<String> lerLinhaOuFim(String prompt) {
        saida.print(prompt);
        if (scanner.hasNextLine()) {
            return Optional.of(scanner.nextLine().trim());
        }
        return Optional.empty();
    }

    public void descartarLinha() {
        scanner.nextLine();
    }

    public void fechar() {
        scanner.close();
    }
}
