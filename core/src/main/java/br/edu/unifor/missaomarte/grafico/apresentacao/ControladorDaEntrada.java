package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import com.badlogic.gdx.Input.Keys;
import java.util.Optional;

/**
 * Tela de entrada (Fase 5), sem OpenGL: 1 entra com uma conta, 2 cadastra e já entra, 3 joga como convidado (só o
 * ranking local, como nas fases anteriores) e, na Fase 6, 4 troca o endereço do servidor (no celular não há "-D"). O formulário tem nome e senha; Tab ou as setas trocam de campo, Enter
 * avança e envia, Esc volta. O pedido é assíncrono: a tela consulta {@link #getSessao()} a cada quadro.
 */
public final class ControladorDaEntrada {

    public enum Etapa { ESCOLHA, FORMULARIO, AGUARDANDO, SERVIDOR }

    public enum Modo { ENTRAR, CADASTRAR }

    public enum Acao { NENHUMA, CONVIDADO }

    public static final int NOME_MAXIMO = 20;
    public static final int SENHA_MAXIMA = 72;

    private final ServidorDoJogo servidor;
    private final ConfiguracaoDoServidor configuracao;
    private final StringBuilder endereco = new StringBuilder();
    private Etapa etapa = Etapa.ESCOLHA;
    private Modo modo = Modo.ENTRAR;
    private final StringBuilder nome = new StringBuilder();
    private final StringBuilder senha = new StringBuilder();
    private boolean editandoSenha;
    private boolean ignorarProximoCaractere;
    private String mensagem = "";
    private Sessao sessao;

    public ControladorDaEntrada(ServidorDoJogo servidor) {
        this(servidor, ConfiguracaoDoServidor.emMemoria("http://localhost:8080"));
    }

    public ControladorDaEntrada(ServidorDoJogo servidor, ConfiguracaoDoServidor configuracao) {
        this.servidor = servidor;
        this.configuracao = configuracao;
    }

    public Acao teclaPressionada(int tecla) {
        switch (etapa) {
            case ESCOLHA: return escolher(tecla);
            case FORMULARIO: editar(tecla); return Acao.NENHUMA;
            case SERVIDOR: editarServidor(tecla); return Acao.NENHUMA;
            default: return Acao.NENHUMA;       // aguardando o servidor: ignora
        }
    }

    private Acao escolher(int tecla) {
        switch (tecla) {
            case Keys.NUM_1:
            case Keys.NUMPAD_1:
                abrirFormulario(Modo.ENTRAR);
                return Acao.NENHUMA;
            case Keys.NUM_2:
            case Keys.NUMPAD_2:
                abrirFormulario(Modo.CADASTRAR);
                return Acao.NENHUMA;
            case Keys.NUM_3:
            case Keys.NUMPAD_3:
                return Acao.CONVIDADO;
            case Keys.NUM_4:
            case Keys.NUMPAD_4:
                abrirServidor();
                return Acao.NENHUMA;
            default:
                mensagem = "Escolha 1, 2, 3 ou 4.";
                return Acao.NENHUMA;
        }
    }

    private void abrirFormulario(Modo m) {
        modo = m;
        etapa = Etapa.FORMULARIO;
        editandoSenha = false;
        mensagem = "";
        // a tecla "1" ou "2" que abriu o formulário ainda vai gerar o caractere digitado: não pode virar nome
        ignorarProximoCaractere = true;
    }

