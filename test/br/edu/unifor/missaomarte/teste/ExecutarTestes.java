package br.edu.unifor.missaomarte.teste;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Procura classes terminadas em "Teste" no diretório compilado, executa cada método @Teste numa instância nova
 * e resume o resultado. Termina com código 1 se algum teste falhar (o script de build para).
 */
public final class ExecutarTestes {

    public static void main(String[] args) throws IOException {
        Path raiz = Paths.get(args.length > 0 ? args[0] : "build/test-classes");
        List<String> classes;
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            classes = arquivos.map(raiz::relativize).map(Path::toString)
                    .filter(n -> n.endsWith("Teste.class") && !n.contains("$"))
                    .map(n -> n.substring(0, n.length() - ".class".length()).replace('/', '.').replace('\\', '.'))
                    .sorted().collect(Collectors.toList());
        }
        int total = 0;
        List<String> falhas = new ArrayList<>();
        for (String nome : classes) {
            Class<?> classe;
            try {
                classe = Class.forName(nome);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            if (Modifier.isAbstract(classe.getModifiers())) {
                continue;
            }
            List<Method> testes = Stream.of(classe.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Teste.class))
                    .sorted(Comparator.comparing(Method::getName)).collect(Collectors.toList());
            int falhasDaClasse = 0;
            for (Method m : testes) {
                total++;
                try {
                    var ctor = classe.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    Object instancia = ctor.newInstance();
                    m.setAccessible(true);
                    m.invoke(instancia);
                } catch (InvocationTargetException e) {
                    falhasDaClasse++;
                    falhas.add(classe.getSimpleName() + "." + m.getName() + " -> " + e.getCause());
                } catch (ReflectiveOperationException e) {
                    falhasDaClasse++;
                    falhas.add(classe.getSimpleName() + "." + m.getName() + " -> " + e);
                }
            }
            System.out.printf("%-28s %2d testes  %s%n", classe.getSimpleName(), testes.size(),
                    falhasDaClasse == 0 ? "ok" : falhasDaClasse + " FALHA(S)");
        }
        falhas.forEach(f -> System.out.println("  FALHOU " + f));
        System.out.printf("%n%d testes, %d falhas%n", total, falhas.size());
        if (!falhas.isEmpty() || total == 0) {
            System.exit(1);
        }
    }
}
