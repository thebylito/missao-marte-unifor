package br.edu.unifor.missaomarte.grafico.recursos;

import br.edu.unifor.missaomarte.grafico.audio.Som;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fachada (padrão Facade) sobre o {@link AssetManager} da libGDX: carrega texturas, sons e fontes do
 * {@link CatalogoDeAssets} aos poucos (a tela de carregamento mostra o progresso), recorta as folhas de sprites em
 * quadros e libera tudo de uma vez no {@link #dispose()}. Cada textura é carregada uma única vez e compartilhada
 * por todos os sprites que a usam (padrão Flyweight).
 */
public final class Recursos implements Disposable {

    public enum Fonte { TEXTO, TITULO }

    /**
     * Tamanho, em pixels, em que as fontes são geradas pelo FreeType. As telas desenham em tamanhos menores; com
     * mipmaps a redução fica nítida, inclusive em telas de alta densidade (Retina), onde o OpenGL desenha com o dobro
     * de pixels do "mundo" de 800x600.
     */
    static final int TAMANHO_GERADO = 64;
    /** Altura de linha da fonte padrão da libGDX na escala 1, que as telas usaram como medida desde a Fase 3. */
    static final float ALTURA_DE_LINHA_DE_REFERENCIA = 18f;
    /**
     * Espaço entre as letras na textura da fonte. Com mipmaps, a imagem é reduzida à metade várias vezes e, com o
     * espaço padrão (1 pixel), a borda de uma letra vaza para a vizinha (aparecia uma barrinha ao lado de "A" e "T").
     */
    static final int ESPACO_ENTRE_LETRAS = 8;

    private static final Map<Fonte, String> APELIDOS = Map.of(Fonte.TEXTO, "fonte-texto.ttf",
            Fonte.TITULO, "fonte-titulo.ttf");

    private final AssetManager gerenciador;
    private final Map<Folha, TextureRegion[]> quadros = new EnumMap<>(Folha.class);
    private final Map<Fonte, Float> escalas = new EnumMap<>(Fonte.class);
    /** Páginas de textura das fontes: como são nossas (não do gerador), são liberadas aqui no dispose. */
    private final List<PixmapPacker> paginasDasFontes = new ArrayList<>();

    public Recursos() {
        FileHandleResolver resolvedor = new InternalFileHandleResolver();
        gerenciador = new AssetManager(resolvedor);
        gerenciador.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolvedor));
        gerenciador.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolvedor));
    }

    /** Põe todos os assets na fila. O carregamento de fato acontece em {@link #atualizar()}, um pouco por quadro. */
    public void enfileirarTudo() {
        for (Folha f : Folha.values()) {
            if (f != Folha.NAVE) {
                gerenciador.load(f.getCaminho(), Texture.class, parametrosDaTextura(f));
            }
        }
        for (Som s : Som.values()) {
            gerenciador.load(s.getCaminho(), Sound.class);
        }
        enfileirarFonte(Fonte.TEXTO, CatalogoDeAssets.FONTE_TEXTO);
        enfileirarFonte(Fonte.TITULO, CatalogoDeAssets.FONTE_TITULO);
    }

    /**
     * Mipmaps só nas texturas com lados potência de 2: o OpenGL ES 2 do Android (Fase 6) não gera mipmaps para as
     * outras. Nas demais, o filtro linear basta, porque a redução é pequena.
     */
    private static TextureLoader.TextureParameter parametrosDaTextura(Folha f) {
        TextureLoader.TextureParameter p = new TextureLoader.TextureParameter();
        int largura = f.getLargura() * f.getColunas();
        int altura = f.getAltura() * f.getLinhas();
        boolean potenciaDeDois = Integer.bitCount(largura) == 1 && Integer.bitCount(altura) == 1;
        p.genMipMaps = potenciaDeDois;
        p.minFilter = potenciaDeDois ? TextureFilter.MipMapLinearLinear : TextureFilter.Linear;
        p.magFilter = TextureFilter.Linear;
        if (f == Folha.ROCHA || f == Folha.PAREDE_GELO || f == Folha.PAREDE_FERRO) {
            // Fase 7: a rocha é um ladrilho repetido pelas paredes da caverna; repetir exige potência de 2 no ES 2
            p.wrapU = Texture.TextureWrap.Repeat;
            p.wrapV = Texture.TextureWrap.Repeat;
        }
        return p;
    }

    private void enfileirarFonte(Fonte fonte, String arquivo) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter p = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        p.fontFileName = arquivo;
        p.fontParameters.size = TAMANHO_GERADO;
        if (fonte == Fonte.TITULO) p.fontParameters.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "▶";
        p.fontParameters.genMipMaps = true;
        p.fontParameters.minFilter = TextureFilter.MipMapLinearLinear;
        p.fontParameters.magFilter = TextureFilter.Linear;
        PixmapPacker paginas = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, ESPACO_ENTRE_LETRAS, false);
        paginasDasFontes.add(paginas);
        p.fontParameters.packer = paginas;
        gerenciador.load(APELIDOS.get(fonte), BitmapFont.class, p);
    }

    /** Carrega mais um pedaço. Devolve true quando tudo está pronto (e então recorta as folhas em quadros). */
    public boolean atualizar() {
        boolean pronto = gerenciador.update();
        if (pronto && quadros.isEmpty()) {
            prepararDepoisDoCarregamento();
        }
        return pronto;
    }

    /** De 0 a 1. */
    public float progresso() {
        return gerenciador.getProgress();
    }

    public boolean isCarregado() {
        return !quadros.isEmpty();
    }

    private void prepararDepoisDoCarregamento() {
        for (Folha f : Folha.values()) {
            if (f == Folha.NAVE) {
                continue;
            }
            Texture textura = gerenciador.get(f.getCaminho(), Texture.class);
            TextureRegion[] regioes = new TextureRegion[f.getQuadros()];
            for (int i = 0; i < regioes.length; i++) {
                FolhaDeSprites.Recorte r = FolhaDeSprites.recorte(f, i);
                regioes[i] = new TextureRegion(textura, r.x(), r.y(), r.largura(), r.altura());
            }
            quadros.put(f, regioes);
        }
        for (Fonte f : Fonte.values()) {
            BitmapFont fonte = gerenciador.get(APELIDOS.get(f), BitmapFont.class);
            fonte.setUseIntegerPositions(false);   // com escala fracionária, posições inteiras deixam o texto tremido
            escalas.put(f, ALTURA_DE_LINHA_DE_REFERENCIA / fonte.getData().lineHeight);
        }
    }

    public TextureRegion quadro(Folha folha, int quadro) {
        return quadros.get(folha)[quadro];
    }

    public BitmapFont fonte(Fonte fonte) {
        return gerenciador.get(APELIDOS.get(fonte), BitmapFont.class);
    }

    /** Escala que faz a fonte gerada ter, na escala 1 das telas, a altura de linha da fonte padrão. */
    public float escalaBase(Fonte fonte) {
        return escalas.get(fonte);
    }

    public Sound som(Som som) {
        return gerenciador.get(som.getCaminho(), Sound.class);
    }

    /** Quantos assets o AssetManager tem carregados agora (o log mostra este número antes e depois do dispose). */
    public int quantidadeCarregada() {
        return gerenciador.getLoadedAssets();
    }

    @Override
    public void dispose() {
        quadros.clear();
        gerenciador.dispose();     // libera texturas, sons, fontes e o gerador do FreeType
        for (PixmapPacker paginas : paginasDasFontes) {
            paginas.dispose();     // as texturas das letras
        }
        paginasDasFontes.clear();
    }
}
