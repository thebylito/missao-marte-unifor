package br.edu.unifor.missaomarte.grafico.online;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;

/**
 * DA-48: os ajustes da Pilotagem Livre. Começa com o padrão; cada {@link #atualizar} pede os do servidor e fica com
 * eles quando chegam. Sem servidor, ou se o pedido falha, ficam os últimos que chegaram. A Horda não usa estes: usa
 * os gravados nela.
 */
public final class AjustesDoServidor {

    private volatile AjustesDeVoo atuais = AjustesDeVoo.PADRAO;

    /** Pede os ajustes; a resposta chega depois (ou nunca, sem conexão). */
    public void atualizar(ServidorDoJogo servidor) {
        servidor.ajustes(Resposta.de(novos -> atuais = novos, falha -> { }));
    }

    public AjustesDeVoo atuais() {
        return atuais;
    }
}
