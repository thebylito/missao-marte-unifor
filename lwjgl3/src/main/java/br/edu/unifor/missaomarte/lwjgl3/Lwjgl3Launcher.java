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
import br.edu.unifor.missaomarte.grafico.online.SessaoGuardada;
import br.edu.unifor.missaomarte.grafico.recursos.CatalogoDeAssets;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        // ~/.prefs/missao-marte guarda o endereço do servidor e a sessão do piloto (DA-38)
        PreferenciasLibgdx preferencias = new PreferenciasLibgdx("missao-marte");
        ConfiguracaoDoServidor servidor = new ConfiguracaoDoServidor(preferencias,
                ConfiguracaoDoServidor.padraoDoBuild(SERVIDOR_PADRAO), System.getProperty(PROPRIEDADE_SERVIDOR));
        Plataforma plataforma = new Plataforma(Boolean.getBoolean(PROPRIEDADE_TOQUE), servidor,
                new SessaoGuardada(preferencias));
        new Lwjgl3Application(new MissaoMarteGame(montarServicos(servidor), plataforma), configuracao());
    }

    static ServicosDoJogo montarServicos(ConfiguracaoDoServidor servidor) {
        Relogio relogio = Relogio.DO_SISTEMA;
        Long semente = Long.getLong(PROPRIEDADE_SEMENTE);
        Path dados = pastaDosDados();
        return new ServicosDoJogo(
                new GeradorDeMissao(semente == null ? new Random() : new Random(semente)),
                new ServicoRanking(new RankingArquivoJson(dados.resolve("ranking.json")), relogio),
                relogio,
                ServidorHttp.daLibgdx(servidor::endereco),
                // Fase 7: a pilotagem tem o seu Top 5, ao lado do ranking.json
                new ServicoRanking(new RankingArquivoJson(dados.resolve("ranking-pilotagem.json")), relogio));
    }

    /** A pasta do ranking local (o pacote nativo usa a do usuário), criada se ainda não existir. */
    private static Path pastaDosDados() {
        Path pasta = PastaDosDados.escolher(System.getProperty(PastaDosDados.PROPRIEDADE),
                System.getProperty("user.home"));
        try {
            Files.createDirectories(pasta.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Não foi possível criar a pasta dos dados " + pasta.toAbsolutePath() + ": " + e);
        }
        return pasta;
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
