package br.edu.unifor.missaomarte.grafico.recursos;

import static br.edu.unifor.missaomarte.grafico.online.Sessao.SKIN_PADRAO;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Disposable;

/** Mantém uma folha de voo por vez e consulta os atlas sem carregar texturas durante o desenho. */
public final class RecursosDasNaves implements Disposable {
    private static final int ANGULOS = 16;
    private static final int LADO_SOMBRA = 64;
    private static final int COLUNAS_PREVIAS = 8;
    private static final Folha GEOMETRIA = Folha.NAVE;

    private final FileHandle assets;
    private final CatalogoDeNaves catalogo;
    private String skinCarregada;
    private String skinSolicitada;
    private Texture folha;
    private Texture porta;
    private Texture pouso;
    private TextureRegion[][] quadrosDoPouso;
    private TextureRegion[][] quadrosDaPorta;
    private TextureRegion[][] quadros;
    private TextureRegion[][] previas;
    private TextureRegion[][] sombras;
    private Pixmap pixelsPadrao;
    private TextureRegion[][] sombrasPadrao;

    public RecursosDasNaves(FileHandle assets) {
        this.assets = assets;
        catalogo = CatalogoDeNaves.carregar(assets).comFolhasInstaladas(assets);
    }

    public CatalogoDeNaves catalogo() {
        return catalogo;
    }

    /** Chamado na troca de tela ou no menu, quando a consulta do perfil confirma a skin. */
    public void selecionar(String equipada) {
        String disponivel = catalogo.skinDisponivel(equipada);
        if (disponivel.equals(skinSolicitada)) {
            return;
        }
        if (!disponivel.equals(skinCarregada)) {
            if (pouso != null) {
                pouso.dispose();
                pouso = null;
                quadrosDoPouso = null;
            }
            if (porta != null) {
                porta.dispose();
                porta = null;
                quadrosDaPorta = null;
            }
            if (folha != null) {
                folha.dispose();
                folha = null;
                quadros = null;
                skinCarregada = null;
            }
            if (!SKIN_PADRAO.equals(disponivel)) {
                carregarFolhaAlternativa(disponivel);
            }
            if (folha == null) {
                carregarFolha(GEOMETRIA.getCaminho(), SKIN_PADRAO);
            }
        }
        if (quadrosDaPorta == null) {
            carregarPorta();
            carregarPouso();
        }
        skinSolicitada = disponivel;
    }

    private void carregarPouso() {
        FileHandle arquivo = assets.child("sprites/naves/pouso_" + skinCarregada + ".png");
        if (!arquivo.exists()) return;
        Texture textura = new Texture(arquivo);
        if (textura.getWidth() != 512 || textura.getHeight() != 256) {
            textura.dispose();
            return;
        }
        textura.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        pouso = textura;
        quadrosDoPouso = TextureRegion.split(textura, 128, 128);
    }

    public TextureRegion quadroDoPouso(float compressao) {
        if (quadrosDoPouso == null) return null;
        return regiao(quadrosDoPouso, Math.round(Math.max(0, Math.min(1, compressao)) * 7), 4);
    }

    private void carregarPorta() {
        FileHandle arquivo = assets.child("sprites/naves/porta_" + skinCarregada + ".png");
        if (!arquivo.exists()) return;
        Texture textura = new Texture(arquivo);
        if (textura.getWidth() != 128 * 4 || textura.getHeight() != 128 * 3) {
            textura.dispose();
            return;
        }
        textura.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        porta = textura;
        quadrosDaPorta = TextureRegion.split(textura, 128, 128);
    }

    /** Só a nave pousada usa esta folha. Distribuições antigas continuam usando a folha de voo. */
    public TextureRegion quadroDaPorta(float abertura) {
        if (quadrosDaPorta == null) return null;
        int quadro = Math.round(Math.max(0, Math.min(1, abertura)) * 11);
        return regiao(quadrosDaPorta, quadro, 4);
    }

    public float deslocamentoDaPorta() {
        return skinCarregada != null && skinCarregada.startsWith("guara") ? 21 : 0;
    }

    private void carregarFolhaAlternativa(String id) {
        try {
            carregarFolha(catalogo.nave(id).folha(), id);
        } catch (RuntimeException e) {
            // Uma distribuição com arte incompleta continua jogável com a nave padrão.
        }
    }

    private void carregarFolha(String caminho, String id) {
        Texture textura = new Texture(assets.child(caminho), true);
        if (textura.getWidth() != GEOMETRIA.getLargura() * GEOMETRIA.getColunas()
                || textura.getHeight() != GEOMETRIA.getAltura() * GEOMETRIA.getLinhas()) {
            textura.dispose();
            throw new IllegalArgumentException("Folha da nave com dimensões inválidas: " + caminho);
        }
        textura.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        quadros = TextureRegion.split(textura, GEOMETRIA.getLargura(), GEOMETRIA.getAltura());
        folha = textura;
        skinCarregada = id;
    }

