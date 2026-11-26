import br.edu.unifor.missaomarte.aplicacao.GeradorDeMissao;
import br.edu.unifor.missaomarte.aplicacao.Relogio;
import br.edu.unifor.missaomarte.aplicacao.ServicoRanking;
import br.edu.unifor.missaomarte.grafico.MissaoMarteGame;
import br.edu.unifor.missaomarte.grafico.Plataforma;
import br.edu.unifor.missaomarte.grafico.ServicosDoJogo;
import br.edu.unifor.missaomarte.grafico.apresentacao.Botao;
import br.edu.unifor.missaomarte.grafico.apresentacao.FormularioNovaPartida.ModoDaMissao;
import br.edu.unifor.missaomarte.grafico.apresentacao.LayoutDaPartida;
import br.edu.unifor.missaomarte.grafico.apresentacao.Retangulo;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem;
import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.ServidorHttp;
import br.edu.unifor.missaomarte.grafico.telas.TelaNovaPartida;
import br.edu.unifor.missaomarte.grafico.telas.TelaPilotagem;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.PilotoAutomatico;
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
 *   javac -d /tmp/piloto -cp "$L/*" validacao/janela/PilotoDaJanela.java \
 *       nucleo/src/test/java/br/edu/unifor/missaomarte/pilotagem/PilotoAutomatico.java
 *   mkdir -p /tmp/janela/prints && cd /tmp/janela
 *   java -XstartOnFirstThread -cp "<projeto>/$L/*:/tmp/piloto" PilotoDaJanela /tmp/janela/prints 3 vitoria
 * (-XstartOnFirstThread só no macOS.) Roteiros (semente 3, Fácil, mapa 3):
 *   vitoria  - ddcsscdwcaaaaaawwwcdddss: resgata os 4 passageiros e vence com 60 pontos;
 *   colisao  - ddcsssaaaadada: embarca um passageiro e bate 3 vezes no asteroide em (-2,3) até a nave ser destruída;
 *   online   - (Fase 5, servidor ligado) cria a conta "Ana", vence, envia o resultado e abre o ranking global e o
 *              histórico. Endereço do servidor: -Dmissaomarte.servidor=http://localhost:8080 (o padrão);
 *   offline  - (Fase 5, servidor desligado) tenta entrar, vê o aviso, joga como convidado e abre o ranking global;
 *   toque-folga - o mesmo roteiro de toque, mas tocando cada seta do direcional um pouco FORA do botão (folga de
 *              toque): só vence se a tela aceitar esses toques;
 *   toque    - (Fase 6) modo toque no desktop: tudo por cliques nos botões (convidado, nova partida, direcional),
 *              vencendo a mesma partida da semente 3;
 *   pilotagem - (Fase 7) o Modo Pilotagem no Fácil, voado pelo piloto automático da validação (o mesmo do teste
 *              PilotoAutomaticoTeste): a cada quadro ele decide e as setas são apertadas e soltas pela tela, como
 *              num teclado. Prints na decolagem, em cada embarque e entrega, na vitória e no ranking da pilotagem.
 *              Leva uns 4 minutos (o voo é em tempo real).
 *   sombras  - (Fase 7, servidor ligado) o modo shadow com explosões: rode antes, em outro terminal,
 *              python3 validacao/sombras/piloto_fantasma.py --servidor <o mesmo> --segundos 40 --explodir-a-cada 4.
 *              Cria uma conta, fica na base e tira prints da sombra e das explosões dela; no fim bate no teto, e o
 *              fantasma registra a explosão desta nave.
 */
public class PilotoDaJanela extends MissaoMarteGame {

    private interface Passo { void executar(); }

    private static final int QUADROS_ENTRE_PASSOS = 12;
    private final Deque<Passo> roteiro = new ArrayDeque<>();
    private final String pasta;
    private int quadros;
    private int numeroDoPrint;
    /** Fase 7: enquanto voa, o roteiro fica parado e o piloto automático aperta as setas a cada quadro. */
    private boolean pilotando;
    private PilotoAutomatico pilotoAutomatico;
    private boolean esquerda;
    private boolean direita;
    private boolean motor;
    private int aBordoAntes;
    private int entreguesAntes;
    private boolean decolou;
    private boolean passouOVao;

