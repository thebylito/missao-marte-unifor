package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.dominio.Dificuldade;

/**
 * As três perguntas do console antes da partida (nome, dificuldade e tamanho do mapa), respondidas na janela.
 * Os padrões são os mesmos do console: "Piloto Anônimo", Médio e tamanho 5.
 *
 * <p>Na janela, o tamanho vai de {@value #TAMANHO_MINIMO} a {@value #TAMANHO_MAXIMO}. É um limite da
 * apresentação, não uma regra nova: acima de 10 as células ficam pequenas demais para ler, e com tamanho 1
 * o mapa (9 células) não comporta os passageiros e obstáculos do Médio e do Difícil, e o sorteio de células
 * livres nunca termina (problema herdado do console, registrado no tutorial da Fase 3).
 */
public final class FormularioNovaPartida {

    public static final String PILOTO_PADRAO = "Piloto Anônimo";
    public static final int TAMANHO_PADRAO = 5;
    public static final int TAMANHO_MINIMO = 2;
    public static final int TAMANHO_MAXIMO = 10;
    public static final int NOME_MAXIMO = 20;

    private final StringBuilder nome = new StringBuilder();
    private Dificuldade dificuldade = Dificuldade.MEDIO;
    private int tamanho = TAMANHO_PADRAO;

    /** Caractere digitado. Teclas de controle (Enter, Backspace, Tab...) não entram no nome. */
    public void digitar(char c) {
        if (!Character.isISOControl(c) && nome.length() < NOME_MAXIMO) {
            nome.append(c);
        }
    }

    public void apagarLetra() {
        if (nome.length() > 0) {
            nome.deleteCharAt(nome.length() - 1);
        }
    }

    public void proximaDificuldade() {
        Dificuldade[] todas = Dificuldade.values();
        dificuldade = todas[(dificuldade.ordinal() + 1) % todas.length];
    }

    public void dificuldadeAnterior() {
        Dificuldade[] todas = Dificuldade.values();
        dificuldade = todas[(dificuldade.ordinal() + todas.length - 1) % todas.length];
    }

    public void aumentarMapa() {
        tamanho = Math.min(TAMANHO_MAXIMO, tamanho + 1);
    }

    public void diminuirMapa() {
        tamanho = Math.max(TAMANHO_MINIMO, tamanho - 1);
    }

    /** O que foi digitado até agora, para mostrar na tela. */
    public String getNomeDigitado() {
        return nome.toString();
    }

    /** Nome usado na partida: o digitado sem espaços nas pontas ou, se vazio, o padrão (como no console). */
    public String getPiloto() {
        String digitado = nome.toString().trim();
        return digitado.isEmpty() ? PILOTO_PADRAO : digitado;
    }

    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    public int getTamanho() {
        return tamanho;
    }
}