    /** Entrada no Hangar. */
    public void prepararPrevias() {
        if (previas == null) {
            previas = carregarAtlas(CatalogoDeNaves.PREVIAS, GEOMETRIA.getLargura(), COLUNAS_PREVIAS);
        }
        if (regiao(previas, indice(SKIN_PADRAO), COLUNAS_PREVIAS) == null && !SKIN_PADRAO.equals(skinCarregada)) {
            prepararSombrasPadrao();
        }
    }

    /** Entrada na Pilotagem. */
    public void prepararSombras() {
        if (sombras == null) {
            sombras = carregarAtlas(CatalogoDeNaves.SOMBRAS, LADO_SOMBRA, ANGULOS);
        }
        if (indice(SKIN_PADRAO) >= sombras.length && !SKIN_PADRAO.equals(skinCarregada)) {
            prepararSombrasPadrao();
        }
    }

    private TextureRegion[][] carregarAtlas(String caminho, int lado, int colunas) {
        FileHandle arquivo = assets.child(caminho);
        if (arquivo.exists()) {
            try {
                Texture textura = new Texture(arquivo);
                if (textura.getWidth() == lado * colunas && textura.getHeight() % lado == 0) {
                    textura.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                    return TextureRegion.split(textura, lado, lado);
                }
                textura.dispose();
            } catch (RuntimeException e) {
                // Atlas opcional: uma instalação antiga ou incompleta usa o desenho padrão.
            }
        }
        return new TextureRegion[0][];
    }

    /** Faixa pequena usada se o atlas instalado não inclui a nave padrão e a folha ativa é outra. */
    private void prepararSombrasPadrao() {
        if (sombrasPadrao != null) {
            return;
        }
        Pixmap original = new Pixmap(assets.child(GEOMETRIA.getCaminho()));
        pixelsPadrao = new Pixmap(LADO_SOMBRA * ANGULOS, LADO_SOMBRA, Pixmap.Format.RGBA8888);
        try {
            pixelsPadrao.setBlending(Pixmap.Blending.None);
            for (int i = 0; i < ANGULOS; i++) {
                var r = FolhaDeSprites.recorte(GEOMETRIA, i * GEOMETRIA.getQuadros() / ANGULOS);
                pixelsPadrao.drawPixmap(original, r.x(), r.y(), r.largura(), r.altura(),
                        i * LADO_SOMBRA, 0, LADO_SOMBRA, LADO_SOMBRA);
            }
            Texture textura = new Texture(new PixmapTextureData(pixelsPadrao, null, false, false, true));
            textura.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            sombrasPadrao = TextureRegion.split(textura, LADO_SOMBRA, LADO_SOMBRA);
        } finally {
            original.dispose();
        }
    }

    public TextureRegion quadro(int quadro) {
        return regiao(quadros, quadro, GEOMETRIA.getColunas());
    }

    public TextureRegion previa(String id) {
        TextureRegion previa = regiao(previas, indice(id), COLUNAS_PREVIAS);
        if (previa == null) {
            previa = regiao(previas, indice(SKIN_PADRAO), COLUNAS_PREVIAS);
        }
        return previa != null ? previa : sombraPadrao(0);
    }

    public TextureRegion sombra(String id, double angulo) {
        int coluna = quadroDaSombra(angulo);
        int linha = indice(id);
        if (linha >= sombras.length) {
            linha = indice(SKIN_PADRAO);
        }
        return linha < sombras.length ? sombras[linha][coluna] : sombraPadrao(coluna);
    }

    private TextureRegion sombraPadrao(int coluna) {
        if (SKIN_PADRAO.equals(skinCarregada)) {
            return quadro(coluna * GEOMETRIA.getQuadros() / ANGULOS);
        }
        return sombrasPadrao[0][coluna];
    }

    private int indice(String id) {
        var nave = catalogo.nave(id);
        return nave == null ? Integer.MAX_VALUE : nave.indice();
    }

    private static TextureRegion regiao(TextureRegion[][] atlas, int indice, int colunas) {
        int linha = indice / colunas;
        return linha < atlas.length ? atlas[linha][indice % colunas] : null;
    }

    public static int quadroDaSombra(double angulo) {
        return Math.floorMod((int) Math.round((angulo % 360) / (360.0 / ANGULOS)), ANGULOS);
    }

    public void liberarPrevias() {
        liberarAtlas(previas);
        previas = null;
    }

    private static void liberarAtlas(TextureRegion[][] atlas) {
        if (atlas != null && atlas.length > 0) {
            atlas[0][0].getTexture().dispose();
        }
    }

    @Override
    public void dispose() {
        if (porta != null) porta.dispose();
        if (pouso != null) pouso.dispose();
        if (folha != null) {
            folha.dispose();
        }
        liberarPrevias();
        liberarAtlas(sombras);
        liberarAtlas(sombrasPadrao);
        if (pixelsPadrao != null) {
            pixelsPadrao.dispose();
        }
    }
}
