package br.edu.unifor.missaomarte.grafico.recursos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.grafico.audio.Som;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Os arquivos de {@code assets/} conferidos sem OpenGL nem placa de som: lê só o cabeçalho de cada PNG e WAV. Pega
 * asset faltando, folha com tamanho diferente do código, manifesto do Blender desatualizado e arquivo sem origem
 * registrada (critério da issue #6: "assets têm origem documentada ou são gerados pelo projeto").
 */
class CatalogoDeAssetsTeste {

    private static final Path ASSETS = Path.of(System.getProperty("missaomarte.assets", "../assets"));

    @Test
    void todoArquivoDoCatalogoExiste() {
        for (String arquivo : CatalogoDeAssets.arquivos()) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(arquivo)), "falta assets/" + arquivo);
        }
    }

    @Test
    void cadaFolhaTemOTamanhoQueOCodigoEspera() throws IOException {
        for (Folha f : Folha.values()) {
            byte[] cabecalho = Arrays.copyOf(Files.readAllBytes(ASSETS.resolve(f.getCaminho())), 26);
            assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'},
                    Arrays.copyOf(cabecalho, 8), f + ": não é PNG");
            ByteBuffer b = ByteBuffer.wrap(cabecalho).order(ByteOrder.BIG_ENDIAN);
            assertEquals(f.getLargura() * f.getColunas(), b.getInt(16), f + ": largura");
            assertEquals(f.getAltura() * f.getLinhas(), b.getInt(20), f + ": altura");
            assertEquals(8, cabecalho[24], f + ": 8 bits por canal");
            assertEquals(6, cabecalho[25], f + ": RGBA");
        }
    }

    @Test
    void asFolhasBatemComOManifestoGeradoPeloBlender() throws IOException {
        JsonValue assets = new JsonReader()
                .parse(Files.readString(ASSETS.resolve("sprites/manifest.json"), StandardCharsets.UTF_8))
                .get("assets");
        for (Folha f : Folha.values()) {
            JsonValue entrada = assets.get(f.getNomeNoManifesto());
            assertTrue(entrada != null, f + " não está no manifesto");
            assertEquals(f.getLargura(), entrada.getInt("cellW"), f + ": cellW");
            assertEquals(f.getAltura(), entrada.getInt("cellH"), f + ": cellH");
            assertEquals(f.getColunas(), entrada.getInt("cols"), f + ": cols");
            assertEquals(f.getLinhas(), entrada.getInt("rows"), f + ": rows");
            assertEquals(f.getQuadros(), entrada.getInt("frames"), f + ": frames");
        }
    }

    @Test
    void cadaSomEhUmWavPcm16BitsMono44Khz() throws IOException {
        for (Som s : Som.values()) {
            byte[] wav = Files.readAllBytes(ASSETS.resolve(s.getCaminho()));
            ByteBuffer b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII), s + ": RIFF");
            assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII), s + ": WAVE");
            assertEquals(1, b.getShort(20), s + ": PCM");
            assertEquals(1, b.getShort(22), s + ": mono");
            assertEquals(44100, b.getInt(24), s + ": 44,1 kHz");
            assertEquals(16, b.getShort(34), s + ": 16 bits");
            int bytesDeAudio = b.getInt(40);
            assertTrue(bytesDeAudio > 44100 * 2 / 20, s + ": pelo menos 50 ms de som");
            assertTrue(bytesDeAudio < 44100 * 2 * 2, s + ": no máximo 2 s (efeito, não música)");
        }
    }

    @Test
    void asFontesVemComALicenca() throws IOException {
        String licenca = Files.readString(ASSETS.resolve("fontes/OFL-ChakraPetch.txt"), StandardCharsets.UTF_8);
        assertTrue(licenca.contains("SIL OPEN FONT LICENSE"), "texto da OFL");
        for (String fonte : new String[] {CatalogoDeAssets.FONTE_TEXTO, CatalogoDeAssets.FONTE_TITULO}) {
            byte[] inicio = Arrays.copyOf(Files.readAllBytes(ASSETS.resolve(fonte)), 4);
            assertArrayEquals(new byte[] {0, 1, 0, 0}, inicio, fonte + ": TrueType");
        }
    }

    @Test
    void todoArquivoTemAOrigemRegistrada() throws IOException {
        String origem = Files.readString(ASSETS.resolve("ORIGEM.md"), StandardCharsets.UTF_8);
        for (String arquivo : CatalogoDeAssets.arquivos()) {
            assertTrue(origem.contains("`" + arquivo + "`"), "ORIGEM.md não cita " + arquivo);
        }
    }
}
