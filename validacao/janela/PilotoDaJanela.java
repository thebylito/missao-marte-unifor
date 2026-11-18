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
 *   java -XstartOnFirstThread -cp "<projeto>/$L/*:/tmp/piloto" PilotoDaJanela /tmp/janela/prints 3 vitoria
 * (-XstartOnFirstThread só no macOS.) Roteiros (semente 3, Fácil, mapa 3):
 *   vitoria  - ddcsscdwcaaaaaawwwcdddss: resgata os 4 passageiros e vence com 60 pontos;
 *   colisao  - ddcsssaaaadada: embarca um passageiro e bate 3 vezes no asteroide em (-2,3) até a nave ser destruída.
 */
public class PilotoDaJanela extends MissaoMarteGame {

    private interface Passo { void executar(); }

    private static final int QUADROS_ENTRE_PASSOS = 12;
    private final Deque<Passo> roteiro = new ArrayDeque<>();
    private final String pasta;
    private int quadros;
    private int numeroDoPrint;

    PilotoDaJanela(ServicosDoJogo servicos, String pasta, String roteiro) {
        super(servicos);
        this.pasta = pasta;
        if (roteiro.equals("colisao")) {
            montarRoteiroDeColisao();
        } else {
            montarRoteiro();
        }
    }

    /** Começo comum: menu, ranking vazio, apagar cancelado e a nova partida (Ana, Fácil, mapa 3). */
    private void comecoComum() {
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
    }

    private void montarRoteiro() {
        comecoComum();
        String vitoria = "ddcsscdwcaaaaaawwwcdddss";
        for (int i = 0; i < vitoria.length(); i++) {
            char c = vitoria.charAt(i);
            tecla(Keys.valueOf(String.valueOf(Character.toUpperCase(c))), c);
            if (i == 5) print("partida-dois-a-bordo");
            if (i == 18) print("partida-retorne-a-plataforma");
        }
        aguardar(0.6f);
        print("fim-vitoria");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_2, '2'); print("ranking-com-ana");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    private void montarRoteiroDeColisao() {
        comecoComum();
        String colisao = "ddcsssaaaadada";
        for (int i = 0; i < colisao.length(); i++) {
            char c = colisao.charAt(i);
            tecla(Keys.valueOf(String.valueOf(Character.toUpperCase(c))), c);
            if (i == 2) print("partida-embarque");
            if (i == 9) print("partida-colisao-explosao");
            if (i == 13) print("partida-nave-destruida");
        }
        aguardar(1.2f);
        print("fim-derrota");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    /** Passos vazios: dá tempo para animações e efeitos terminarem. */
    private void aguardar(float segundos) {
        int passos = (int) Math.ceil(segundos * 60 / QUADROS_ENTRE_PASSOS);
        for (int i = 0; i < passos; i++) {
            roteiro.add(() -> { });
        }
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
            // O framebuffer guarda também o alfa das partes translúcidas, mas a janela mostra só as cores (RGB).
            // Sem forçar o alfa a 255, um visualizador compõe o print sobre branco e as cores ficam diferentes.
            java.nio.ByteBuffer pixels = imagem.getPixels();
            for (int i = 3; i < pixels.limit(); i += 4) {
                pixels.put(i, (byte) 255);
            }
            String arquivo = String.format("%s/%02d-%s.png", pasta, ++numeroDoPrint, nome);
            PixmapIO.writePNG(Gdx.files.absolute(arquivo), imagem, Deflater.DEFAULT_COMPRESSION, true);
            imagem.dispose();
            Gdx.app.log("Piloto", "print " + arquivo + " (" + w + "x" + h + ")");
        });
    }

    @Override
    public void render() {
        super.render();                       // a tela desenha o quadro normalmente
        if (getRecursos() == null || !getRecursos().isCarregado()) {
            return;                           // Fase 4: espera a tela de carregamento terminar
        }
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
        new Lwjgl3Application(new PilotoDaJanela(servicos, args[0], args.length > 2 ? args[2] : "vitoria"), config);
    }
}
