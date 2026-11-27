package br.edu.unifor.missaomarte.grafico.recursos;

import br.edu.unifor.missaomarte.grafico.audio.Som;

/** Recursos de cada ambiente. IDs desconhecidos usam a ambientação marciana. */
public enum TemaDaCaverna {
    MARCIANA(Folha.FUNDO, Folha.ROCHA, Folha.ANTENA, Folha.ASTEROIDE, Folha.CRATERADO, Som.AMBIENTE_MARTE),
    POLAR(Folha.FUNDO_POLAR, Folha.PAREDE_GELO, Folha.SONDA, Folha.GELO_POLAR, Folha.CRATERADO, Som.AMBIENTE_GELO),
    FERRO(Folha.FUNDO_FERRO, Folha.PAREDE_FERRO, Folha.GUINDASTE, Folha.FERRO, Folha.CRATERADO, Som.AMBIENTE_FERRO);

    public final Folha fundo;
    public final Folha parede;
    public final Folha marco;
    public final Folha asteroide;
    public final Folha variante;
    public final Som ambiente;

    TemaDaCaverna(Folha fundo, Folha parede, Folha marco, Folha asteroide, Folha variante, Som ambiente) {
        this.fundo = fundo;
        this.parede = parede;
        this.marco = marco;
        this.asteroide = asteroide;
        this.variante = variante;
        this.ambiente = ambiente;
    }

    public static TemaDaCaverna porId(String id) {
        if ("poco-polar".equals(id)) return POLAR;
        if ("veio-de-ferro".equals(id)) return FERRO;
        return MARCIANA;
    }
}
