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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            b.append("{\"name\":").append(texto(r.nome()))
                    .append(",\"score\":").append(r.pontos())
                    .append(",\"dificuldade\":").append(texto(r.dificuldade().name()))
                    .append(",\"passageirosColetados\":").append(r.passageirosColetados())
                    .append(",\"dataHora\":").append(texto(r.dataHora()))
                    .append(",\"tempoJogo\":").append(r.tempoJogoSegundos())
                    .append("}");
            if (i < registros.size() - 1) {
                b.append(",");
            }
        }
        return b.append("]").toString();
    }

    /** Texto JSON entre aspas, escapando aspas, barra invertida e caracteres de controle. */
    private static String texto(String valor) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }

    /**
     * Lê uma lista de objetos simples. Corrigido na Fase 1: a versão original separava os campos por vírgula
     * e descartava nomes com vírgula. Continua tolerante: registro sem nome ou pontos é ignorado e, se o
     * arquivo estiver malformado, valem os registros lidos até ali.
     */
    static List<RegistroRanking> ler(String json) {
        List<RegistroRanking> registros = new ArrayList<>();
        LeitorJson leitor = new LeitorJson(json);
        try {
            if (leitor.fim()) {
                return registros;
            }
            leitor.esperar('[');
            if (leitor.consumirSe(']')) {
                return registros;
            }
            do {
                RegistroRanking r = paraRegistro(leitor.objeto());
                if (r != null) {
                    registros.add(r);
                }
            } while (leitor.consumirSe(','));
            leitor.esperar(']');
        } catch (IllegalArgumentException malformado) {
            // mantém o que foi lido
        }
        return registros;
    }

    private static RegistroRanking paraRegistro(Map<String, Object> campos) {
        Object nome = campos.get("name");
        Integer pontos = inteiro(campos.get("score"));
        if (!(nome instanceof String) || pontos == null) {
            return null;
        }
        Object dificuldade = campos.get("dificuldade");
        Integer coletados = inteiro(campos.get("passageirosColetados"));
        Object dataHora = campos.get("dataHora");
        Long tempo = campos.get("tempoJogo") instanceof Long ? (Long) campos.get("tempoJogo") : null;
        return new RegistroRanking((String) nome, pontos,
                dificuldade instanceof String ? Dificuldade.deString((String) dificuldade) : Dificuldade.MEDIO,
                coletados == null ? 0 : coletados,
                dataHora instanceof String ? (String) dataHora : "",
                tempo == null ? 0 : tempo);
    }

    private static Integer inteiro(Object valor) {
        if (valor instanceof Long && (Long) valor >= Integer.MIN_VALUE && (Long) valor <= Integer.MAX_VALUE) {
            return ((Long) valor).intValue();
        }
        return null;
    }

    /** Analisador mínimo para objetos planos com valores texto, inteiro, true, false ou null. */
    private static final class LeitorJson {
        private final String s;
        private int i;

        LeitorJson(String s) {
            this.s = s;
        }

        boolean fim() {
            pularEspacos();
            return i >= s.length();
        }

        void esperar(char c) {
            if (!consumirSe(c)) {
                throw new IllegalArgumentException("esperado '" + c + "' na posição " + i);
            }
        }

        boolean consumirSe(char c) {
            pularEspacos();
            if (i < s.length() && s.charAt(i) == c) {
                i++;
                return true;
            }
            return false;
        }

        Map<String, Object> objeto() {
            Map<String, Object> campos = new HashMap<>();
            esperar('{');
            if (consumirSe('}')) {
                return campos;
            }
            do {
                pularEspacos();
                String chave = texto();
                esperar(':');
                campos.put(chave, valor());
            } while (consumirSe(','));
            esperar('}');
            return campos;
        }

        private Object valor() {
            pularEspacos();
            if (i >= s.length()) {
                throw new IllegalArgumentException("valor ausente");
            }
            char c = s.charAt(i);
            if (c == '"') {
                return texto();
            }
            int inicio = i;
            while (i < s.length() && ",}] \t\r\n".indexOf(s.charAt(i)) < 0) {
                i++;
            }
            String bruto = s.substring(inicio, i);
            switch (bruto) {
                case "true": return Boolean.TRUE;
                case "false": return Boolean.FALSE;
                case "null": return null;
                default:
                    try {
                        return Long.parseLong(bruto);
                    } catch (NumberFormatException e) {
                        return bruto; // número com casas decimais etc.: não usado no ranking
                    }
            }
        }

        private String texto() {
            if (i >= s.length() || s.charAt(i) != '"') {
                throw new IllegalArgumentException("esperado texto na posição " + i);
            }
            i++;
            StringBuilder b = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (i >= s.length()) {
                    break;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u':
                        if (i + 4 > s.length()) {
                            throw new IllegalArgumentException("escape \\u incompleto");
                        }
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: b.append(e); // \" \\ \/
                }
            }
            throw new IllegalArgumentException("texto sem aspas de fechamento");
        }

        private void pularEspacos() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
