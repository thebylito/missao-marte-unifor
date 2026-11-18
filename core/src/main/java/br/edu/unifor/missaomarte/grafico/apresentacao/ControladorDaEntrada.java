package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import com.badlogic.gdx.Input.Keys;
import java.util.Optional;

/**
 * Tela de entrada (Fase 5), sem OpenGL: 1 entra com uma conta, 2 cadastra e já entra, 3 joga como convidado (só o
 * ranking local, como nas fases anteriores). O formulário tem nome e senha; Tab ou as setas trocam de campo, Enter
 * avança e envia, Esc volta. O pedido é assíncrono: a tela consulta {@link #getSessao()} a cada quadro.
 */
public final class ControladorDaEntrada {

    public enum Etapa { ESCOLHA, FORMULARIO, AGUARDANDO }

    public enum Modo { ENTRAR, CADASTRAR }

    public enum Acao { NENHUMA, CONVIDADO }

    public static final int NOME_MAXIMO = 20;
    public static final int SENHA_MAXIMA = 72;

    private final ServidorDoJogo servidor;
    private Etapa etapa = Etapa.ESCOLHA;
    private Modo modo = Modo.ENTRAR;
    private final StringBuilder nome = new StringBuilder();
    private final StringBuilder senha = new StringBuilder();
    private boolean editandoSenha;
    private boolean ignorarProximoCaractere;
    private String mensagem = "";
    private Sessao sessao;

    public ControladorDaEntrada(ServidorDoJogo servidor) {
        this.servidor = servidor;
    }

    public Acao teclaPressionada(int tecla) {
        switch (etapa) {
            case ESCOLHA: return escolher(tecla);
            case FORMULARIO: editar(tecla); return Acao.NENHUMA;
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
            default:
                mensagem = "Escolha 1, 2 ou 3.";
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

    /** Caractere digitado (keyTyped). Controles (Enter, Tab, Backspace) não entram nos campos. */
    public void digitar(char c) {
        if (ignorarProximoCaractere) {
            ignorarProximoCaractere = false;
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
