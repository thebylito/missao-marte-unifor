package br.edu.unifor.missaomarte.teste;

import java.util.Objects;

/** Asserções mínimas, com os mesmos nomes de ideia do JUnit (assertEquals, assertTrue...). */
public final class Verifica {

    private Verifica() {
    }

    public static void igual(Object esperado, Object obtido) {
        igual(esperado, obtido, "");
    }

    public static void igual(Object esperado, Object obtido, String contexto) {
        if (!Objects.equals(esperado, obtido)) {
            throw new AssertionError(prefixo(contexto) + "esperado <" + esperado + "> mas foi <" + obtido + ">");
        }
    }

    public static void verdadeiro(boolean condicao, String contexto) {
        if (!condicao) {
            throw new AssertionError(prefixo(contexto) + "esperado verdadeiro");
        }
    }

    public static void falso(boolean condicao, String contexto) {
        verdadeiro(!condicao, contexto);
    }

    public static <T extends Throwable> T lanca(Class<T> tipo, Runnable codigo) {
        try {
            codigo.run();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) {
                return tipo.cast(t);
            }
            throw new AssertionError("esperado " + tipo.getSimpleName() + " mas foi " + t, t);
        }
        throw new AssertionError("esperado " + tipo.getSimpleName() + ", nada foi lançado");
    }

    private static String prefixo(String contexto) {
        return contexto == null || contexto.isEmpty() ? "" : contexto + ": ";
    }
}
