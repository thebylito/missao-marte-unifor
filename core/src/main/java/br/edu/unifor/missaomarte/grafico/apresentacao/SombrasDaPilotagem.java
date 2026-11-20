package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.ConexaoDeSombras;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.NaveEmVoo;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * O modo shadow na tela de pilotagem (Fase 7): manda a posição da nave ao servidor 10 vezes por segundo e guarda as
 * naves dos outros pilotos ("sombras"), que a tela desenha translúcidas. Cada um joga a sua partida: as sombras não
 * batem, não pousam e não pegam passageiros.
 *
 * <p>Só entra quem fez login (o servidor exige o token). As posições chegam 10 vezes por segundo e a tela desenha 60:
 * entre uma e outra, a sombra desliza até a última posição recebida (suavização exponencial), em vez de pular.
 * Sombra sem notícia por {@value #TEMPO_SEM_NOTICIA} s some. Se a conexão cair, tenta de novo a cada
 * {@value #ESPERA_PARA_RECONECTAR} s enquanto a tela estiver aberta.
 *
 * <p>Tudo roda na thread do jogo (a {@link ConexaoDeSombras} entrega os avisos nela).
 */
public final class SombrasDaPilotagem {

    /** Em que pé está a conexão (aparece no painel). */
    public enum Situacao { DESLIGADAS, SEM_LOGIN, CONECTANDO, CONECTADAS, SEM_CONEXAO }

    /** Uma sombra, já na posição de desenho. */
    public record Sombra(int piloto, String nome, double x, double y, double angulo, boolean motor) {
    }

    static final float INTERVALO_DE_ENVIO = 0.1f;
    static final float TEMPO_SEM_NOTICIA = 3f;
    static final float ESPERA_PARA_RECONECTAR = 3f;
    /** Quanto maior, mais rápido a sombra alcança a posição recebida (por segundo). */
    static final double RAPIDEZ = 12;
    static final int NOME_MAXIMO = 20;

    /** Uma sombra: onde está desenhada e para onde está indo. */
    private static final class Vista {
        final int piloto;
        String nome;
        double x;
        double y;
        double angulo;
        double alvoX;
        double alvoY;
        double alvoAngulo;
        boolean motor;
        float semNoticia;

        Vista(int piloto) {
            this.piloto = piloto;
        }
    }

    private final Optional<Sessao> sessao;
    private final Optional<ConexaoDeSombras> conexao;
    private final Map<Integer, Vista> vistas = new LinkedHashMap<>();
    private final JsonReader leitor = new JsonReader();
    private Situacao situacao;
    private boolean iniciadas;
    private boolean fechadas;
    private float desdeOEnvio;
    private float paraReconectar;

    public SombrasDaPilotagem(Optional<Sessao> sessao, Optional<ConexaoDeSombras> conexao) {
        this.sessao = sessao;
        this.conexao = conexao;
        this.situacao = conexao.isEmpty() ? Situacao.DESLIGADAS
                : sessao.isEmpty() ? Situacao.SEM_LOGIN : Situacao.CONECTANDO;
    }

    /** Sem servidor (validação da janela, testes antigos): nada é enviado nem desenhado. */
    public static SombrasDaPilotagem desligadas() {
        return new SombrasDaPilotagem(Optional.empty(), Optional.empty());
    }

    // ------------------------------------------------------------------ tempo

    /** Chamado a cada quadro: conecta na primeira vez, manda a posição e move as sombras. */
    public void atualizar(float delta, NaveEmVoo nave, Caverna caverna) {
        if (fechadas) {
            return;
        }
        if (!iniciadas) {
            iniciadas = true;
            abrir();
        }
        if (situacao == Situacao.SEM_CONEXAO) {
            paraReconectar -= delta;
            if (paraReconectar <= 0) {
                abrir();
            }
        }
        if (situacao == Situacao.CONECTADAS) {
            desdeOEnvio += delta;
            if (desdeOEnvio >= INTERVALO_DE_ENVIO && nave.getEstado() != NaveEmVoo.Estado.DESTRUIDA) {
                desdeOEnvio = 0;
                conexao.get().enviar(posicao(nave, caverna));
            }
        }
        mover(delta);
    }

    private void abrir() {
        if (situacao == Situacao.DESLIGADAS || situacao == Situacao.SEM_LOGIN) {
            return;
        }
        situacao = Situacao.CONECTANDO;
        conexao.get().abrir(sessao.get().token(), new ConexaoDeSombras.Ouvinte() {
            @Override
            public void aberta() {
                if (!fechadas) {
                    situacao = Situacao.CONECTADAS;
                    desdeOEnvio = INTERVALO_DE_ENVIO;   // a primeira posição sai no próximo quadro
                }
            }

            @Override
            public void mensagem(String texto) {
                if (!fechadas) {
                    receber(texto);
                }
            }

            @Override
            public void fechada(String motivo) {
                if (!fechadas) {
                    situacao = Situacao.SEM_CONEXAO;
                    paraReconectar = ESPERA_PARA_RECONECTAR;
                    vistas.clear();
                }
            }
        });
    }

    /** O que vai ao servidor; dentro da caverna e do intervalo de ângulo que o servidor aceita. */
    static String posicao(NaveEmVoo nave, Caverna caverna) {
        double x = Math.max(0, Math.min(caverna.getLargura(), nave.getX()));
        double y = Math.max(0, Math.min(caverna.getAltura(), nave.getY()));
        double angulo = Math.max(-180, Math.min(180, nave.getAngulo()));
        return String.format(Locale.ROOT, "{\"tipo\":\"posicao\",\"x\":%.1f,\"y\":%.1f,\"angulo\":%.1f,\"motor\":%b}",
                x, y, angulo, nave.isMotorLigado());
    }

    /** Uma mensagem do servidor. O que não for entendido é ignorado. */
    void receber(String texto) {
        JsonValue m;
        try {
            m = leitor.parse(texto);
        } catch (RuntimeException e) {
            return;
        }
        if (m == null || !m.isObject()) {
            return;
        }
        String tipo = m.getString("tipo", "");
        try {
            if (tipo.equals("sombra")) {
                int piloto = m.getInt("piloto");
                double x = m.getDouble("x");
                double y = m.getDouble("y");
                double angulo = m.getDouble("angulo");
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(angulo)) {
                    return;
                }
                Vista v = vistas.get(piloto);
                if (v == null) {   // sombra nova: aparece direto onde está
                    v = new Vista(piloto);
                    v.x = x;
                    v.y = y;
                    v.angulo = angulo;
                    vistas.put(piloto, v);
                }
                String nome = m.getString("nome", "Piloto");
                v.nome = nome.length() > NOME_MAXIMO ? nome.substring(0, NOME_MAXIMO) : nome;
                v.alvoX = x;
                v.alvoY = y;
                v.alvoAngulo = angulo;
                v.motor = m.getBoolean("motor", false);
                v.semNoticia = 0;
            } else if (tipo.equals("saiu")) {
                vistas.remove(m.getInt("piloto"));
            }
        } catch (RuntimeException e) {
            // campo faltando ou de outro tipo: ignora a mensagem
        }
    }

    private void mover(float delta) {
        double fator = 1 - Math.exp(-RAPIDEZ * delta);
        for (Iterator<Vista> it = vistas.values().iterator(); it.hasNext(); ) {
            Vista v = it.next();
            v.semNoticia += delta;
            if (v.semNoticia > TEMPO_SEM_NOTICIA) {
                it.remove();
                continue;
            }
            v.x += (v.alvoX - v.x) * fator;
            v.y += (v.alvoY - v.y) * fator;
            v.angulo = normalizar(v.angulo + normalizar(v.alvoAngulo - v.angulo) * fator);
        }
    }

    /** Ângulo em (-180, 180]: a sombra gira pelo lado mais curto (de 179° para -179° são 2°, não 358°). */
    static double normalizar(double graus) {
        double a = graus % 360;
        if (a > 180) {
            a -= 360;
        } else if (a <= -180) {
            a += 360;
        }
        return a;
    }

    /** A tela saiu (fim da partida, abortou, fechou o jogo): fecha a conexão e esquece as sombras. */
    public void fechar() {
        if (fechadas) {
            return;
        }
        fechadas = true;
        conexao.ifPresent(ConexaoDeSombras::fechar);
        vistas.clear();
    }

    // ------------------------------------------------------------------ leitura

    public List<Sombra> getSombras() {
        List<Sombra> lista = new ArrayList<>(vistas.size());
        for (Vista v : vistas.values()) {
            lista.add(new Sombra(v.piloto, v.nome, v.x, v.y, v.angulo, v.motor));
        }
        return lista;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    /** A linha do painel; vazia quando as sombras estão desligadas (sem servidor). */
    public Optional<String> descricao() {
        return switch (situacao) {
            case DESLIGADAS -> Optional.empty();
            case SEM_LOGIN -> Optional.of("Sombras: entre com uma conta");
            case CONECTANDO -> Optional.of("Sombras: conectando...");
            case SEM_CONEXAO -> Optional.of("Sombras: sem conexão");
            case CONECTADAS -> Optional.of(switch (vistas.size()) {
                case 0 -> "Sombras: nenhum outro piloto";
                case 1 -> "Sombras: 1 piloto";
                default -> "Sombras: " + vistas.size() + " pilotos";
            });
        };
    }
}
