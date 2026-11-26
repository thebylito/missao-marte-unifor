package br.edu.unifor.missaomarte.grafico;

import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.SessaoGuardada;

/**
 * O que muda de uma plataforma para outra (Fase 6), escolhido pelo launcher de cada uma (raiz de composição).
 *
 * @param toque          mostra os botões de toque e usa o teclado do sistema para digitar (Android sempre; no desktop
 *                       só com -Dmissaomarte.toque=true, para testar com o mouse)
 * @param servidor       endereço do servidor de ranking, configurável dentro do jogo
 * @param sessaoGuardada onde a sessão do piloto fica entre uma abertura do jogo e outra (DA-38)
 */
public record Plataforma(boolean toque, ConfiguracaoDoServidor servidor, SessaoGuardada sessaoGuardada) {

    /** Sem lugar para guardar a sessão (testes e o piloto da janela): ela fica só na memória. */
    public Plataforma(boolean toque, ConfiguracaoDoServidor servidor) {
        this(toque, servidor, SessaoGuardada.emMemoria());
    }

    /** Desktop sem toque e servidor em localhost (testes e launchers antigos). */
    public static Plataforma padrao() {
        return new Plataforma(false, ConfiguracaoDoServidor.emMemoria("http://localhost:8080"));
    }
}
