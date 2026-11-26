package br.edu.unifor.missaomarte.servidor.seguranca;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Deriva sementes independentes com rótulos distintos para cada uso. */
public final class Sementes {

    private Sementes() {}

    public static byte[] derivar(byte[] chave, String rotulo) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return hmac.doFinal(rotulo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("não foi possível derivar a semente", e);
        }
    }

    public static String emHex(byte[] semente) {
        return HexFormat.of().formatHex(semente);
    }

    public static byte[] deHex(String semente) {
        return HexFormat.of().parseHex(semente);
    }

    public static long comoLong(byte[] semente) {
        return ByteBuffer.wrap(semente).getLong();
    }
}
