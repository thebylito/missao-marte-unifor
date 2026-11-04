package br.edu.unifor.missaomarte.infraestrutura;

import br.edu.unifor.missaomarte.aplicacao.RepositorioRanking;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.dominio.Ranking;
import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Guarda o ranking num arquivo JSON, no mesmo formato da versão original (arquivos antigos continuam válidos). */
public class RankingArquivoJson implements RepositorioRanking {

    private final Path arquivo;

    public RankingArquivoJson(Path arquivo) {
        this.arquivo = arquivo;
    }

    @Override
    public List<RegistroRanking> carregar() {
        if (!Files.exists(arquivo)) {
            return new ArrayList<>();
        }
        try {
            String json = new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8).trim();
            return Ranking.ordenar(ler(json));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void salvar(List<RegistroRanking> registros) throws IOException {
        Files.write(arquivo, escrever(registros).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void apagar() throws IOException {
        Files.deleteIfExists(arquivo);
    }

    static String escrever(List<RegistroRanking> registros) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < registros.size(); i++) {
            RegistroRanking r = registros.get(i);
            b.append("{\"name\":\"").append(r.nome().replace("\"", "\\\""))
                    .append("\",\"score\":").append(r.pontos())
                    .append(",\"dificuldade\":\"").append(r.dificuldade().name())
                    .append("\",\"passageirosColetados\":").append(r.passageirosColetados())
                    .append(",\"dataHora\":\"").append(r.dataHora())
                    .append("\",\"tempoJogo\":").append(r.tempoJogoSegundos())
                    .append("}");
            if (i < registros.size() - 1) {
                b.append(",");
            }
        }
        return b.append("]").toString();
    }

    /**
     * Leitor do formato acima. Herdado da versão original: separa os campos por vírgula, então um nome
     * com vírgula faz o registro ser descartado.
     */
    static List<RegistroRanking> ler(String json) {
        List<RegistroRanking> registros = new ArrayList<>();
        if (json.isEmpty() || json.equals("[]")) {
            return registros;
        }
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }
        int indice = 0;
        while (indice < json.length()) {
            int inicio = json.indexOf('{', indice);
            if (inicio < 0) break;
            int fim = json.indexOf('}', inicio);
            if (fim < 0) break;
            RegistroRanking r = lerObjeto(json.substring(inicio + 1, fim));
            if (r != null) {
                registros.add(r);
            }
            indice = fim + 1;
        }
        return registros;
    }

    private static RegistroRanking lerObjeto(String objeto) {
        String nome = null;
        Integer pontos = null;
        Dificuldade dificuldade = Dificuldade.MEDIO;
        int coletados = 0;
        String dataHora = "";
        long tempo = 0;
        for (String parte : objeto.split(",")) {
            String[] par = parte.split(":", 2);
            if (par.length != 2) continue;
            String chave = par[0].trim().replaceAll("\"", "");
            String valor = par[1].trim();
            switch (chave) {
                case "name":
                    if (entreAspas(valor)) nome = semAspas(valor).replace("\\\"", "\"");
                    break;
                case "score":
                    try { pontos = Integer.parseInt(valor); } catch (NumberFormatException ignorado) { }
                    break;
                case "dificuldade":
                    if (entreAspas(valor)) dificuldade = Dificuldade.deString(semAspas(valor));
                    break;
                case "passageirosColetados":
                    try { coletados = Integer.parseInt(valor); } catch (NumberFormatException ignorado) { }
                    break;
                case "dataHora":
                    if (entreAspas(valor)) dataHora = semAspas(valor);
                    break;
                case "tempoJogo":
                    try { tempo = Long.parseLong(valor); } catch (NumberFormatException ignorado) { }
                    break;
                default:
                    break;
            }
        }
        return nome != null && pontos != null
                ? new RegistroRanking(nome, pontos, dificuldade, coletados, dataHora, tempo)
                : null;
    }

    private static boolean entreAspas(String v) {
        return v.startsWith("\"") && v.endsWith("\"");
    }

    private static String semAspas(String v) {
        return v.substring(1, v.length() - 1);
    }
}
