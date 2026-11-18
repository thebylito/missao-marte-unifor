package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;

/**
 * O que muda de uma plataforma para outra (Fase 6), escolhido pelo launcher de cada uma (raiz de composição).
 *
 * @param toque    mostra os botões de toque e usa o teclado do sistema para digitar (Android sempre; no desktop só
 *                 com -Dmissaomarte.toque=true, para testar com o mouse)
 * @param servidor endereço do servidor de ranking, configurável dentro do jogo
 */
public record Plataforma(boolean toque, ConfiguracaoDoServidor servidor) {

    /** Desktop sem toque e servidor em localhost (testes e launchers antigos). */
    public static Plataforma padrao() {
        return new Plataforma(false, ConfiguracaoDoServidor.emMemoria("http://localhost:8080"));
    }
}
