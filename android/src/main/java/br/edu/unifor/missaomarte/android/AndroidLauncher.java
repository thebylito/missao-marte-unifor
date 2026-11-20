package br.edu.unifor.missaomarte.android;

import android.os.Bundle;
import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.Plataforma;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.PreferenciasLibgdx;
import br.edu.unifor.missaomarte.grafico.online.ServidorHttp;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import java.io.File;
import java.util.Random;

/**
 * Raiz de composição do Android (Fase 6): monta as mesmas peças do launcher desktop, com três diferenças de
 * plataforma: o ranking local fica na pasta privada do app, os controles são de toque e o servidor padrão é o do
 * emulador (10.0.2.2 é o computador onde o emulador roda). Num celular de verdade, o jogador troca o endereço na
 * tela de entrada (opção 4), e ele fica guardado nas preferências do app.
 */
public class AndroidLauncher extends AndroidApplication {

    /** O computador visto de dentro do emulador do Android. */
    static final String SERVIDOR_DO_EMULADOR = "http://10.0.2.2:8080";

    /**
     * Sorteio do mapa. Para validação, a mesma ideia do -Dmissaomarte.semente do desktop:
     * {@code adb shell am start -n br.edu.unifor.missaomarte/.android.AndroidLauncher --el semente 3}.
     */
    private Random sorteio() {
        long semente = getIntent() == null ? -1 : getIntent().getLongExtra("semente", -1);
        return semente >= 0 ? new Random(semente) : new Random();
    }

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        ConfiguracaoDoServidor servidor = new ConfiguracaoDoServidor(new PreferenciasLibgdx("missao-marte"),
                SERVIDOR_DO_EMULADOR, null);
        Relogio relogio = Relogio.DO_SISTEMA;
        ServicosDoJogo servicos = new ServicosDoJogo(
                new GeradorDeMissao(sorteio()),
                new ServicoRanking(new RankingArquivoJson(new File(getFilesDir(), "ranking.json").toPath()), relogio),
                relogio,
                ServidorHttp.daLibgdx(servidor::endereco),
                new ServicoRanking(new RankingArquivoJson(new File(getFilesDir(), "ranking-pilotagem.json").toPath()),
                        relogio));

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;      // esconde as barras do sistema durante o jogo
        initialize(new MissaoMarteGame(servicos, new Plataforma(true, servidor)), config);
    }
}
