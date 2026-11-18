package br.edu.unifor.missaomarte.lwjgl3;

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * No macOS, a janela (GLFW) só funciona na primeira thread do processo, e a JVM só faz isso com a opção
 * {@code -XstartOnFirstThread}. A tarefa {@code ./gradlew :lwjgl3:run} já passa a opção; quem abre o jogo
 * pelo script do installDist ou por {@code java -jar} normalmente não passa. Nesse caso, este código
 * inicia uma nova JVM igual à atual, acrescentando a opção, e espera ela terminar.
 *
 * <p>Em Windows e Linux não faz nada.
 */
final class ReinicioNoMacOS {

    private static final String JA_REINICIADO = "missaomarte.reiniciado";

    private ReinicioNoMacOS() {
    }

    /** @return true se o jogo rodou num processo filho (o processo atual deve apenas terminar). */
    static boolean reiniciarSeNecessario(Class<?> principal, String[] args) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return false;
        }
        // a JVM do macOS define esta variável quando foi iniciada com -XstartOnFirstThread
        String variavel = "JAVA_STARTED_ON_FIRST_THREAD_" + ProcessHandle.current().pid();
        if ("1".equals(System.getenv(variavel)) || Boolean.getBoolean(JA_REINICIADO)) {
            return false;
        }
        List<String> comando = new ArrayList<>();
        comando.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        comando.add("-XstartOnFirstThread");
        comando.add("-D" + JA_REINICIADO + "=true");     // evita reiniciar em laço
        comando.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());  // ex.: -Dmissaomarte.semente
        comando.add("-cp");
        comando.add(System.getProperty("java.class.path"));
        comando.add(principal.getName());
        comando.addAll(List.of(args));
        try {
            Process filho = new ProcessBuilder(comando).inheritIO().start();
            int codigo = filho.waitFor();
            if (codigo != 0) {
                System.exit(codigo);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Não foi possível reiniciar a JVM com -XstartOnFirstThread: " + e
                    + "\nExecute com: java -XstartOnFirstThread ...");
            return true;
        }
    }
}
