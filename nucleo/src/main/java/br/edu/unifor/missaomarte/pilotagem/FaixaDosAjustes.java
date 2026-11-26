package br.edu.unifor.missaomarte.pilotagem;

/**
 * DA-48: a conferência de faixa dos ajustes. Fica numa classe sem estado: se ficasse em {@link AjustesDeVoo}, o
 * construtor de {@link AjustesDosEventos} inicializaria {@code AjustesDeVoo} no meio da própria inicialização, e o
 * padrão dos eventos ainda seria nulo.
 */
final class FaixaDosAjustes {

    private FaixaDosAjustes() {
    }

    /** Recusa um valor fora da faixa (inclusive) ou que não é um número. */
    static void exigir(String campo, double valor, double minimo, double maximo) {
        if (!(valor >= minimo && valor <= maximo)) {
            throw new IllegalArgumentException(campo + " precisa estar entre " + numero(minimo) + " e "
                    + numero(maximo) + " (veio " + numero(valor) + ")");
        }
    }

    /** 20 em vez de 20.0, para a mensagem ficar igual ao que se escreve na variável. */
    static String numero(double valor) {
        return valor == Math.rint(valor) && Math.abs(valor) < 1e15 ? Long.toString((long) valor)
                : Double.toString(valor);
    }
}