    PilotoDaJanela(ServicosDoJogo servicos, String pasta, String roteiro) {
        super(servicos, new Plataforma(roteiro.startsWith("toque"),
                ConfiguracaoDoServidor.emMemoria(System.getProperty("missaomarte.servidor", "http://localhost:8080"))));
        this.pasta = pasta;
        if (roteiro.equals("colisao")) {
            montarRoteiroDeColisao();
        } else if (roteiro.equals("online")) {
            montarRoteiroOnline();
        } else if (roteiro.equals("offline")) {
            montarRoteiroOffline();
        } else if (roteiro.equals("toque")) {
            montarRoteiroDeToque(false);
        } else if (roteiro.equals("toque-folga")) {
            montarRoteiroDeToque(true);
        } else if (roteiro.equals("pilotagem")) {
            montarRoteiroDePilotagem();
        } else if (roteiro.equals("sombras")) {
            montarRoteiroDeSombras();
        } else {
            montarRoteiro();
        }
    }

    /** Começo comum: entra como convidado, menu, ranking vazio, apagar cancelado e a nova partida (Ana, Fácil, mapa 3). */
    private void comecoComum() {
        tecla(Keys.NUM_3, '3');               // Fase 5: tela de entrada -> jogar como convidado
        print("menu");
        tecla(Keys.NUM_2, '2'); print("ranking-vazio");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_3, '3'); print("menu-confirmar-apagar");
        tecla(Keys.N, 'n'); print("menu-apagar-cancelado");
        tecla(Keys.NUM_1, '1'); print("nova-partida-padrao");
        tecla(Keys.TAB, '\t');                // modo: clássico (a pilotagem é o padrão)
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

    /** Fase 7: o Modo Pilotagem no Fácil, voado pelo piloto automático; depois o fim e o ranking da pilotagem. */
    private void montarRoteiroDePilotagem() {
        tecla(Keys.NUM_3, '3');               // convidado
        tecla(Keys.NUM_1, '1'); print("nova-partida-pilotagem-padrao");
        tecla(Keys.SHIFT_LEFT, (char) 0);
        tecla(Keys.A, 'A'); tecla(Keys.N, 'n'); tecla(Keys.A, 'a');
        tecla(Keys.LEFT, (char) 0);           // Médio -> Fácil
        print("nova-partida-pilotagem");
        tecla(Keys.ENTER, '\r');
        aguardar(0.5f);
        print("pilotagem-na-base");
        roteiro.add(() -> {
            pilotoAutomatico = new PilotoAutomatico();
            pilotando = true;
            Gdx.app.log("Piloto", "piloto automático assumiu");
        });
        // o roteiro só continua quando a tela de fim aparecer
        aguardar(0.8f);
        print("fim-vitoria-pilotagem");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_2, '2'); print("ranking-classico");
        tecla(Keys.DOWN, (char) 0); print("ranking-pilotagem");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    /**
     * Fase 7, modo shadow com explosões (servidor ligado e o piloto_fantasma.py --explodir-a-cada voando perto da
     * base): cria uma conta, abre a pilotagem e fica parado na base. Tira um print quando vê a sombra e outro no meio de
     * cada explosão dela. No fim, liga o motor até bater no teto: o fantasma registra a explosão desta nave.
     */
    private void montarRoteiroDeSombras() {
        tecla(Keys.NUM_2, '2');               // criar conta (nome novo a cada execução)
        texto("Vigia" + (System.currentTimeMillis() % 10000));
        tecla(Keys.ENTER, '\r');
        texto("segredo1");
        tecla(Keys.ENTER, '\r');
        aguardar(1.5f);
        tecla(Keys.NUM_1, '1');
        selecionarModo(ModoDaMissao.PILOTAGEM);
        tecla(Keys.LEFT, (char) 0);           // Médio -> Fácil
        tecla(Keys.ENTER, '\r');
        aguardar(0.5f);
        roteiro.add(() -> vigiando = true);
        aguardar(14f);                        // o fantasma explode a cada poucos segundos
        roteiro.add(() -> Gdx.input.getInputProcessor().keyDown(Keys.UP));
        aguardar(4f);                         // sobe até bater no teto da caverna
        roteiro.add(() -> Gdx.input.getInputProcessor().keyUp(Keys.UP));
        aguardar(2f);
        print("depois-da-batida");
    }

