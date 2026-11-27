package br.edu.unifor.missaomarte.infraestrutura;

import br.edu.unifor.missaomarte.historia.ProgressoDaHistoria;
import br.edu.unifor.missaomarte.historia.RepositorioHistoria;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.zip.CRC32;

/** Save de campanha independente dos rankings, com formato validado e cópia de recuperação. */
public final class HistoriaArquivo implements RepositorioHistoria {
    private final Path arquivo;
    private final Path copia;
    private static final int LIMITE = 65536;

    public HistoriaArquivo(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath();
        copia = this.arquivo.resolveSibling(this.arquivo.getFileName() + ".bak");
    }

    private record Tentativa(ProgressoDaHistoria progresso, IOException erro) {}

    private static Tentativa tentar(Path caminho) {
        try { return new Tentativa(ler(caminho), null); }
        catch (IllegalArgumentException erro) { return new Tentativa(null, new DadosInvalidos()); }
        catch (IOException erro) { return new Tentativa(null, erro); }
    }

    @Override public synchronized Leitura carregar() throws IOException {
        if (Files.notExists(arquivo) && Files.notExists(copia)) {
            return new Leitura(ProgressoDaHistoria.novo(), "", true);
        }
        Tentativa principal = tentar(arquivo);
        Tentativa reserva = tentar(copia);
        if (principal.erro() instanceof VersaoFutura || reserva.erro() instanceof VersaoFutura) {
            return new Leitura(ProgressoDaHistoria.novo(), "Este progresso pertence a uma versão mais recente do jogo. Ele foi preservado.", false);
        }
        if (principal.progresso() != null) return new Leitura(principal.progresso(), "", true);
        if (reserva.progresso() != null) {
            return new Leitura(reserva.progresso(), "Progresso recuperado da cópia de segurança.", true);
        }
        boolean corrompido = invalidoOuAusente(principal.erro()) && invalidoOuAusente(reserva.erro());
        return new Leitura(ProgressoDaHistoria.novo(), corrompido
                ? "Os dados estão danificados. Você pode arquivá-los e iniciar uma nova expedição."
                : "Não foi possível ler o progresso. Confira o armazenamento e tente novamente.", false, corrompido);
    }

    private static boolean invalidoOuAusente(IOException erro) {
        return erro instanceof DadosInvalidos || erro instanceof java.nio.file.NoSuchFileException;
    }

    @Override public synchronized void reiniciarAposCorrupcao() throws IOException {
        if (!carregar().permiteReiniciar()) throw new IOException("Estes dados não podem ser reiniciados por esta opção");
        String sufixo = ".corrompido-" + java.util.UUID.randomUUID();
        for (Path origem : new Path[]{arquivo, copia}) {
            if (Files.exists(origem)) Files.move(origem, origem.resolveSibling(origem.getFileName() + sufixo));
        }
        salvar(ProgressoDaHistoria.novo());
    }

    @Override public synchronized void salvar(ProgressoDaHistoria progresso) throws IOException {
        Leitura anterior = carregar();
        if (!anterior.gravavel()) throw new IOException(anterior.aviso());
        Files.createDirectories(arquivo.getParent());
        boolean existia = Files.exists(arquivo) || Files.exists(copia);
        gravarAtomico(copia, codificar(existia ? anterior.progresso() : progresso));
        gravarAtomico(arquivo, codificar(progresso));
    }

    private static ProgressoDaHistoria ler(Path origem) throws IOException {
        if (Files.size(origem) > LIMITE) throw new DadosInvalidos();
        StringBuilder texto = new StringBuilder();
        try (var leitor = Files.newBufferedReader(origem, StandardCharsets.UTF_8)) {
            char[] bloco = new char[2048];
            for (int lidos; (lidos = leitor.read(bloco)) >= 0;) {
                texto.append(bloco, 0, lidos);
                if (texto.length() > LIMITE) throw new DadosInvalidos();
            }
        }
        Properties p = new Properties();
        p.load(new StringReader(texto.toString()));
        int versao = Integer.parseInt(obrigatorio(p, "versao"));
        if (versao > 1) throw new VersaoFutura();
        if (versao != 1 || !"missao-marte".equals(obrigatorio(p, "campanha"))
                || !"expedicao".equals(obrigatorio(p, "perfil"))) throw new DadosInvalidos();
        String concluida = obrigatorio(p, "m01Concluida");
        if (!concluida.equals("true") && !concluida.equals("false")) throw new DadosInvalidos();
        var progresso = new ProgressoDaHistoria(Boolean.parseBoolean(concluida), obrigatorio(p, "retomada"),
                Integer.parseInt(obrigatorio(p, "melhorPontuacao")));
        if (!assinatura(progresso).equals(obrigatorio(p, "integridade"))) throw new DadosInvalidos();
        return progresso;
    }

    private static String obrigatorio(Properties p, String chave) throws IOException {
        String valor = p.getProperty(chave);
        if (valor == null) throw new DadosInvalidos();
        return valor;
    }

    private static String assinatura(ProgressoDaHistoria p) {
        CRC32 crc = new CRC32();
        crc.update(("1|missao-marte|expedicao|" + p.m01Concluida() + "|" + p.retomada() + "|"
                + p.melhorPontuacao()).getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc.getValue());
    }

    private static String codificar(ProgressoDaHistoria progresso) throws IOException {
        Properties p = new Properties();
        p.setProperty("versao", "1"); p.setProperty("campanha", "missao-marte"); p.setProperty("perfil", "expedicao");
        p.setProperty("m01Concluida", Boolean.toString(progresso.m01Concluida()));
        p.setProperty("retomada", progresso.retomada());
        p.setProperty("melhorPontuacao", Integer.toString(progresso.melhorPontuacao()));
        p.setProperty("integridade", assinatura(progresso));
        StringWriter texto = new StringWriter(); p.store(texto, "Missão Marte - progresso local");
        return texto.toString();
    }

    private static void gravarAtomico(Path destino, String texto) throws IOException {
        Path temporario = Files.createTempFile(destino.getParent(), ".historia-", ".tmp");
        try {
            try (var escritor = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) { escritor.write(texto); }
            try { Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporario); }
    }

    private static final class DadosInvalidos extends IOException {
        private static final long serialVersionUID = 1L;
    }

    private static final class VersaoFutura extends IOException {
        private static final long serialVersionUID = 1L;
    }
}
