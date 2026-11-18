import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import br.edu.unifor.missaomarte.infraestrutura.RankingArquivoJson;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.zip.Deflater;

/**
 * Joga na janela real (LWJGL3 + OpenGL, as mesmas telas do jogo) sem teclado físico: as teclas são entregues
 * ao InputProcessor da tela atual, na mesma ordem que o backend usa (keyDown, keyTyped, keyUp). Os prints
 * são lidos do framebuffer do OpenGL, então não dependem da permissão de Gravação de Tela do macOS.
 *
 * Não faz parte do jogo: é uma ferramenta de validação (como os scripts Python desta pasta). Serve quando o
 * macOS não deixa o terminal enviar teclas (Acessibilidade) nem capturar a tela (Gravação de Tela).
 *
 * Uso, a partir da raiz do projeto (o ranking.json é gravado na pasta de onde o java é chamado):
 *   ./gradlew :lwjgl3:installDist
 *   L=lwjgl3/build/install/missao-marte/lib
 *   javac -d /tmp/piloto -cp "$L/*" validacao/janela/PilotoDaJanela.java
 *   mkdir -p /tmp/janela/prints && cd /tmp/janela
 *   java -XstartOnFirstThread -cp "<projeto>/$L/*:/tmp/piloto" PilotoDaJanela /tmp/janela/prints 3
 * (-XstartOnFirstThread só no macOS.) O roteiro de teclas está em montarRoteiro(); com a semente 3 vence com 60.
 */
public class PilotoDaJanela extends MissaoMarteGame {

    private interface Passo { void executar(); }

    private static final int QUADROS_ENTRE_PASSOS = 12;
    private final Deque<Passo> roteiro = new ArrayDeque<>();
    private final String pasta;
    private int quadros;
    private int numeroDoPrint;

    PilotoDaJanela(ServicosDoJogo servicos, String pasta) {
        super(servicos);
        this.pasta = pasta;
        montarRoteiro();
    }

    private void montarRoteiro() {
        print("menu");
        tecla(Keys.NUM_2, '2'); print("ranking-vazio");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_3, '3'); print("menu-confirmar-apagar");
        tecla(Keys.N, 'n'); print("menu-apagar-cancelado");
        tecla(Keys.NUM_1, '1'); print("nova-partida-padrao");
        tecla(Keys.SHIFT_LEFT, (char) 0);
        tecla(Keys.A, 'A'); tecla(Keys.N, 'n'); tecla(Keys.A, 'a');
        tecla(Keys.LEFT, (char) 0);
        tecla(Keys.DOWN, (char) 0); tecla(Keys.DOWN, (char) 0); print("nova-partida-ana-facil-3");
        tecla(Keys.ENTER, '\r'); print("partida-inicio");
        String vitoria = "ddcsscdwcaaaaaawwwcdddss";
        for (int i = 0; i < vitoria.length(); i++) {
            char c = vitoria.charAt(i);
            tecla(Keys.valueOf(String.valueOf(Character.toUpperCase(c))), c);
            if (i == 5) print("partida-dois-a-bordo");
            if (i == 18) print("partida-retorne-a-plataforma");
        }
        print("fim-vitoria");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_2, '2'); print("ranking-com-ana");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    private void tecla(int codigo, char caractere) {
        roteiro.add(() -> {
            InputProcessor p = Gdx.input.getInputProcessor();
            p.keyDown(codigo);
            InputProcessor depois = Gdx.input.getInputProcessor();   // pode ter trocado de tela no keyDown
            if (caractere != 0) depois.keyTyped(caractere);
            depois.keyUp(codigo);
            Gdx.app.log("Piloto", "tecla " + Keys.toString(codigo));
        });
    }

    private void print(String nome) {
        roteiro.add(() -> {
            int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
            Pixmap imagem = Pixmap.createFromFrameBuffer(0, 0, w, h);
            String arquivo = String.format("%s/%02d-%s.png", pasta, ++numeroDoPrint, nome);
            PixmapIO.writePNG(Gdx.files.absolute(arquivo), imagem, Deflater.DEFAULT_COMPRESSION, true);
            imagem.dispose();
            Gdx.app.log("Piloto", "print " + arquivo + " (" + w + "x" + h + ")");
        });
    }

    @Override
    public void render() {
        super.render();                       // a tela desenha o quadro normalmente
        if (++quadros % QUADROS_ENTRE_PASSOS == 0 && !roteiro.isEmpty()) {
            roteiro.poll().executar();        // o print lê o quadro recém-desenhado, antes da troca de buffers
        }
    }

    public static void main(String[] args) {
        Relogio relogio = Relogio.DO_SISTEMA;
        ServicosDoJogo servicos = new ServicosDoJogo(new GeradorDeMissao(new Random(Long.parseLong(args[1]))),
                new ServicoRanking(new RankingArquivoJson(Paths.get("ranking.json")), relogio), relogio);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Missão Marte Unifor (piloto automático)");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(60);                // sem foco a janela continua no mesmo ritmo
        new Lwjgl3Application(new PilotoDaJanela(servicos, args[0]), config);
    }
}
