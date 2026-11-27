package br.edu.unifor.missaomarte.lwjgl3;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class HistoriaPastaTeste {
    @TempDir Path pasta;
    @Test void campanhaUsaPastaDeDadosConfiguradaNoDesktop() {
        String antiga=System.getProperty(PastaDosDados.PROPRIEDADE);
        try {
            System.setProperty(PastaDosDados.PROPRIEDADE,pasta.toString());
            assertEquals(pasta.resolve("historia"),Lwjgl3Launcher.pastaDaHistoria());
        } finally {
            if(antiga==null)System.clearProperty(PastaDosDados.PROPRIEDADE);
            else System.setProperty(PastaDosDados.PROPRIEDADE,antiga);
        }
    }
}