    private boolean vigiando;
    private boolean sombraVista;
    private int explosoesFotografadas;

    /** A cada quadro do roteiro "sombras": prints da primeira sombra e do meio de cada explosão recebida. */
    private void vigiar() {
        if (!(getScreen() instanceof TelaPilotagem)) {
            return;
        }
        TelaPilotagem tela = (TelaPilotagem) getScreen();
        SombrasDaPilotagem sombras;
        try {
            java.lang.reflect.Field campo = TelaPilotagem.class.getDeclaredField("sombras");
            campo.setAccessible(true);
            sombras = (SombrasDaPilotagem) campo.get(tela);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        if (!sombraVista && !sombras.getSombras().isEmpty()) {
            sombraVista = true;
            salvarPrint("sombra-a-vista");
        }
        for (SombrasDaPilotagem.Explosao e : sombras.getExplosoes()) {
            // a 0,25 s a explosão está no 5º dos 16 quadros: bem visível
            if (e.tempo() >= 0.25f && e.tempo() < 0.25f + 1 / 30f && explosoesFotografadas < 3) {
                salvarPrint("explosao-da-sombra-" + (++explosoesFotografadas));
            }
        }
    }

    /** Um quadro do piloto automático: decide e aperta ou solta as setas que mudaram. Prints nos marcos do voo. */
    private void pilotar() {
        if (!(getScreen() instanceof TelaPilotagem)) {
            apertar(false, false, false);
            pilotando = false;            // a tela de fim apareceu: o roteiro continua
            Gdx.app.log("Piloto", "voo terminado");
            return;
        }
        PartidaDePilotagem voo = ((TelaPilotagem) getScreen()).getControlador().getPartida();
        NaveEmVoo nave = voo.getNave();
        if (!decolou && nave.getEstado() == NaveEmVoo.Estado.VOANDO && nave.getY() > 400) {
            decolou = true;
            salvarPrint("pilotagem-voando");
        }
        if (!passouOVao && nave.getX() > 1100 && nave.getY() < 720) {
            passouOVao = true;
            salvarPrint("pilotagem-depois-do-vao");
        }
        if (nave.getABordo().size() > aBordoAntes) {
            salvarPrint("pilotagem-embarque-" + (voo.getEntregues() + nave.getABordo().size()));
        }
        if (voo.getEntregues() > entreguesAntes) {
            salvarPrint("pilotagem-entrega-" + voo.getEntregues());
        }
        aBordoAntes = nave.getABordo().size();
        entreguesAntes = voo.getEntregues();
        Comandos c = pilotoAutomatico.decidir(voo);
        apertar(c.girarEsquerda(), c.girarDireita(), c.motor());
    }

    private void apertar(boolean novaEsquerda, boolean novaDireita, boolean novoMotor) {
        esquerda = trocar(esquerda, novaEsquerda, Keys.LEFT);
        direita = trocar(direita, novaDireita, Keys.RIGHT);
        motor = trocar(motor, novoMotor, Keys.UP);
    }

    private static boolean trocar(boolean antes, boolean agora, int tecla) {
        InputProcessor p = Gdx.input.getInputProcessor();
        if (agora && !antes) {
            p.keyDown(tecla);
        } else if (!agora && antes) {
            p.keyUp(tecla);
        }
        return agora;
    }

    /** Digita um texto, uma tecla por caractere (keyDown, keyTyped, keyUp). */
    private void texto(String t) {
        for (char c : t.toCharArray()) {
            tecla(Character.isDigit(c) ? Keys.NUM_0 + (c - '0') : Keys.valueOf(String.valueOf(Character.toUpperCase(c))), c);
        }
    }

    /** Fase 5, servidor ligado: cadastro, vitória enviada, ranking global e histórico. */
    private void montarRoteiroOnline() {
        print("entrada");
        tecla(Keys.NUM_2, '2');               // criar conta
        texto("Ana");
        tecla(Keys.ENTER, '\r');
        texto("segredo1");
        print("entrada-cadastro-preenchido");
        tecla(Keys.ENTER, '\r');
        aguardar(1.5f);                       // cadastro + login no servidor
        print("menu-conectado");
        tecla(Keys.NUM_1, '1');               // nova partida: o nome já vem preenchido com o da conta
        selecionarModo(ModoDaMissao.CLASSICO);
        tecla(Keys.LEFT, (char) 0);
        tecla(Keys.DOWN, (char) 0); tecla(Keys.DOWN, (char) 0); print("nova-partida-conectado");
        tecla(Keys.ENTER, '\r');
        texto("ddcsscdwcaaaaaawwwcdddss");
        aguardar(1.5f);
        print("fim-vitoria-enviada");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_2, '2'); print("ranking-local");
        tecla(Keys.RIGHT, (char) 0);
        aguardar(1f);
        print("ranking-global");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_5, '5');
        aguardar(1f);
        print("historico");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    /** Fase 5, servidor desligado: o login falha com aviso, o convidado joga e o ranking global avisa. */
    private void montarRoteiroOffline() {
        tecla(Keys.NUM_1, '1');               // entrar
        texto("Ana");
        tecla(Keys.ENTER, '\r');
        texto("segredo1");
        tecla(Keys.ENTER, '\r');
        aguardar(1.5f);
        print("entrada-servidor-indisponivel");
        tecla(Keys.ESCAPE, (char) 27);
        tecla(Keys.NUM_3, '3');               // convidado
        print("menu-convidado");
        tecla(Keys.NUM_2, '2');
        tecla(Keys.RIGHT, (char) 0);
        aguardar(1f);
        print("ranking-global-indisponivel");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_5, '5');
        print("historico-convidado");
        tecla(Keys.ENTER, '\r');
        tecla(Keys.NUM_4, '4');
    }

    /** Toque (clique) num ponto do mundo. A janela tem 800x600 e mostra exatamente o mundo 800x600. */
    private void tocar(float x, float y) {
        roteiro.add(() -> {
            Gdx.input.getInputProcessor().touchDown(Math.round(x), Math.round(600 - y), 0, 0);
            Gdx.app.log("Piloto", "toque em " + Math.round(x) + "," + Math.round(y));
        });
    }

    /** Fase 6: os mesmos botões que o celular mostra, clicados na janela (-Dmissaomarte.toque=true). */
    /**
     * @param comFolga toca cada seta do direcional de 12 a 15 unidades FORA do botão (dentro da folga de toque): a
     *                 partida só termina em vitória se a tela aceitar esses toques
     */
    private void montarRoteiroDeToque(boolean comFolga) {
        print("entrada-toque");
        tocar(440, 325 - 2 * 42 - 13);        // 3. Jogar como convidado
        print("menu-toque");
        tocar(470, 385 - 10);                 // 1. Iniciar Nova Missão
        tocar(635, 477);                      // "Trocar": modo clássico (a pilotagem é o padrão)
        tocar(595, 334);                      // dificuldade "<": Médio -> Fácil
        tocar(595, 264);
        tocar(595, 264);                      // mapa "-" duas vezes: 5 -> 3
        print("nova-partida-toque");
        tocar(325, 96);                       // Decolar
        print("partida-toque");
        java.util.Map<Botao, Retangulo> botoes =
                LayoutDaPartida.calcular(new Retangulo(0, 0, 800, 600), true).botoes();
        String vitoria = "ddcsscdwcaaaaaawwwcdddss";
        for (int i = 0; i < vitoria.length(); i++) {
            Botao b = switch (vitoria.charAt(i)) {
                case 'w' -> Botao.CIMA;
                case 's' -> Botao.BAIXO;
                case 'a' -> Botao.ESQUERDA;
                case 'd' -> Botao.DIREITA;
                default -> Botao.EMBARCAR;
            };
            Retangulo r = botoes.get(b);
            if (comFolga && b == Botao.CIMA) {
                tocar(r.centroX(), r.topo() + 12);
            } else if (comFolga && b == Botao.BAIXO) {
                tocar(r.centroX(), r.y() - 15);
            } else if (comFolga && b == Botao.ESQUERDA) {
                tocar(r.x() - 15, r.centroY());
            } else if (comFolga && b == Botao.DIREITA) {
                tocar(r.direita() + 15, r.centroY());
            } else {
                tocar(r.centroX(), r.centroY());
            }
            if (i == 9) print("partida-toque-embarques");
        }
        aguardar(0.6f);
        print("fim-vitoria-toque");
        tocar(400, 300);                      // tocar em qualquer lugar volta ao menu
        tocar(470, 385 - 36 - 10);            // 2. ranking
        print("ranking-toque");
        tocar(400, 68);                       // Voltar ao menu
        tocar(470, 385 - 3 * 36 - 10);        // 4. Sair
    }

    /** Passos vazios: dá tempo para animações e efeitos terminarem. */
    private void aguardar(float segundos) {
        int passos = (int) Math.ceil(segundos * 60 / QUADROS_ENTRE_PASSOS);
        for (int i = 0; i < passos; i++) {
            roteiro.add(() -> { });
        }
    }

    private void tecla(int codigo, char caractere) {
        roteiro.add(() -> apertarTecla(codigo, caractere));
    }

    /** Nos roteiros com conta, o modo inicial depende da publicação da Horda. */
    private void selecionarModo(ModoDaMissao modo) {
        roteiro.add(() -> {
            if (getScreen() instanceof TelaNovaPartida nova) {
                for (int troca = 0; troca < 3; troca++) {
                    apertarTecla(Keys.TAB, '\t');
                    if (nova.getModo() == modo) return;
                }
            }
            throw new IllegalStateException("Não foi possível selecionar " + modo.getNome());
        });
    }

    private void apertarTecla(int codigo, char caractere) {
        InputProcessor p = Gdx.input.getInputProcessor();
        p.keyDown(codigo);
        InputProcessor depois = Gdx.input.getInputProcessor();   // pode ter trocado de tela no keyDown
        if (caractere != 0) depois.keyTyped(caractere);
        depois.keyUp(codigo);
        Gdx.app.log("Piloto", "tecla " + Keys.toString(codigo));
    }

    private void print(String nome) {
        roteiro.add(() -> salvarPrint(nome));
    }

    private void salvarPrint(String nome) {
        {
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
        }
    }

    @Override
    public void render() {
        super.render();                       // a tela desenha o quadro normalmente
        if (getRecursos() == null || !getRecursos().isCarregado()) {
            return;                           // Fase 4: espera a tela de carregamento terminar
        }
        if (pilotando) {
            pilotar();
            return;
        }
        if (vigiando) {
            vigiar();
        }
        if (++quadros % QUADROS_ENTRE_PASSOS == 0 && !roteiro.isEmpty()) {
            roteiro.poll().executar();        // o print lê o quadro recém-desenhado, antes da troca de buffers
        }
    }

    public static void main(String[] args) {
        Relogio relogio = Relogio.DO_SISTEMA;
        ServicosDoJogo servicos = new ServicosDoJogo(new GeradorDeMissao(new Random(Long.parseLong(args[1]))),
                new ServicoRanking(new RankingArquivoJson(Paths.get("ranking.json")), relogio), relogio,
                ServidorHttp.daLibgdx(System.getProperty("missaomarte.servidor", "http://localhost:8080")),
                new ServicoRanking(new RankingArquivoJson(Paths.get("ranking-pilotagem.json")), relogio));
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Missão Marte Unifor (piloto automático)");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(60);                // sem foco a janela continua no mesmo ritmo
        new Lwjgl3Application(new PilotoDaJanela(servicos, args[0], args.length > 2 ? args[2] : "vitoria"), config);
    }
}
