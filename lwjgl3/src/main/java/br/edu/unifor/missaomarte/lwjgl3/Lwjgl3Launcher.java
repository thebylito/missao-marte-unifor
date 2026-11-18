package br.edu.unifor.missaomarte.lwjgl3;

import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.Plataforma;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.PreferenciasLibgdx;
import br.edu.unifor.missaomarte.grafico.online.ServidorHttp;
import br.edu.unifor.missaomarte.grafico.recursos.CatalogoDeAssets;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Ponto de entrada da versão desktop. Faz duas coisas: monta as peças do núcleo (raiz de composição, como o
 * {@code Main} do console) e abre a janela. A partir daí a libGDX controla o laço do jogo e chama o
 * ciclo de vida de {@link MissaoMarteGame}.
 */
public final class Lwjgl3Launcher {

    /** Opcional: -Dmissaomarte.semente=N repete o mesmo sorteio (o mesmo parâmetro do console). */
    static final String PROPRIEDADE_SEMENTE = "missaomarte.semente";
    /** Fase 5: endereço do servidor de ranking. Ex.: -Dmissaomarte.servidor=http://192.168.0.10:8080 */
    static final String PROPRIEDADE_SERVIDOR = "missaomarte.servidor";
    static final String SERVIDOR_PADRAO = "http://localhost:8080";
    /** Fase 6: -Dmissaomarte.toque=true mostra os controles de toque (para testar com o mouse). */
    static final String PROPRIEDADE_TOQUE = "missaomarte.toque";

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        if (ReinicioNoMacOS.reiniciarSeNecessario(Lwjgl3Launcher.class, args)) {
            return;   // a janela abriu (e já fechou) no processo filho
        }
        ConfiguracaoDoServidor servidor = new ConfiguracaoDoServidor(new PreferenciasLibgdx("missao-marte"),
                SERVIDOR_PADRAO, System.getProperty(PROPRIEDADE_SERVIDOR));
        Plataforma plataforma = new Plataforma(Boolean.getBoolean(PROPRIEDADE_TOQUE), servidor);
        new Lwjgl3Application(new MissaoMarteGame(montarServicos(servidor), plataforma), configuracao());
    }

    static ServicosDoJogo montarServicos(ConfiguracaoDoServidor servidor) {
        Relogio relogio = Relogio.DO_SISTEMA;
        Long semente = Long.getLong(PROPRIEDADE_SEMENTE);
        return new ServicosDoJogo(
                new GeradorDeMissao(semente == null ? new Random() : new Random(semente)),
                new ServicoRanking(new RankingArquivoJson(Paths.get("ranking.json")), relogio),
                relogio,
                ServidorHttp.daLibgdx(servidor::endereco));
    }

    static Lwjgl3ApplicationConfiguration configuracao() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Missão Marte Unifor");
        config.setWindowedMode((int) MissaoMarteGame.LARGURA, (int) MissaoMarteGame.ALTURA);
        config.setResizable(true);          // o FitViewport mantém a proporção ao redimensionar
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(10);              // janela minimizada ou sem foco: menos trabalho para a GPU
        // ícone da janela, renderizado no Blender (o macOS ignora ícone de janela; Windows e Linux mostram)
        config.setWindowIcon(CatalogoDeAssets.ICONE);
        return config;
    }
}