    private void editar(int tecla) {
        switch (tecla) {
            case Keys.TAB:
            case Keys.UP:
            case Keys.DOWN:
                editandoSenha = !editandoSenha;
                break;
            case Keys.BACKSPACE:
                StringBuilder campo = editandoSenha ? senha : nome;
                if (campo.length() > 0) {
                    campo.deleteCharAt(campo.length() - 1);
                }
                break;
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
                if (!editandoSenha) {
                    editandoSenha = true;
                } else {
                    enviar();
                }
                break;
            case Keys.ESCAPE:
                etapa = Etapa.ESCOLHA;
                nome.setLength(0);
                senha.setLength(0);
                mensagem = "";
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ Fase 6: endereço do servidor

    private void abrirServidor() {
        if (configuracao.isForcado()) {
            mensagem = "Servidor definido ao abrir o jogo: " + configuracao.endereco();
            return;
        }
        etapa = Etapa.SERVIDOR;
        endereco.setLength(0);
        endereco.append(configuracao.endereco());
        mensagem = "";
        ignorarProximoCaractere = true;
    }

    private void editarServidor(int tecla) {
        switch (tecla) {
            case Keys.BACKSPACE:
                if (endereco.length() > 0) {
                    endereco.deleteCharAt(endereco.length() - 1);
                }
                break;
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
                salvarServidor(endereco.toString());
                break;
            case Keys.ESCAPE:
                etapa = Etapa.ESCOLHA;
                mensagem = "";
                break;
            default:
                break;
        }
    }

    /**
     * Grava o endereço (pelo teclado, com Enter, ou pela caixa de texto do celular). Válido: volta à escolha com a
     * confirmação; inválido: continua editando, com o motivo.
     */
    public void salvarServidor(String novo) {
        java.util.Optional<String> problema = configuracao.alterar(novo);
        if (problema.isPresent()) {
            etapa = Etapa.SERVIDOR;
            endereco.setLength(0);
            endereco.append(novo == null ? "" : novo.trim());
            mensagem = problema.get();
        } else {
            etapa = Etapa.ESCOLHA;
            mensagem = "Servidor: " + configuracao.endereco();
        }
    }

    public String getEnderecoEditado() { return endereco.toString(); }
    public String getEnderecoAtual() { return configuracao.endereco(); }

    // ------------------------------------------------------------------ Fase 6: toque

    /** Caixa de texto do celular para o nome (substitui o que havia). */
    public void definirNome(String texto) {
        if (etapa != Etapa.FORMULARIO) return;
        nome.setLength(0);
        String t = texto == null ? "" : texto.strip();
        nome.append(t, 0, Math.min(t.length(), NOME_MAXIMO));
        editandoSenha = false;
    }

    /** Caixa de texto do celular para a senha (substitui a que havia). */
    public void definirSenha(String texto) {
        if (etapa != Etapa.FORMULARIO) return;
        senha.setLength(0);
        String t = texto == null ? "" : texto;
        senha.append(t, 0, Math.min(t.length(), SENHA_MAXIMA));
        editandoSenha = true;
    }

    /** Toque num campo: o teclado físico passa a digitar nele (desktop em modo toque). */
    public void focar(boolean senhaEmFoco) {
        if (etapa == Etapa.FORMULARIO) {
            editandoSenha = senhaEmFoco;
        }
    }

    /** Botão "Entrar"/"Criar conta": envia direto, sem precisar passar pelo Enter do nome. */
    public void confirmar() {
        if (etapa == Etapa.FORMULARIO) {
            enviar();
        }
    }

    /** Caractere digitado (keyTyped). Controles (Enter, Tab, Backspace) não entram nos campos. */
    public void digitar(char c) {
        if (ignorarProximoCaractere) {
            ignorarProximoCaractere = false;
            return;
        }
        if (etapa == Etapa.SERVIDOR && !Character.isISOControl(c)) {
            if (endereco.length() < 120) endereco.append(c);
            return;
        }
        if (etapa != Etapa.FORMULARIO || Character.isISOControl(c)) {
            return;
        }
        if (editandoSenha) {
            if (senha.length() < SENHA_MAXIMA) senha.append(c);
        } else if (nome.length() < NOME_MAXIMO) {
            nome.append(c);
        }
    }

    private void enviar() {
        String n = nome.toString().trim();
        String s = senha.toString();
        if (n.isEmpty() || s.isEmpty()) {
            mensagem = "Preencha o nome e a senha.";
            return;
        }
        etapa = Etapa.AGUARDANDO;
        mensagem = "Conectando ao servidor...";
        Resposta<Sessao> aoEntrar = new Resposta<>() {
            @Override public void sucesso(Sessao s) { sessao = s; }
            @Override public void falha(Falha f) { voltarComErro(f); }
        };
        if (modo == Modo.ENTRAR) {
            servidor.entrar(n, s, aoEntrar);
        } else {
            servidor.cadastrar(n, s, new Resposta<>() {
                @Override public void sucesso(Void nada) { servidor.entrar(n, s, aoEntrar); }
                @Override public void falha(Falha f) { voltarComErro(f); }
            });
        }
    }

    private void voltarComErro(Falha f) {
        etapa = Etapa.FORMULARIO;
        editandoSenha = true;
        senha.setLength(0);          // a senha não fica guardada depois de uma tentativa
        mensagem = f.mensagem();
    }

    /** Presente quando o login (ou o cadastro seguido de login) deu certo: a tela vai para o menu. */
    public Optional<Sessao> getSessao() { return Optional.ofNullable(sessao); }
    public Etapa getEtapa() { return etapa; }
    public Modo getModo() { return modo; }
    public String getNome() { return nome.toString(); }
    /** A senha aparece só como asteriscos. */
    public String getSenhaMascarada() { return "*".repeat(senha.length()); }
    public boolean isEditandoSenha() { return editandoSenha; }
    public String getMensagem() { return mensagem; }
}
